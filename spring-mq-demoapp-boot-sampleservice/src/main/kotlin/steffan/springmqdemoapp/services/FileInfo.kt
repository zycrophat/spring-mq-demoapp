package steffan.springmqdemoapp.services

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class FileInfo(
        val path: String,
        val size: Long = -1,
        val lastModified: String = "",
        val targetPath: String = ""
)