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

package net.subnoize.qcat.listen;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used to annotate a listener method.
 * 
 * <pre>
 * <code>
 * &#64;Component
 * public class MyListeners {	
 *   &#64;ListenTo(value = "${examples.say.hello.queue}")
 *   &#64;SendTo("${examples.say.hello.result.queue}")
 *   public String sayHello(String msg) {
 *     return "Hello, "+msg;
 *   }
 * }
 * </code>
 * </pre>
 * 
 * 
 * @author John Bryant
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface ListenTo {

	/**
	 * The Queue name to listen too.
	 * 
	 * @return the queue name
	 */
	String value();

	/**
	 * Minimum threads used for listening to this queue. Default is 1.
	 * 
	 * @return the minimum active threads
	 */
	int min() default 1;

	/**
	 * Maximum threads used for listening to this queue. Default is 1 and is
	 * probably NOT production ready.
	 * 
	 * @return the maximum thread count
	 */
	int max() default 1;

	/**
	 * The timeout value for the listener
	 * 
	 * @return the timeout value in milliseconds
	 */
	long timeout() default 0;

	/**
	 * The polling is the manager thread wake period. This can bet turned to reduce
	 * CPU or increase recycling of worker threads. Be careful as a lower number
	 * doesn't mean faster recycling as the runtime for the threads will almost
	 * always be longer that teh poll time.
	 * 
	 * @return the long value for the polling interval in milliseconds
	 */
	long polling() default 10;

	/**
	 * This parameter governs the ability to auto-acknowledge messages by defaulting
	 * to true. Set this to false to handle and then in the listener methods you can
	 * manually acknowledge by changing the value in the Session object.
	 * 
	 * @return the boolean value of the auto-acknowledge function
	 */
	boolean acknowledge() default true;

	/**
	 * Providing a name for the field you want to place your custom Transaction ID
	 * means that Qcat can pass this value through for you and make it available via
	 * the Session object.
	 * 
	 * @return the name of the attribute that contains the transaction ID
	 */
	String transactionId() default "";
}
