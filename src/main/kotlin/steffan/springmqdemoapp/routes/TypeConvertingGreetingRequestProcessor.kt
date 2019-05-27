package steffan.springmqdemoapp.routes

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.TypeConverter
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger


@Component
class TypeConvertingGreetingRequestProcessor(private val typeConverter: TypeConverter) : Processor, Logging {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    override fun process(exchange: Exchange) {
        val request = typeConverter
                .convertTo(GreetingRequest::class.java, exchange, exchange.getIn().body)

        logger().info("Hi ${request?.name ?: "stranger"}!")
    }
}