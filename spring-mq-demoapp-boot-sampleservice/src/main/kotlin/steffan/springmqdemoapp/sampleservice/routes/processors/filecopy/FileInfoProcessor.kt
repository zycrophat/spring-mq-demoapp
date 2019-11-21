package steffan.springmqdemoapp.sampleservice.routes.processors.filecopy

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.sampleservice.services.interfaces.FileInfo
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.nio.file.Paths
import java.time.Instant
import java.time.format.DateTimeFormatter

@Component
class FileInfoProcessor: Processor, Logging {

    override fun process(exchange: Exchange) {
        val headers = exchange.`in`.headers
        val message = exchange.`in`

        val fileInfo = FileInfo(
                path = Paths.get(headers["CamelFileAbsolutePath"] as String),
                size = headers[Exchange.FILE_LENGTH] as Long,
                lastModified = DateTimeFormatter.ISO_INSTANT.format(
                        Instant.ofEpochMilli(headers[Exchange.FILE_LAST_MODIFIED] as Long)
                )
        )
        message.body = fileInfo
        logger().debug("FileInfo created for ${fileInfo.path}")
    }
}
