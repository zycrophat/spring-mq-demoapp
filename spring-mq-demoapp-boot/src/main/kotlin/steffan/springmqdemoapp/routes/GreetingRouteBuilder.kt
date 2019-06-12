package steffan.springmqdemoapp.routes

import org.apache.camel.ExchangePattern
import org.apache.camel.builder.xml.Namespaces
import org.apache.camel.component.infinispan.processor.idempotent.InfinispanIdempotentRepository
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository
import org.apache.camel.spi.DataFormat
import org.apache.camel.spring.SpringRouteBuilder
import org.infinispan.manager.EmbeddedCacheManager
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import javax.sql.DataSource


@Component
class GreetingRouteBuilder(
        private val jaxbDataFormat: DataFormat,
        private val typeConvertingGreetingRequestProcessor: TypeConvertingGreetingRequestProcessor,
        private val unmarshalledGreetingRequestProcessor: UnmarshalledGreetingRequestProcessor,
        private val messageIdDataSource: DataSource,
        private val infiniCacheManager: EmbeddedCacheManager
) : SpringRouteBuilder(), Logging {

    override fun configure() {
        context.isStreamCaching = true

        logger().debug("Start configuring routes")

        from("jms:greetConvert")
                .routeId("greetConvert")
                .transacted()
                .setExchangePattern(ExchangePattern.InOnly)
                .idempotentConsumer()
                    .xpath("//g:greetingRequest/g:name/text()",
                            Namespaces("g", "urn:steffan.springmqdemoapp:greeting")
                    )
                    .messageIdRepository(
                            JdbcMessageIdRepository(messageIdDataSource, TypeConvertingGreetingRequestProcessor::class.simpleName)
                    )
                .process(typeConvertingGreetingRequestProcessor)

        from("jms:greetUnmarshall")
                .routeId("greetUnmarshall")
                .transacted()
                .setExchangePattern(ExchangePattern.InOnly)
                .unmarshal(jaxbDataFormat)
                .idempotentConsumer()
                    .body(GreetingRequest::class.java, GreetingRequest::getName)
                    .messageIdRepository(
                            InfinispanIdempotentRepository(infiniCacheManager, UnmarshalledGreetingRequestProcessor::class.simpleName)
                    )
                .process(unmarshalledGreetingRequestProcessor)

        logger().debug("Finished configuring routes")
    }
}