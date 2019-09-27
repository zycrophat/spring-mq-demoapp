package steffan.springmqdemoapp.sampleservice.routes.greet

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import steffan.springmqdemoapp.sampleservice.services.interfaces.Greeter
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger


@Component
open class UnmarshalledGreetingRequestProcessor(private val greeter: Greeter) : Processor, Logging {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    override fun process(exchange: Exchange) {
        val request = exchange.getIn().getBody(GreetingRequest::class.java)

        greeter.greet(request)
        throw RuntimeException("Der Fehler")
    }
}