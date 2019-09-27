package steffan.springmqdemoapp.sampleservice.messaging

import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import steffan.springmqdemoapp.sampleservice.services.interfaces.Greeter

@Component
open class GreetMdb(private val greeter: Greeter) {

    @Transactional
    @JmsListener(destination = "greetMdb", containerFactory = "jmsListenerContainerFactory")
    fun greet(msg: Message<GreetingRequest>) {
        greeter.greet(msg.payload)
    }

}