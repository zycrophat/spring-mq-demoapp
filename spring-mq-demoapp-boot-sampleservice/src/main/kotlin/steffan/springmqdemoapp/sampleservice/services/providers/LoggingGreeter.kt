package steffan.springmqdemoapp.sampleservice.services.providers

import org.springframework.stereotype.Component
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import steffan.springmqdemoapp.sampleservice.services.interfaces.Greeter
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger

@Component
class LoggingGreeter: Greeter, Logging {

    override fun greet(greetingRequest: GreetingRequest) {
        logger().info("Hi ${greetingRequest.name ?: "stranger"} ${greetingRequest.dateTimeOfGreet}!")
    }

}
