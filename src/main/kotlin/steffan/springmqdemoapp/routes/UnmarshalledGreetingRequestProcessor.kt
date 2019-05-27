package steffan.springmqdemoapp.routes

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger


@Component
class UnmarshalledGreetingRequestProcessor : Processor, Logging {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    override fun process(exchange: Exchange) {
        val request = exchange.getIn().getBody(GreetingRequest::class.java)

        logger().info("Hi ${request?.name ?: "stranger"}!")
    }
}