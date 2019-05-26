package steffan.springmqdemoapp.routes

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.spi.DataFormat
import org.springframework.stereotype.Component


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
                .process(typeConvertingGreetingRequestProcessor)

        from("jms:greetUnmarshall")
                .transacted()
                .unmarshal(jaxbDataFormat)
                .process(unmarshalledGreetingRequestProcessor)
    }
}