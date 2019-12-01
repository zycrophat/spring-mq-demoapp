package steffan.springmqdemoapp.sampleservice

import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration

@SpringBootApplication
@Configuration
class Main

fun main(args: Array<String>) = runBlocking<Unit> {
    runApplication<Main>(*args)
}
