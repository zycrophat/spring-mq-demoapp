package steffan.springmqdemoapp.admin.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import de.codecentric.boot.admin.server.config.AdminServerProperties
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler


@Configuration
open class SecurityConfiguration(val adminServer: AdminServerProperties) : WebSecurityConfigurerAdapter() {


    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        val successHandler = SavedRequestAwareAuthenticationSuccessHandler()
        successHandler.setTargetUrlParameter("redirectTo")
        successHandler.setDefaultTargetUrl(this.adminServer.contextPath.plus("/"))

        http.authorizeRequests()
                .antMatchers(this.adminServer.contextPath.plus("/assets/**")).permitAll()
                .antMatchers(this.adminServer.contextPath.plus("/login")).permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin().loginPage(this.adminServer.contextPath.plus("/login")).successHandler(successHandler).and()
                .logout().logoutUrl(this.adminServer.contextPath.plus("/logout")).and()
                .httpBasic().and()
                .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringAntMatchers(
                        this.adminServer.contextPath.plus("/instances"),
                        this.adminServer.contextPath.plus("/actuator/**")
                )
    }
}