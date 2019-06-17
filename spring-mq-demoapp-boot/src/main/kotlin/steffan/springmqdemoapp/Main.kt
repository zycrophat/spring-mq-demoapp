package steffan.springmqdemoapp

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Configuration
import steffan.springmqdemoapp.AppCtxHolder.ctx

@SpringBootApplication
@Configuration
open class Main

object AppCtxHolder {
    lateinit var ctx: ConfigurableApplicationContext
}

fun main(args: Array<String>) {
    ctx = runApplication<Main>(*args)
}

fun stop() {
    SpringApplication.exit(ctx)
}


