package steffan.springmqdemoapp.sampleservice.services.interfaces

import com.fasterxml.jackson.annotation.JsonInclude
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class FileInfo(
        val path: Path,
        val size: Long = -1,
        val lastModified: Instant = Instant.EPOCH
) {

    constructor(path: Path): this(
            path,
            Files.size(path),
            Files.getLastModifiedTime(path).toInstant()
    )
}