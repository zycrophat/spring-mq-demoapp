package steffan.springmqdemoapp.sampleservice.routes

import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
open class JdbcMessageIdRepositoryFactory(
        private val messageIdDataSource: DataSource,
        @Value("\${steffan.springmqdemoapp.sampleservice.camel.message-id-max-length}")
        private val messageIdMaxLength: Int
) {

    private val createStringWithMaxLength =
            """|CREATE TABLE CAMEL_MESSAGEPROCESSED (
               |  processorName VARCHAR(255),
               |  messageId VARCHAR($messageIdMaxLength),
               |  createdAt TIMESTAMP
               |)""".trimMargin()

    fun createIdempotentRepository(processorName: String?): JdbcMessageIdRepository =
            JdbcMessageIdRepository(messageIdDataSource, processorName).apply {
        createString = createStringWithMaxLength
    }

}