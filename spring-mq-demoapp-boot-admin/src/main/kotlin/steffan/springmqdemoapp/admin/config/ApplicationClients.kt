package steffan.springmqdemoapp.admin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


@Component
@ConfigurationProperties("application")
open class ApplicationClients {
    lateinit var clients: List<ApplicationClient>
}
