package steffan.springmqdemoapp.routes

import org.apache.camel.Exchange
import org.apache.camel.ExchangePattern
import org.apache.camel.builder.xml.Namespaces
import org.apache.camel.component.infinispan.processor.idempotent.InfinispanIdempotentRepository
import org.apache.camel.model.dataformat.JsonLibrary
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository
import org.apache.camel.spi.DataFormat
import org.apache.camel.spring.SpringRouteBuilder
import org.infinispan.manager.EmbeddedCacheManager
import org.quartz.DateBuilder
import org.quartz.DateBuilder.futureDate
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import steffan.springmqdemoapp.routes.filecopy.FileCopyProcessor
import steffan.springmqdemoapp.routes.filecopy.FileInfoProcessor
import steffan.springmqdemoapp.routes.greet.TypeConvertingGreetingRequestProcessor
import steffan.springmqdemoapp.routes.greet.UnmarshalledGreetingRequestProcessor
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.sql.DataSource


@Component
class SampleRouteBuilder(
        private val jaxbDataFormat: DataFormat,
        private val typeConvertingGreetingRequestProcessor: TypeConvertingGreetingRequestProcessor,
        private val unmarshalledGreetingRequestProcessor: UnmarshalledGreetingRequestProcessor,
        private val messageIdDataSource: DataSource,
        private val infiniCacheManager: EmbeddedCacheManager,
        private val fileInfoProcessor: FileInfoProcessor,
        private val fileCopyProcessor: FileCopyProcessor,
        @Value("\${filecopy.inputdir}")
        private val inputDirectoryPath: String,
        private val scheduler: Scheduler
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

        from("file://$inputDirectoryPath?recursive=true&noop=true&" +
                "idempotentRepository=#fileInputIdempotentRepository&readLock=idempotent")
                .onException(Exception::class.java)
                    .maximumRedeliveries(10)
                    .handled(true)
                    .process { e ->
                        logger().warn("Reached maximum redelivery count. Stopping route ${e.fromRouteId}")
                        e.context.stopRoute("readFiles", 5, TimeUnit.SECONDS)
                        scheduleRouteRestart(e)
                    }
                    .rollback()
                .end()
                .transacted()
                .routeId("readFiles")
                .process(fileInfoProcessor).id("fileInfoProcessor")
                .process(fileCopyProcessor).id("fileCopyProcessor")
                .marshal().json(JsonLibrary.Jackson)
                .to("jms:filesToMove")

        logger().info("Finished configuring routes")
    }

    private fun scheduleRouteRestart(e: Exchange) {
        scheduler.scheduleJob(
                JobBuilder.newJob()
                        .ofType(RouteRestartQuartzJob::class.java)
                        .withIdentity("restart ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                                LocalDateTime.now())}"
                        )
                        .usingJobData("routeId", e.fromRouteId)
                        .build(),
                TriggerBuilder.newTrigger()
                        .startAt(
                                futureDate(20, DateBuilder.IntervalUnit.SECOND)
                        ).build()
        )
    }
}

