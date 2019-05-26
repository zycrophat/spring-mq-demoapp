package steffan.springmqdemoapp

import de.codecentric.boot.admin.server.config.EnableAdminServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration

@SpringBootApplication
@EnableAdminServer
@Configuration
open class Main

fun main(args: Array<String>) {
    runApplication<Main>(*args)
}
