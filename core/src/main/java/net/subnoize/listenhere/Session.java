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

package net.subnoize.listenhere;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

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
 */
@Data
@Builder
public class Session {

	private boolean acknowledge;

	private boolean error;

	private int errorCode;

	private String errorDescription;

	/**
	 * The raw inbound message is present here. If the message is delivered by JMS
	 * the this might be a TextMessage or the like
	 */
	private Object rawInboundMessage;

	/**
	 * The override when the SendTo is present. This means that if the process
	 * requires redirection you can change that destination here in the Session
	 */
	private String destination;

	@Builder.Default
	private Map<String, Object> attributes = new HashMap<>();

}
