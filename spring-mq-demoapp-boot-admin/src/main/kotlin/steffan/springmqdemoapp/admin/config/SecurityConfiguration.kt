package steffan.springmqdemoapp.admin.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import de.codecentric.boot.admin.server.config.AdminServerProperties




@Configuration
open class SecurityConfiguration(val adminServer: AdminServerProperties) : WebSecurityConfigurerAdapter() {


    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.authorizeRequests()
                .anyRequest()
                .permitAll()
                .and()
                .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringAntMatchers(this.adminServer.contextPath.plus("/instances"), this.adminServer.contextPath.plus("/actuator/**"))
    }
}