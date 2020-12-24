<div style="text-align:center;float:left; margin-right: 20px;"><img src="q/q-200.png" alt="Qcat is fat and lazy like me"/></div>

# <img src="q/Q-logo-32.png" alt="Qcat is fat and lazy like me" />cat is a transparent, vendor agnostic messaging for Lazy Cats and Humans alike


A Spring Boot Starter that automates much of the boiler plate tasks associated with messaging based application development. The core is vendor neutral and lends itself to integration with most brokers.

Qcat grew out of fustration with more mainstream annotation based system that either were more complex than the underlying framework or not yet supporting Java 11.

## <img src="q/Q-logo-32.png" alt="Qcat is fat and lazy like me" />cat for SQS (qcat4sqs)

The first Provider for Qcat is for AWS SQS Java 2.x API. Simply configure a Spring Bean for the service and annotate your listeners and you are off to another cat nap instead of banging your head against bad architecture.

Maven Dependency;
```
<dependency>
	<groupId>net.subnoize</groupId>
	<artifactId>qcat4sqs</artifactId>
	<version>0.0.8-SNAPSHOT</version>
</dependency>
```

Example Configuration;
```
package net.subnoize.examples;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsConfiguration {
	
	@Bean
	public SqsAsyncClient getSqsAsyncClient() {
		return SqsAsyncClient.create();
	}
	
}
```

Example Usage;
```
package net.subnoize.examples;

import org.springframework.stereotype.Controller;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.subnoize.qcat.listen.Listen;
import net.subnoize.qcat.listen.ListenTo;
import net.subnoize.qcat.model.Payload;
import net.subnoize.qcat.send.SendTo;
import net.subnoize.qcat.sqs.Qcat4Sqs;

@Controller
@Slf4j
@NoArgsConstructor
@Listen(Qcat4Sqs.PROVIDER)
public class TestQueues {
	
	@ListenTo(value = "Test_1", transactionId = "txnid")
	@SendTo("Test_2")
	public String test1Queue(@Payload String msg) {
		log.info("Test 1: {}",msg);
		return "Hello, "+msg;
	}
	
}

```

*NOTE: We are working on making the line `@Listen(Qcat4Sqs.PROVIDER)` have a default if no provider is specified AND make that String able to pull from the Spring configuration stack so it can be a true LIB and CONFIG change with zero coding when you change brokers. The neat part is you can mix different messaging brokers in the same application.
