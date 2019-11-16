package steffan.springmqdemoapp.sampleservice.routes.processors.foologging

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import javax.transaction.Transactional

@Component
class FooLoggingProcessor: Processor, Logging {

    @Transactional
    override fun process(exchange: Exchange) {
        val name = exchange.getIn().getBody(String::class.java)

        for (x in 0..10) {
            logger().info("Hello %s %02d".format(name, x))
            Thread.sleep(1000)
        }

    }

}
