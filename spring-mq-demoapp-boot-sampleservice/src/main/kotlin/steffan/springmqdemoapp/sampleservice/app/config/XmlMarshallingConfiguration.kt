package steffan.springmqdemoapp.sampleservice.app.config

import org.apache.camel.converter.jaxb.JaxbDataFormat
import org.reflections.Reflections
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.support.converter.MarshallingMessageConverter
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.XmlRootElement

@Configuration
open class XmlMarshallingConfiguration {

    @Bean
    open fun jaxb2marshaller(): Jaxb2Marshaller {
        val marshaller = Jaxb2Marshaller()
        val xmlRootElementClasses = jaxbClassesToBeBound()
        marshaller.setClassesToBeBound(*(xmlRootElementClasses.toTypedArray()))

        return marshaller
    }

    @Bean
    open fun jaxbClassesToBeBound(): Set<Class<*>> {
        val reflections = Reflections("steffan.springmqdemoapp.api.bindings")
        return reflections.getTypesAnnotatedWith(XmlRootElement::class.java)
    }

    @Bean
    open fun converter(): MarshallingMessageConverter {
        val converter = MarshallingMessageConverter()
        converter.setMarshaller(jaxb2marshaller())
        converter.setUnmarshaller(jaxb2marshaller())

        return converter
    }

    @Bean
    open fun jaxbDataFormat(jaxbContext: JAXBContext): JaxbDataFormat {
        return JaxbDataFormat(jaxbContext)
    }

    @Bean
    open fun jaxbContext(): JAXBContext {
        return JAXBContext.newInstance(*(jaxbClassesToBeBound().toTypedArray()))
    }
}