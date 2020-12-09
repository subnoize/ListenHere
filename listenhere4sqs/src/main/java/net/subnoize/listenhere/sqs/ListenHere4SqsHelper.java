package net.subnoize.listenhere.sqs;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public class ListenHere4SqsHelper {
	
	public static final Map<String,MessageAttributeValue> getAttributeMap() {
		return new HashMap<>();
	}

}
