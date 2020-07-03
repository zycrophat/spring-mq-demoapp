package steffan.springmqdemoapp.dpapipropertysupport

import com.github.windpapi4j.WinDPAPI
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.*
import steffan.springmqdemoapp.dpapipropertysupport.Constants.dpApiPropertySourceName
import steffan.springmqdemoapp.dpapipropertysupport.Constants.entropyPropertyName
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.util.*
import java.util.stream.StreamSupport
import kotlin.streams.asSequence


class JDPAPIEnvironmentPostProcessor : EnvironmentPostProcessor, Logging, Ordered {

    private val propertyRegex = """^DPAPI\((.*)\)$""".toRegex()

    private val base64Decoder = Base64.getDecoder()

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication?) {
        if(WinDPAPI.isPlatformSupported()) {
            logger().info("Detected Windows operating system")
            val winDPAPI = createWinDPAPI()

            val entropy = loadOptionalEntropy(environment)
            val clearTextPropertiesMap = createPropertyKeyValueStream(environment)
                    .map { Pair(it.first, propertyRegex.matchEntire(it.second))}
                    .filter { it.second != null }
                    .map { Pair(it.first, it.second!!.groups[1]?.value!!)} // get the cipher text from DPAPI(<ciphertext>) string
                    .map {
                        val propertyName = it.first
                        val cipherText = it.second

                        val clearText = String(winDPAPI.unprotectData(base64Decoder.decode(cipherText), entropy.orElse(null)), Charsets.UTF_8)

                        propertyName to clearText
                    }
                    .toMap()

            logger().info("Decrypted ${clearTextPropertiesMap.size} DPAPI encrypted properties")
            if (clearTextPropertiesMap.isNotEmpty()) {
                logger().info("Adding dpapiPropertySource")
                environment.propertySources.addFirst(MapPropertySource(dpApiPropertySourceName, clearTextPropertiesMap))
            }
        } else {
            logger().warn("Detected a non-Windows operating system. DPAPI encrypted properties will not be decrypted")
        }
    }

    private fun createWinDPAPI() = WinDPAPI.newInstance(WinDPAPI.CryptProtectFlag.CRYPTPROTECT_UI_FORBIDDEN)

    private fun createPropertyKeyValueStream(environment: ConfigurableEnvironment): Sequence<Pair<String, String>> {
        val propSrcs = environment.propertySources
        return StreamSupport.stream(propSrcs.spliterator(), false)
                .filter { ps: PropertySource<*>? -> ps is EnumerablePropertySource<*> }
                .map { ps: PropertySource<*> -> (ps as EnumerablePropertySource<*>).propertyNames }
                .flatMap<String>(Arrays::stream)
                .map { Pair(it, environment[it] ?: "") }
                .asSequence()
    }

    private fun loadOptionalEntropy(environment: ConfigurableEnvironment) =
        Optional
                .ofNullable(environment.getProperty(entropyPropertyName))
                .map(base64Decoder::decode)

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

}