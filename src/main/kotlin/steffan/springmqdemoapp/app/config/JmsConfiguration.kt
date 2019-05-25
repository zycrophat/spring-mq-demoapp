package steffan.springmqdemoapp.app.config


import org.apache.activemq.ActiveMQConnectionFactory
import org.reflections.Reflections
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.connection.CachingConnectionFactory
import org.springframework.jms.support.converter.MarshallingMessageConverter
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.transaction.annotation.EnableTransactionManagement
import steffan.springmqdemoapp.util.Logging
import javax.xml.bind.annotation.XmlRootElement


@Configuration
@EnableTransactionManagement
@EnableJms
open class JmsConfiguration : Logging {

    @Value("\${spring.activemq.broker-url}")
    val brokerUrl: String? = null

    @Value("\${spring.activemq.user}")
    val user: String? = null

    @Value("\${spring.activemq.password}")
    val pass: String? = null

    @Bean
    open fun activeMQConnectionFactory(): ActiveMQConnectionFactory {
        val activeMQConnectionFactory = ActiveMQConnectionFactory().apply {
            brokerURL = brokerUrl
            userName = user
            password = pass
        }

        return activeMQConnectionFactory
    }

    @Bean
    open fun cachingConnectionFactory(): CachingConnectionFactory {
        return CachingConnectionFactory(activeMQConnectionFactory())
    }

    @Bean
    open fun myFactory(configurer: DefaultJmsListenerContainerFactoryConfigurer) :
            JmsListenerContainerFactory<*> {
        val factory = DefaultJmsListenerContainerFactory()
        configurer.configure(factory, cachingConnectionFactory())
        return factory
    }

    @Bean
    open fun jaxb2marshaller() : Jaxb2Marshaller {
        val marshaller = Jaxb2Marshaller()
        val reflections = Reflections("steffan.springmqdemoapp.api.messages")
        val xmlRootElementClasses = reflections.getTypesAnnotatedWith(XmlRootElement::class.java)
        marshaller.setClassesToBeBound(*(xmlRootElementClasses.toTypedArray()))

        return marshaller
    }

    @Bean
    open fun converter() : MarshallingMessageConverter {
        val converter = MarshallingMessageConverter()
        converter.setMarshaller(jaxb2marshaller())
        converter.setUnmarshaller(jaxb2marshaller())

        return converter
    }

}
