package net.subnoize.listenhere;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Session {
	
	private boolean acknowledge;
	
	private boolean error;
	
	private int errorCode;
	
	private String errorDescription;
	
	private Object rawInboundMessage;
	
	private String replyToQueueUrl;
	
	@Builder.Default
	private Map<String, Object> attributes = new HashMap<>();;
	
}
