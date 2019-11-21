package steffan.springmqdemoapp.sampleservice.services.interfaces

import com.fasterxml.jackson.annotation.JsonInclude
import java.nio.file.Path

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class FileInfo(
        val path: Path,
        val size: Long = -1,
        val lastModified: String = ""
)
