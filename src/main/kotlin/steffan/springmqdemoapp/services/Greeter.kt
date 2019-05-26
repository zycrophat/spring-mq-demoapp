package steffan.springmqdemoapp.services

import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger

@Component
open class Greeter : Logging {

    @Transactional()
    @JmsListener(destination = "greetMdb", containerFactory = "jmsListenerContainerFactory")
    fun greet(msg: Message<GreetingRequest>) {
        logger().info("Hello ${msg.payload.name}")
    }
}