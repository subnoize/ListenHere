/**
 * (c)opyright 2020 subnoize llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.subnoize.qcat.sqs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.subnoize.qcat.Session;
import net.subnoize.qcat.model.Attribute;
import net.subnoize.qcat.util.ConfigurationUtils;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * 
 * @author youca
 *
 */
@Slf4j
class Qcat4SqsWorker implements Runnable, RejectedExecutionHandler {

	@Autowired
	private SqsAsyncClient asyncClient;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ConfigurationUtils helper;

	private List<CompletableFuture<Integer>> threadHandles = new CopyOnWriteArrayList<>();

	private boolean running = false;

	private DescriptiveStatistics stats = new DescriptiveStatistics();
	private AtomicInteger taskCounter = new AtomicInteger(0);
	private int threadCeiling = 0;

	private ExecutorService executorService;
	private ScheduledExecutorService scheduleService;

	private SqsExecutionTemplate template;

	Qcat4SqsWorker(SqsExecutionTemplate template) {
		this.template = template;
		stats.setWindowSize(100);
	}

	@PostConstruct
	public void init() {
		running = true;
		executorService = new ThreadPoolExecutor(template.getTo().min(), template.getTo().max(), 0L,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
		scheduleService = Executors.newSingleThreadScheduledExecutor();
		scheduleService.scheduleAtFixedRate(this, 0, template.getTo().polling(), TimeUnit.MILLISECONDS);
	}

	/**
	 * 
	 */
	public void shutdown() {
		running = false;
		log.info("Closing ListenTo: {}", helper.getString(template.getTo().value()));
		scheduleService.shutdown();
		executorService.shutdown();
	}

	@Override
	public void run() {
		if (running) {
			try {
				if (taskCounter.get() < template.getTo().min() || taskCounter.get() < threadCeiling) {
					pollServer();
				}
				manageThreadHandles();
			} catch (Exception e) {
				log.error("Message handling errors", e);
			}
		}
	}

	/**
	 * 
	 */
	private void pollServer() {
		CompletableFuture<Integer> fut = new CompletableFuture<>();
		executorService.submit(() -> {
			try {
				taskCounter.addAndGet(1);
				ReceiveMessageResponse resp = asyncClient.receiveMessage(req -> req.queueUrl(template.getQueueUrl())
						.maxNumberOfMessages(10).messageAttributeNames(template.getAttributeNames())).get();
				List<Message> messages = resp.messages();
				if (!messages.isEmpty()) {
					processMessages(messages);
				}
				fut.complete(messages.size());
			} catch (Exception e) {
				if (running) {
					log.error("Error from server polling thread", e);
				}
				fut.completeExceptionally(e);
			} finally {
				taskCounter.addAndGet(-1);
			}
		});
		threadHandles.add(fut);
	}

	/**
	 * 
	 */
	private void manageThreadHandles() {
		int change = threadHandles.size();
		threadHandles.forEach(h -> {
			if (h.isDone()) {
				try {
					if (!h.isCompletedExceptionally()) {
						stats.addValue(h.get() * 0.1d);
					}
				} catch (Exception e) {
					log.error("Error getting results from server polling thread", e);
				} finally {
					threadHandles.remove(h);
				}
			}
		});
		if (change < threadHandles.size()) {
			calculateThreadCeiling();
		}
	}

	/**
	 * 
	 */
	private void calculateThreadCeiling() {

		if (stats.getMean() == Double.NaN) {
			threadCeiling = template.getTo().min();
		}

		double newThreadCeiling = template.getTo().max() * stats.getMean();

		if (newThreadCeiling < template.getTo().min()) {
			newThreadCeiling = template.getTo().min();
		}

		int rounded = (int) Math.round(newThreadCeiling);

		if (rounded != threadCeiling) {
			log.info("Thread Ceiling: {}", rounded);
			threadCeiling = rounded;
		}
	}

	/**
	 * 
	 * @param messages
	 */
	private void processMessages(List<Message> messages) {
		messages.forEach(m -> {
			try {
				Session session = template.newSession();
				handleMessage(session, m);
				if (session.isAcknowledge()) {
					DeleteMessageResponse result = asyncClient
							.deleteMessage(del -> del.queueUrl(template.getQueueUrl()).receiptHandle(m.receiptHandle()))
							.get();
					if (200 != result.sdkHttpResponse().statusCode()) {
						log.info("Delete: {} {}", result.sdkHttpResponse().statusText(),
								result.sdkHttpResponse().statusCode());
					}
				}
			} catch (Exception e) {
				log.error("Error while handling messages", e);
			}
		});
	}

	/**
	 * Actually handle the individual messages
	 * 
	 * @param session
	 * @param m
	 * @throws JsonProcessingException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void handleMessage(Session session, Message m) throws JsonProcessingException, IllegalAccessException,
			InvocationTargetException, InterruptedException, ExecutionException {
		Object[] params = parseParams(session, m);
		if (params.length == template.getParameters().length) {
			Object ret = template.invoke(params);
			if (template.isSendToPresent()) {
				if (ret instanceof SendMessageRequest) {
					asyncClient.sendMessage((SendMessageRequest) ret).get();
				} else {
					String body = null;
					if (template.isSendToAsString()) {
						body = ret.toString();
					} else {
						body = mapper.writeValueAsString(ret);
					}
					asyncClient.sendMessage(SendMessageRequest.builder().queueUrl(session.getDestination())
							.messageBody(body).messageAttributes(getAttributes(session)).build()).get();
				}
			}
		}
		MDC.clear();
	}

	private Map<String, MessageAttributeValue> getAttributes(Session session) {
		if (session.getAttributes().isEmpty()) {
			return null;
		}
		SqsMessageAttributes.Builder builder = SqsMessageAttributes.builder();
		session.getAttributes().forEach((k, v) -> {
			builder.attr(k, v);
		});
		return builder.build();
	}

	private Object[] parseParams(Session session, Message m) throws JsonProcessingException {
		Object[] params = new Object[template.getParameters().length];
		for (int i = 0; i < params.length; i++) {
			Parameter p = template.getParameters()[i];
			if (p.getType() == Session.class) {
				params[i] = session;
			} else if (p.equals(template.getPayload())) {
				if (p.getType() == m.getClass()) {
					params[i] = p.getType().cast(m);
				} else if (p.getType() == String.class) {
					params[i] = m.body();
				} else {
					try {
						params[i] = mapper.readValue(m.body(), template.getPayload().getType());
					} catch (JsonProcessingException e) {
						if (session != null) {
							params[i] = null;
							session.setError(true);
							session.setErrorCode(500);
							session.setErrorDescription(e.getMessage());
							log.error("Error parsing object: {} Exception: {}", m.body(), e.getMessage());
						} else {
							throw e;
						}
					}
				}
			} else if (p.isAnnotationPresent(Attribute.class)) {
				Attribute attr = p.getAnnotation(Attribute.class);
				MessageAttributeValue v = m.messageAttributes().get(attr.value());
				if (v != null) {
					params[i] = v.stringValue();
				} else {
					params[i] = null;
				}
			} else {
				params[i] = null;
			}
		}
		if (StringUtils.isNotBlank(template.getTo().transactionId())) {
			MessageAttributeValue v = m.messageAttributes().get(template.getTo().transactionId());
			if (v != null) {
				MDC.put(template.getTo().transactionId(), v.stringValue());
				switch (v.dataType()) {
				case "String":
					session.getAttributes().put(template.getTo().transactionId(), v.stringValue());
					break;
				case "Number":
					if (NumberUtils.isParsable(v.stringValue())) {
						if (v.stringValue().contains(".")) {
							session.getAttributes().put(template.getTo().transactionId(),
									Double.parseDouble(v.stringValue()));
						} else {
							session.getAttributes().put(template.getTo().transactionId(),
									Long.parseLong(v.stringValue()));
						}
					} else {
						session.getAttributes().put(template.getTo().transactionId(), v.stringValue());
					}
					break;
				default:
					session.getAttributes().put(template.getTo().transactionId(), v.binaryValue().asByteArray());
					break;
				}

			} else {
				// auto create?
			}
		}
		return params;
	}

	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		log.error("{} : has been rejected", r.toString());
	}

}
