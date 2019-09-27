package steffan.springmqdemoapp.sampleservice.routes

import org.apache.camel.Exchange
import org.apache.camel.ExchangePattern
import org.apache.camel.builder.xml.Namespaces
import org.apache.camel.component.infinispan.processor.idempotent.InfinispanIdempotentRepository
import org.apache.camel.model.dataformat.JsonLibrary
import org.apache.camel.processor.RedeliveryPolicy
import org.apache.camel.spi.DataFormat
import org.apache.camel.spring.SpringRouteBuilder
import org.infinispan.manager.EmbeddedCacheManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.api.bindings.GreetingRequest
import steffan.springmqdemoapp.sampleservice.routes.filecopy.FileCopyProcessor
import steffan.springmqdemoapp.sampleservice.routes.filecopy.FileInfoProcessor
import steffan.springmqdemoapp.sampleservice.routes.greet.TypeConvertingGreetingRequestProcessor
import steffan.springmqdemoapp.sampleservice.routes.greet.UnmarshalledGreetingRequestProcessor
import steffan.springmqdemoapp.sampleservice.services.interfaces.FileInfo
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.time.format.DateTimeFormatter
import java.util.*


@Component
class SampleRouteBuilder(
        private val jaxbDataFormat: DataFormat,
        private val typeConvertingGreetingRequestProcessor: TypeConvertingGreetingRequestProcessor,
        private val unmarshalledGreetingRequestProcessor: UnmarshalledGreetingRequestProcessor,
        private val infiniCacheManager: EmbeddedCacheManager,
        private val fileInfoProcessor: FileInfoProcessor,
        private val fileCopyProcessor: FileCopyProcessor,
        @Value("\${steffan.springmqdemoapp.sampleservice.filecopy.inputdir}")
        private val inputDirectoryPath: String,
        private val jdbcMessageIdRepositoryFactory: JdbcMessageIdRepositoryFactory,
        @Value("\${steffan.springmqdemoapp.sampleservice.camel.message-id-max-length}")
        private val messageIdMaxLength: Int
) : SpringRouteBuilder(), Logging {

    private val redeliveryByCamelPolicy = RedeliveryPolicy().apply {
        maximumRedeliveries = 10
        delayPattern = "1:1000;4:2000;8:5000;10:30000"
    }

    override fun configure() {
        configureContextScoped()

        logger().debug("Start configuring routes")

        configureGreetConvertRoute()
        configureGreetUnmarshallRoute()

        configureCreateFilesRoute()
        configureReadFilesRoute()
        configureCopyFilesRoute()

        logger().info("Finished configuring routes")
    }

    private fun configureContextScoped() {
        context.isStreamCaching = true
    }

    private fun configureGreetConvertRoute() {
        from("jms:greetConvert")
                .routeId("greetConvert")
                .transacted()
                .setExchangePattern(ExchangePattern.InOnly)
                .idempotentConsumer()
                .xpath("//g:greetingRequest/g:name/text()",
                        Namespaces("g", "urn:steffan.springmqdemoapp:greeting")
                )
                .messageIdRepository(
                        jdbcMessageIdRepositoryFactory.createIdempotentRepository(TypeConvertingGreetingRequestProcessor::class.simpleName)
                )
                .process(typeConvertingGreetingRequestProcessor)
    }

    private fun configureGreetUnmarshallRoute() {
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
    }

    private fun configureCopyFilesRoute() {
        from("jms:filesToCopy?concurrentConsumers=12")
                .autoStartup(true)
                .routeId("copyFiles")
                .transacted()
                .unmarshal().json(JsonLibrary.Jackson, FileInfo::class.java)
                .idempotentConsumer()
                    .body(FileInfo::class.java) { b -> b.path.takeLast(messageIdMaxLength).reversed() }
                    .messageIdRepository(
                            jdbcMessageIdRepositoryFactory.createIdempotentRepository("fileCopyProcessor")
                    )
                .process(fileCopyProcessor).id("fileCopyProcessor")
                .marshal().json(JsonLibrary.Jackson)
                .to("jms:filesToMove")
    }

    private fun configureReadFilesRoute() {
        from("file://$inputDirectoryPath?recursive=true&noop=true")
                .errorHandler(defaultErrorHandler().apply { this.redeliveryPolicy = redeliveryByCamelPolicy })
                .onException(RuntimeException::class.java)
                    .redeliveryPolicy(redeliveryByCamelPolicy)
                    .handled(true)
                    .rollback()
                .end()
                .routeId("readFiles")
                .transacted()
                .process(fileInfoProcessor).id("fileInfoProcessor")
                .marshal().json(JsonLibrary.Jackson)
                .to("jms:filesToCopy")
    }

    private fun configureCreateFilesRoute() {
        from("timer:foo?repeatCount=1000000&period=1&delay=0")
                .autoStartup(false)
                .routeId("createFiles")
                .process { e ->
                    val counter = e.properties[Exchange.TIMER_COUNTER] as Long
                    val date = e.properties[Exchange.TIMER_FIRED_TIME] as Date
                    e.`in`.body = "$counter,${DateTimeFormatter.ISO_INSTANT.format(date.toInstant())}"
                    e.`in`.headers[Exchange.FILE_NAME] = "$counter.txt"
                }
                .to("file://$inputDirectoryPath?fileExist=Ignore")
    }

}

