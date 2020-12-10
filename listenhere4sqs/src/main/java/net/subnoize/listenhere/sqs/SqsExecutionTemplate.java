package net.subnoize.listenhere.sqs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.subnoize.listenhere.Session;
import net.subnoize.listenhere.listen.ListenTo;
import net.subnoize.listenhere.model.Attribute;
import net.subnoize.listenhere.model.Payload;
import net.subnoize.listenhere.send.SendTo;
import net.subnoize.listenhere.util.ConfigurationUtils;

@NoArgsConstructor
@Getter
@Setter
public class SqsExecutionTemplate {

	@Autowired
	private ConfigurationUtils helper;

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
	 * 
	 */
	@PostConstruct
	private void init() {
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
	}

	@PostConstruct
	private void parameterInit() {
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
	}

	@PostConstruct
	private void transactionId() {
		if (StringUtils.isNotBlank(to.transactionId()) && !attributeNames.contains(to.transactionId())) {
			attributeNames.add(to.transactionId());
		}
	}

	public Session newSession() {
		return Session.builder().acknowledge(to.acknowledge()).error(false).errorCode(-1).errorDescription(null)
				.replyToQueueUrl(sendTo).build();
	}

	public Object invoke(Object[] args) throws IllegalAccessException, InvocationTargetException {
		return method.invoke(target, args);
	}

}
