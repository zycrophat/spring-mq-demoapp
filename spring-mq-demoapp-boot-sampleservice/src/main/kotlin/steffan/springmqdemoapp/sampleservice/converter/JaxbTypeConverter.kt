package steffan.springmqdemoapp.sampleservice.converter

import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.stereotype.Component
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext

@Component
class JaxbTypeConverter(private val jaxbClassesToBeBound: Set<Class<*>>): GenericConverter {

    private val jaxbContext = JAXBContext.newInstance(*jaxbClassesToBeBound.toTypedArray())

    override fun getConvertibleTypes(): MutableSet<GenericConverter.ConvertiblePair> {
        return jaxbClassesToBeBound.asSequence().map { c -> GenericConverter.ConvertiblePair(CharSequence::class.java, c) }.toMutableSet()
    }

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any =
        source?.let {
            if (sourceTypeIsACharSequence(sourceType) && typeIsSupported(targetType)) {
                return@convert unmarshal(it)
            } else if (targetTypeIsACharSequence(targetType) && typeIsSupported(sourceType)){
                return@convert marshal(it)
            }
            throw IllegalArgumentException("Cannot convert ${sourceType.objectType} to ${targetType.objectType}")
        } ?:
            throw IllegalArgumentException("Cannot convert null")

    private fun marshal(source: Any): Any {
        val marshaller = jaxbContext.createMarshaller()
        StringWriter().use {
            return marshaller.marshal(source, it)
        }
    }

    private fun unmarshal(source: Any): Any {
        val unmarshaller = jaxbContext.createUnmarshaller()
        StringReader(source as String).use {
            return unmarshaller.unmarshal(it)
        }
    }

    private fun typeIsSupported(sourceType: TypeDescriptor) =
            jaxbClassesToBeBound.contains(sourceType.objectType)

    private fun targetTypeIsACharSequence(targetType: TypeDescriptor) =
            targetType.objectType.isAssignableFrom(CharSequence::class.java)

    private fun sourceTypeIsACharSequence(sourceType: TypeDescriptor) =
            CharSequence::class.java.isAssignableFrom(sourceType.objectType)
}
