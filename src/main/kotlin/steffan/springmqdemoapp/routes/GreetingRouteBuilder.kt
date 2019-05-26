package steffan.springmqdemoapp.routes

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.builder.xml.Namespaces
import org.apache.camel.processor.idempotent.FileIdempotentRepository
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository
import org.apache.camel.spi.DataFormat
import org.apache.camel.util.AsyncProcessorHelper.process
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import java.io.File


@Component
class GreetingRouteBuilder(
        context: CamelContext?,
        private val jaxbDataFormat: DataFormat,
        private val unmarshalledGreetingRequestProcessor: UnmarshalledGreetingRequestProcessor,
        private val typeConvertingGreetingRequestProcessor: TypeConvertingGreetingRequestProcessor
) : RouteBuilder(context) {

    override fun configure() {
        context.isStreamCaching = true

        from("jms:greetConvert")
                .transacted()
                .idempotentConsumer()
                    .xpath("//g:greetingRequest/g:name/text()",
                            Namespaces("g", "urn:steffan.springmqdemoapp:greeting")
                    )
                    .messageIdRepository(
                            FileIdempotentRepository.fileIdempotentRepository(File("run/greetConvertDedup"))
                    )
                .process(typeConvertingGreetingRequestProcessor)

        from("jms:greetUnmarshall")
                .transacted()
                .unmarshal(jaxbDataFormat)
                .idempotentConsumer()
                    .body(GreetingRequest::class.java, GreetingRequest::getName)
                    .messageIdRepository(
                            FileIdempotentRepository.fileIdempotentRepository(File("run/greetUnmarshallDedup"))
                    )
                .process(unmarshalledGreetingRequestProcessor)
    }
}