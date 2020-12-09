package net.subnoize.listenhere.sqs;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.subnoize.listenhere.Session;
import net.subnoize.listenhere.listen.ListenTo;
import net.subnoize.listenhere.model.Attribute;
import net.subnoize.listenhere.model.Payload;
import net.subnoize.listenhere.send.SendTo;
import net.subnoize.listenhere.util.ConfigurationUtils;
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
class ListenHere4SqsWorker implements Runnable, RejectedExecutionHandler {

	@Autowired
	private SqsAsyncClient asyncClient;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ConfigurationUtils helper;

	private List<CompletableFuture<Integer>> threadHandles = new CopyOnWriteArrayList<>();

	private String queueUrl;

	private Method method;

	private Object target;

	private boolean running = false;

	private Parameter[] parameters;

	private Parameter payload;
	private List<Parameter> attributes;
	private Collection<String> attributeNames;
	private Session session;

	private ListenTo to;

	private DescriptiveStatistics stats = new DescriptiveStatistics();
	private AtomicInteger taskCounter = new AtomicInteger(0);
	private int threadCeiling = 0;

	private ExecutorService executorService;
	private ScheduledExecutorService scheduleService;

	private boolean sendToPresent = false;
	private String sendTo;
	private boolean sendToAsString;

	ListenHere4SqsWorker(ListenTo to, String queueUrl, Method method, Object target) throws NoSuchMethodException {
		this.queueUrl = queueUrl;
		this.method = method;
		this.target = target;
		this.to = to;
		stats.setWindowSize(100);
	}

	@PostConstruct
	public void init() {
		processParameters();
		session = Session.builder().acknowledge(to.acknowledge()).error(false).errorCode(-1).errorDescription(null)
				.replyToQueueUrl(sendTo).build();
		executorService = new ThreadPoolExecutor(to.min(), to.max(), 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		scheduleService = Executors.newSingleThreadScheduledExecutor();
		running = true;
		scheduleService.scheduleAtFixedRate(this, 0, to.polling(), TimeUnit.MILLISECONDS);
	}

	/**
	 * 
	 */
	private void processParameters() {

		parameters = method.getParameters();
		if (method.isAnnotationPresent(SendTo.class)) {
			sendToPresent = true;
			sendTo = method.getAnnotation(SendTo.class).value();
			if (sendTo.contains("${")) {
				sendTo = helper.getString(sendTo);
				Class<?> ret = method.getReturnType();
				if (String.class.equals(ret) || Integer.class.equals(ret) || Long.class.equals(ret)
						|| Float.class.equals(ret) || Double.class.equals(ret)) {
					sendToAsString = true;
				} else {
					sendToAsString = false;
				}
			}
		}

		if (parameters.length == 1) {
			payload = parameters[0];
		} else {
			for (Parameter p : parameters) {
				if (p.isAnnotationPresent(Payload.class) && payload == null) {
					payload = p;
				} else if (p.isAnnotationPresent(Attribute.class)) {
					if (attributes == null) {
						attributes = new ArrayList<>();
						attributeNames = new ArrayList<>();
					}
					attributes.add(p);
					attributeNames.add(p.getAnnotation(Attribute.class).value());
				}
			}
		}

		if (StringUtils.isNotBlank(to.transactionId())) {
			if (attributeNames == null) {
				attributeNames = new ArrayList<>();
			}
			if (!attributeNames.contains(to.transactionId())) {
				attributeNames.add(to.transactionId());
			}
		}

	}

	public void shutdown() {
		running = false;
		log.info("Closing ListenTo: {}", helper.getString(to.value()));
		scheduleService.shutdown();
		executorService.shutdown();
	}

	@Override
	public void run() {
		if (running) {
			try {
				if (taskCounter.get() < to.min() || taskCounter.get() < threadCeiling) {
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
				ReceiveMessageResponse resp = asyncClient.receiveMessage(
						req -> req.queueUrl(queueUrl).maxNumberOfMessages(10).messageAttributeNames(attributeNames))
						.get();
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
			threadCeiling = to.min();
		}

		double newThreadCeiling = to.max() * stats.getMean();

		if (newThreadCeiling < to.min()) {
			newThreadCeiling = to.min();
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
				Object[] params = parseParams(m);
				if (params.length == parameters.length) {
					Object ret = method.invoke(target, params);
					if (sendToPresent) {
						if (ret instanceof SendMessageRequest) {
							asyncClient.sendMessage((SendMessageRequest) ret).get();
						} else if (StringUtils.isNotBlank(sendTo)) {
							String body = null;
							if (sendToAsString) {
								body = ret.toString();
							} else {
								body = mapper.writeValueAsString(ret);
							}
							asyncClient.sendMessage(SendMessageRequest.builder().queueUrl(sendTo).messageBody(body)
									.messageAttributes(getAttributes()).build()).get();
						}
					}
				}

				if (session.isAcknowledge()) {
					DeleteMessageResponse result = asyncClient
							.deleteMessage(del -> del.queueUrl(queueUrl).receiptHandle(m.receiptHandle())).get();
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

	private Map<String, MessageAttributeValue> getAttributes() {
		if (session.getAttributes().isEmpty()) {
			return null;
		}
		SqsMessageAttributes.Builder builder = SqsMessageAttributes.builder();
		session.getAttributes().forEach((k, v) -> {
			builder.attr(k, v);
		});
		return builder.build();
	}

	private Object[] parseParams(Message m) throws JsonProcessingException {
		Object[] params = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter p = parameters[i];
			if (p.getType() == Session.class) {
				params[i] = session;
			} else if (p.equals(payload)) {
				if (p.getType() == m.getClass()) {
					params[i] = p.getType().cast(m);
				} else if (p.getType() == String.class) {
					params[i] = m.body();
				} else {
					try {
						params[i] = mapper.readValue(m.body(), payload.getType());
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
		if (StringUtils.isNotBlank(to.transactionId())) {
			MessageAttributeValue v = m.messageAttributes().get(to.transactionId());
			if (v != null) {
				switch (v.dataType()) {
				case "String":
					session.getAttributes().put(to.transactionId(), v.stringValue());
					break;
				case "Number":
					if (NumberUtils.isParsable(v.stringValue())) {
						if(v.stringValue().contains(".")) {
							session.getAttributes().put(to.transactionId(), Double.parseDouble(v.stringValue()));
						} else {
							session.getAttributes().put(to.transactionId(), Long.parseLong(v.stringValue()));
						}
					} else {
						session.getAttributes().put(to.transactionId(), v.stringValue());
					}
					break;
				default:
					session.getAttributes().put(to.transactionId(), v.binaryValue().asByteArray());
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
