package steffan.springmqdemoapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration

@SpringBootApplication
@Configuration
open class Main

fun main(args: Array<String>) {
    runApplication<Main>(*args)
}


