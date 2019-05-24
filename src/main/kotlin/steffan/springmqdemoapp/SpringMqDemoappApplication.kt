package steffan.springmqdemoapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer

@SpringBootApplication
@Configuration
open class SpringMqDemoappApplication {

}

fun main(args: Array<String>) {
	runApplication<SpringMqDemoappApplication>(*args)
}
