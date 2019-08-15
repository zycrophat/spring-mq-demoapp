package steffan.springmqdemoapp.routes.filecopy

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import steffan.springmqdemoapp.services.FileInfo
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.io.File
import java.io.IOException
import java.nio.file.Path

@Component
open class FileCopyProcessor(
    @Value("\${steffan.springmqdemoapp.sampleservice.filecopy.inputdir}")
    private val inputDirectoryPath: String,

    @Value("\${steffan.springmqdemoapp.sampleservice.filecopy.targetdir}")
    private val targetDirectoryPath: String
) : Processor, Logging {

    private val inputDirectory = Path.of(inputDirectoryPath)
    private val targetDirectory = Path.of(targetDirectoryPath)

    override fun process(exchange: Exchange) {
        val fileInfo = exchange.`in`.body as FileInfo
        val srcPath = fileInfo.path

        val relativePath = inputDirectory.relativize(Path.of(srcPath))
        val targetFile = targetDirectory.resolve(relativePath)

        try {
            FileUtils.copyFile(File(srcPath), targetFile.toFile(), true)
            exchange.`in`.body = fileInfo.copy(targetPath = "$targetFile")
            logger().debug("Copied $srcPath -> $targetFile")
        } catch(e: IOException) {
            val msg = "Error copying file $srcPath -> $targetFile"
            logger().error(msg)
            throw RuntimeException(msg)
        }
    }
}
