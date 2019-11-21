package steffan.springmqdemoapp.sampleservice.routes.processors.filecopy

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import steffan.springmqdemoapp.sampleservice.services.interfaces.FileInfo
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Component
class FileCopyProcessor(
    @Value("\${steffan.springmqdemoapp.sampleservice.filecopy.inputdir}")
    private val inputDirectoryPath: Path,

    @Value("\${steffan.springmqdemoapp.sampleservice.filecopy.targetdir}")
    private val targetDirectoryPath: Path
) : Processor, Logging {

    private val inputDirectory = inputDirectoryPath.toAbsolutePath()
    private val targetDirectory = targetDirectoryPath

    override fun process(exchange: Exchange) {
        val fileInfo = exchange.`in`.body as FileInfo
        val srcPath = fileInfo.path

        val relativePath = inputDirectory.relativize(srcPath)
        val targetFile = targetDirectory.resolve(relativePath)

        try {
            Files.createDirectories(targetFile.parent)
            Files.copy(srcPath, targetFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
            exchange.`in`.body = fileInfo.copy(path = targetFile)
            logger().debug("Copied $srcPath -> $targetFile")
        } catch(e: IOException) {
            val msg = "Error copying file $srcPath -> $targetFile"
            logger().error(msg)
            throw RuntimeException(msg)
        }
    }
}
