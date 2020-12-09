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
 * 
 * @author youca
 *
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = {"net.subnoize.listenhere"})
public class ListenHereConfiguration {

	@Autowired
	private ApplicationContext context;

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
			if(StringUtils.isNotBlank(listen.value())) {
				context.getBean(listen.value(),Provider.class).registerListener(klass);
			} else {
				log.error("@Listen used with blank service reference: {}",klass.getName());
			}
		}
	}
}
