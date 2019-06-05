package steffan.springmqdemoapp.admin

import de.codecentric.boot.admin.server.config.EnableAdminServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration

@Configuration
@SpringBootApplication
@EnableAdminServer
open class Main

fun main(args: Array<String>) {
    runApplication<Main>(*args)
}
