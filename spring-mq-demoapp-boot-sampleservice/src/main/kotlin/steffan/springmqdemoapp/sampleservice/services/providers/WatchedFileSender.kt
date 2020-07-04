package steffan.springmqdemoapp.sampleservice.services.providers

import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import steffan.springmqdemoapp.sampleservice.services.interfaces.FileInfo
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.nio.file.Path

@Component
class WatchedFileSender(@Autowired private val producerTemplate: ProducerTemplate) : Logging {

    @Transactional
    fun send(path: Path) {
        val fileInfo = FileInfo(path)
        logger().debug("Sending file path to route: $fileInfo")
        producerTemplate.sendBody(
                "direct:newFiles",
                fileInfo
        )
    }


}