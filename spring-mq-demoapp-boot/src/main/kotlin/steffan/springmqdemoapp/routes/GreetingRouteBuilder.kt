package steffan.springmqdemoapp.routes

import org.apache.camel.builder.xml.Namespaces
import org.apache.camel.component.infinispan.processor.idempotent.InfinispanIdempotentRepository
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository
import org.apache.camel.spi.DataFormat
import org.apache.camel.spring.SpringRouteBuilder
import org.infinispan.manager.EmbeddedCacheManager
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import javax.sql.DataSource


@Component
class GreetingRouteBuilder(
        private val jaxbDataFormat: DataFormat,
        private val typeConvertingGreetingRequestProcessor: TypeConvertingGreetingRequestProcessor,
        private val unmarshalledGreetingRequestProcessor: UnmarshalledGreetingRequestProcessor,
        private val messageIdDataSource: DataSource,
        private val infiniCacheManager: EmbeddedCacheManager
) : SpringRouteBuilder() {

    override fun configure() {
        context.isStreamCaching = true

        from("jms:greetConvert")
                .routeId("greetConvert")
                //.transacted()
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
                .unmarshal(jaxbDataFormat)
                .idempotentConsumer()
                    .body(GreetingRequest::class.java, GreetingRequest::getName)
                    .messageIdRepository(
                            InfinispanIdempotentRepository(infiniCacheManager, UnmarshalledGreetingRequestProcessor::class.simpleName)
                    )
                .process(unmarshalledGreetingRequestProcessor)


    }
}