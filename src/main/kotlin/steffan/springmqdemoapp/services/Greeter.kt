package steffan.springmqdemoapp.services

import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger

@Component
open class Greeter : Logging {

    @Transactional()
    @JmsListener(destination = "mailbox", containerFactory = "myFactory")
    fun greet(@Payload msg : String) {
        logger().info("Hello ${msg}")
    }
}