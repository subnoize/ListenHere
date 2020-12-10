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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import net.subnoize.listenhere.listen.Listen;

/**
 * The configuration class for ListenHere is the processor for the annotations
 * which brings the elements together into a runnable state. The user will see
 * this as Spring magic.
 * 
 * @author John Bryant
 *
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = { "net.subnoize.listenhere" })
public class ListenHereConfiguration {

	@Autowired
	private ApplicationContext context;

	/**
	 * The process by which the various annotated elements are brought together into
	 * a runnable state.
	 * 
	 * @throws ClassNotFoundException
	 */
	@PostConstruct
	public void init() throws ClassNotFoundException {
		Map<String, Object> beans = context.getBeansWithAnnotation(Listen.class);
		for (Object target : beans.values()) {
			Class<?> klass = target.getClass();
			List<Class<?>> interfaces = Arrays.asList(klass.getInterfaces());
			if (interfaces.contains(org.springframework.cglib.proxy.Factory.class)
					|| interfaces.contains(org.springframework.aop.SpringProxy.class)
					|| interfaces.contains(org.springframework.aop.framework.Advised.class)) {
				klass = klass.getSuperclass();
			}
			Listen listen = klass.getAnnotation(Listen.class);
			if (StringUtils.isNotBlank(listen.value())) {
				context.getBean(listen.value(), Provider.class).registerListener(klass);
			} else {
				log.error("@Listen used with blank service reference: {}", klass.getName());
			}
		}
	}
}
