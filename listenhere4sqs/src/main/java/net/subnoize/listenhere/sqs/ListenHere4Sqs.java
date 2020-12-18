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

package net.subnoize.listenhere.sqs;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextClosedEvent;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.subnoize.listenhere.Provider;
import net.subnoize.listenhere.listen.ListenTo;
import net.subnoize.listenhere.util.ConfigurationUtils;

/**
 * Usage:
 * 
 * Create a SqsAsyncClient bean and annotate your listeners appropriately to begin.
 * 
 * <pre><code>
 * &#64;Configuration
 * public class SqsConfiguration {	
 *   &#64;Bean
 *   public SqsAsyncClient getSqsAsyncClient() {
 *     return SqsAsyncClient.create();
 *   }
 * }
 * </code></pre>
 * 
 * @author John Bryant
 *
 */
@Slf4j
@Configuration(ListenHere4Sqs.PROVIDER)
@NoArgsConstructor
public class ListenHere4Sqs implements Provider, ApplicationListener<ContextClosedEvent> {

	public static final String PROVIDER = "ListenHere4Sqs";

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ConfigurationUtils helper;

	private Map<String, ListenHere4SqsWorker> workers = new HashMap<>();

	public void shutdown() {
		workers.entrySet().forEach(e -> e.getValue().shutdown());
	}

	@Override
	public void registerListener(Class<?> klass) {
		for (Method method : klass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(ListenTo.class)) {
				try {
					ListenTo lt = method.getAnnotation(ListenTo.class);
					String queueString = lt.value();
					if (queueString.contains("${")) {
						queueString = helper.getString(queueString);
					}
					workers.put(queueString, getMesssageWorker(getExecutionTemplate(lt, queueString, method, context.getBean(klass))));
					log.info("ListenTo: {}.{}('{}',{},{},{},{})", klass.getName(), method.getName(), queueString,
							lt.min(), lt.max(), lt.timeout(), lt.polling());
				} catch (Exception e) {
					log.error("Error creating method and target for listener worker", e);
				}
			}
		}
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		shutdown();
	}

	@Bean(name = "ListenHere4SqsWorker")
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public ListenHere4SqsWorker getMesssageWorker(SqsExecutionTemplate template) {
		return new ListenHere4SqsWorker(template);
	}

	@Bean(name = "SqsExecutionTemplate")
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public SqsExecutionTemplate getExecutionTemplate(ListenTo to, String queueUrl, Method method, Object target)
			throws NoSuchMethodException {
		SqsExecutionTemplate temp = new SqsExecutionTemplate();
		temp.setQueueUrl(queueUrl);
		temp.setMethod(method);
		temp.setTarget(target);
		temp.setTo(to);
		return temp;
	}
}
