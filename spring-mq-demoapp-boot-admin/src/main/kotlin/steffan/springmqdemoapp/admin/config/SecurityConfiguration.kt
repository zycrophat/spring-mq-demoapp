package steffan.springmqdemoapp.admin.config

import de.codecentric.boot.admin.server.config.AdminServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger as configLogger


@Configuration
open class SecurityConfiguration(val adminServer: AdminServerProperties,
                                 val applicationClients: ApplicationClients) : WebSecurityConfigurerAdapter() {

    // required to circumvent name clash with WebSecurityConfigurerAdapter::logger()
    companion object LoggerCompanion : Logging

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

    @Bean
    fun inMemoryUserDetailsManager(): InMemoryUserDetailsManager {
        val manager = InMemoryUserDetailsManager()
        configLogger().info("Importing {} clients:", applicationClients.clients.size)

        applicationClients.clients.forEach { client ->
            manager.createUser(
                    User.builder()
                            .passwordEncoder(passwordEncoder()::encode)
                            .username(client.username)
                            .password(client.password)
                            .roles(*client.roles)
                            .build()
            )
            configLogger().info("Imported client {}", client.username)
        }

        return manager
    }

    @Bean
    open fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

}