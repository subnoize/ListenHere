package net.subnoize.listenhere.listen;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface ListenTo {
	String value();
	int min() default 1;
	int max() default 1;
	long timeout() default 0;
	long polling() default 10;
	boolean acknowledge() default true;
	String transactionId() default "";
}
