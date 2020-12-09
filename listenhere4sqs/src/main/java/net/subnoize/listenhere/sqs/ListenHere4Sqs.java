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
					workers.put(queueString, getMesssageWorker(lt, queueString, method, context.getBean(klass)));
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
	public ListenHere4SqsWorker getMesssageWorker(ListenTo to, String queueUrl, Method method, Object target)
			throws NoSuchMethodException {
		return new ListenHere4SqsWorker(to, queueUrl, method, target);
	}
}
