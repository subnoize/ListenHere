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

package net.subnoize.qcat;

import java.util.HashMap;
import java.util.Map;

/**
 * Session objects are to maintain the thread safety during operation while
 * processing events to which you are listening. They are built for you by the
 * Provider and control the various aspects of the event.
 * <p>
 * When a SendTo annotation is present it can specify a default destination but
 * by requesting the Session object in your method signature you can then change
 * the destination by setting the Session destination.
 * 
 * @author John Bryant
 *
 */public class Session {

	private boolean acknowledge;

	private boolean error;

	private int errorCode;

	private String errorDescription;

	private Object response;
	
	/**
	 * The raw inbound message is present here. If the message is delivered by JMS
	 * the this might be a TextMessage or the like
	 */
	private Object request;

	/**
	 * The override when the SendTo is present. This means that if the process
	 * requires redirection you can change that destination here in the Session
	 */
	private String destination;

	private Map<String, Object> attributes = new HashMap<>();

	public Session() {
		super();
		// TODO Auto-generated constructor stub
	}

	public boolean isAcknowledge() {
		return acknowledge;
	}

	public void setAcknowledge(boolean acknowledge) {
		this.acknowledge = acknowledge;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorDescription() {
		return errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}

	public Object getRequest() {
		return request;
	}

	public void setRequest(Object request) {
		this.request = request;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

}
