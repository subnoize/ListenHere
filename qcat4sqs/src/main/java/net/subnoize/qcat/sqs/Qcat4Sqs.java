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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
import net.subnoize.qcat.Provider;
import net.subnoize.qcat.listen.ListenTo;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Usage:
 * 
 * Create a SqsAsyncClient bean and annotate your listeners appropriately to
 * begin.
 * 
 * <pre>
 * <code>
 * &#64;Configuration
 * public class SqsConfiguration {	
 *   &#64;Bean
 *   public SqsAsyncClient getSqsAsyncClient() {
 *     return SqsAsyncClient.create();
 *   }
 * }
 * </code>
 * </pre>
 * 
 * @author John Bryant
 *
 */
@Slf4j
@Configuration(Qcat4Sqs.PROVIDER)
@NoArgsConstructor
public class Qcat4Sqs implements Provider, ApplicationListener<ContextClosedEvent> {

	public static final String PROVIDER = "Qcat4Sqs";

	@Autowired
	private SqsAsyncClient asyncClient;

	@Autowired
	private ApplicationContext context;

	private List<Qcat4SqsWorker> workers = new ArrayList<>();

	public void shutdown() {
		workers.forEach(Qcat4SqsWorker::shutdown);
		asyncClient.close();
	}

	@Override
	public void registerListener(Class<?> klass) {
		for (Method method : klass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(ListenTo.class)) {
				try {
					workers.add(getMesssageWorker(method, context.getBean(klass)));
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

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public Qcat4SqsWorker getMesssageWorker(Method method, Object target) {
		return new Qcat4SqsWorker(getExecutionTemplate(method, target));
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public SqsExecutionTemplate getExecutionTemplate(Method method, Object target) {
		SqsExecutionTemplate temp = new SqsExecutionTemplate();
		temp.setMethod(method);
		temp.setTarget(target);
		return temp;
	}
}