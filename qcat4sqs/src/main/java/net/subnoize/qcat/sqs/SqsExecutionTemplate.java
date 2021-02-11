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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.subnoize.qcat.Session;
import net.subnoize.qcat.listen.ListenTo;
import net.subnoize.qcat.model.Attribute;
import net.subnoize.qcat.model.Payload;
import net.subnoize.qcat.send.SendTo;
import net.subnoize.qcat.util.ConfigurationUtils;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

public class SqsExecutionTemplate {
	
	private static Logger log = LoggerFactory.getLogger(Qcat4Sqs.class);

	@Autowired
	private ConfigurationUtils helper;

	@Autowired
	private SqsAsyncClient asyncClient;

	private String queueUrl;
	private Method method;
	private Object target;
	private Parameter[] parameters;
	private Parameter payload;
	private List<Parameter> attributes = new ArrayList<>();
	private Collection<String> attributeNames = new ArrayList<>();
	private ListenTo to;
	private int threadCeiling = 0;
	private boolean sendToPresent = false;
	private String sendTo;
	private boolean sendToAsString;

	/**
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * 
	 */
	@PostConstruct
	private void init() throws InterruptedException, ExecutionException {

		to = method.getAnnotation(ListenTo.class);
		queueUrl = to.value();

		if (queueUrl.contains("${")) {
			queueUrl = helper.getString(queueUrl);
		}

		String temp = resolveQueueUrl(queueUrl);
		if (StringUtils.isNotBlank(temp)) {
			queueUrl = temp;
		}

		log.info("Starting: {}.{}('{}',{},{},{},{})", target.getClass().getName(), method.getName(), queueUrl,
				to.min(), to.max(), to.timeout(), to.polling());

		if (method.isAnnotationPresent(SendTo.class)) {
			sendToPresent = true;
			sendTo = method.getAnnotation(SendTo.class).value();

			if (StringUtils.isNotBlank(sendTo)) {
				if (sendTo.contains("${")) {
					sendTo = helper.getString(sendTo);
				}

				String sendToStr = resolveQueueUrl(sendTo);
				if (StringUtils.isNotBlank(sendToStr)) {
					sendTo = sendToStr;
				}
			}
			Class<?> ret = method.getReturnType();
			if (String.class.equals(ret) || Integer.class.equals(ret) || Long.class.equals(ret)
					|| Float.class.equals(ret) || Double.class.equals(ret)) {
				sendToAsString = true;
			} else {
				sendToAsString = false;
			}

		}

		parameters = method.getParameters();
		if (parameters.length == 1) {
			payload = parameters[0];
		} else {
			for (Parameter p : parameters) {
				if (p.isAnnotationPresent(Payload.class) && payload == null) {
					payload = p;
				} else if (p.isAnnotationPresent(Attribute.class)) {
					if (attributes == null) {
						attributes = new ArrayList<>();
					}
					attributes.add(p);
					attributeNames.add(p.getAnnotation(Attribute.class).value());
				}
			}
		}

		if (StringUtils.isNotBlank(to.transactionId()) && !attributeNames.contains(to.transactionId())) {
			attributeNames.add(to.transactionId());
		}
	}

	private String resolveQueueUrl(String queueName) throws InterruptedException, ExecutionException {
		if (queueName.toLowerCase().contains("https://")) {
			return queueName;
		} else {
			return asyncClient.getQueueUrl(b -> b.queueName(queueName)).get().queueUrl();
		}
	}

	/**
	 * Builds and returns the Qcat Session object
	 * @param m
	 * @return
	 */
	public Session newSession(Message m) {
		Session s = new Session();
		s.setAcknowledge(to.acknowledge());
		s.setError(false);
		s.setErrorCode(-1);
		s.setErrorDescription(null);
		s.setDestination(sendTo);
		s.setRequest(m);
		return s;
	}

	public Object invoke(Object[] args) throws IllegalAccessException, InvocationTargetException {
		return method.invoke(target, args);
	}

	public ConfigurationUtils getHelper() {
		return helper;
	}

	public void setHelper(ConfigurationUtils helper) {
		this.helper = helper;
	}

	public SqsAsyncClient getAsyncClient() {
		return asyncClient;
	}

	public void setAsyncClient(SqsAsyncClient asyncClient) {
		this.asyncClient = asyncClient;
	}

	public String getQueueUrl() {
		return queueUrl;
	}

	public void setQueueUrl(String queueUrl) {
		this.queueUrl = queueUrl;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public Parameter[] getParameters() {
		return parameters;
	}

	public void setParameters(Parameter[] parameters) {
		this.parameters = parameters;
	}

	public Parameter getPayload() {
		return payload;
	}

	public void setPayload(Parameter payload) {
		this.payload = payload;
	}

	public List<Parameter> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Parameter> attributes) {
		this.attributes = attributes;
	}

	public Collection<String> getAttributeNames() {
		return attributeNames;
	}

	public void setAttributeNames(Collection<String> attributeNames) {
		this.attributeNames = attributeNames;
	}

	public ListenTo getTo() {
		return to;
	}

	public void setTo(ListenTo to) {
		this.to = to;
	}

	public int getThreadCeiling() {
		return threadCeiling;
	}

	public void setThreadCeiling(int threadCeiling) {
		this.threadCeiling = threadCeiling;
	}

	public boolean isSendToPresent() {
		return sendToPresent;
	}

	public void setSendToPresent(boolean sendToPresent) {
		this.sendToPresent = sendToPresent;
	}

	public String getSendTo() {
		return sendTo;
	}

	public void setSendTo(String sendTo) {
		this.sendTo = sendTo;
	}

	public boolean isSendToAsString() {
		return sendToAsString;
	}

	public void setSendToAsString(boolean sendToAsString) {
		this.sendToAsString = sendToAsString;
	}

}
