package steffan.springmqdemoapp.app.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter


@Configuration
open class SecurityConfiguration : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    protected override fun configure(http: HttpSecurity) {
        http.authorizeRequests().anyRequest().permitAll()
                .and().csrf().disable()
    }
}