package steffan.springmqdemoapp.sampleservice.app.config

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter


@Configuration
@EnableEncryptableProperties
open class SecurityConfiguration : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    protected override fun configure(http: HttpSecurity) {
        http.authorizeRequests().anyRequest().permitAll()
                .and().csrf().disable()
    }
}