package steffan.springmqdemoapp.dpapipropertysupport

import com.github.windpapi4j.WinDPAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.get
import steffan.springmqdemoapp.util.Logging
import java.security.SecureRandom
import java.util.*


class JDPAPIEnvironmentPostProcessorTest : Logging {

    @Test
    fun `postProcessEnvironment must decrypt protected properties with entropy provided`() {
        assumeTrue(WinDPAPI.isPlatformSupported(), "Test can only run on windows")

        val jdpapiEnvironmentPostProcessor = JDPAPIEnvironmentPostProcessor()
        val environment: ConfigurableEnvironment = StandardEnvironment()
        val propertySources = environment.propertySources

        val winDPAPI = WinDPAPI.newInstance(WinDPAPI.CryptProtectFlag.CRYPTPROTECT_UI_FORBIDDEN)
        val entropy = SecureRandom().generateSeed(256)
        val base64Encoder = Base64.getEncoder()
        val clearTextPropertyMap =
                mapOf(
                        "encFoo" to "secretBar",
                        "bla.encBla" to "secretBlubb"
                )

        val cipherTextPropertyMap =
                clearTextPropertyMap
                        .mapValues { entry ->
                            "DPAPI(${String(base64Encoder.encode(winDPAPI.protectData(entry.value.toByteArray(), entropy)))})"
                        }

        val unprotectedPropertyMap = mapOf(
                "foo" to "bar",
                "bla.bla" to "blubb",
                Constants.entropyPropertyName to String(base64Encoder.encode(entropy))
        )

        val combinedPropertyMap = cipherTextPropertyMap.plus(unprotectedPropertyMap)
        propertySources.addFirst(MapPropertySource("MY_MAP", combinedPropertyMap))
        jdpapiEnvironmentPostProcessor.postProcessEnvironment(environment, null)

        assertEquals("bar", environment.getProperty("foo"))
        assertAll("All properties must be decrypted",
                clearTextPropertyMap.plus(unprotectedPropertyMap)
                        .map {
                            { assertEquals(it.value, environment[it.key])}
                        }
        )
    }

    @Test
    fun `postProcessEnvironment must decrypt protected properties without entropy provided`() {
        assumeTrue(WinDPAPI.isPlatformSupported(), "Test can only run on windows")

        val jdpapiEnvironmentPostProcessor = JDPAPIEnvironmentPostProcessor()
        val environment: ConfigurableEnvironment = StandardEnvironment()
        val propertySources = environment.propertySources

        val winDPAPI = WinDPAPI.newInstance(WinDPAPI.CryptProtectFlag.CRYPTPROTECT_UI_FORBIDDEN)
        val base64Encoder = Base64.getEncoder()
        val clearTextPropertyMap =
                mapOf(
                        "encFoo" to "secretBar",
                        "bla.encBla" to "secretBlubb"
                )

        val cipherTextPropertyMap =
                clearTextPropertyMap
                        .mapValues { entry ->
                            "DPAPI(${String(base64Encoder.encode(winDPAPI.protectData(entry.value.toByteArray())))})"
                        }

        val unprotectedPropertyMap = mapOf(
                "foo" to "bar",
                "bla.bla" to "blubb"
        )

        val combinedPropertyMap = cipherTextPropertyMap.plus(unprotectedPropertyMap)
        propertySources.addFirst(MapPropertySource("MY_MAP", combinedPropertyMap))
        jdpapiEnvironmentPostProcessor.postProcessEnvironment(environment, null)

        assertEquals("bar", environment.getProperty("foo"))
        assertAll("All properties must be decrypted",
                clearTextPropertyMap.plus(unprotectedPropertyMap)
                        .map {
                            { assertEquals(it.value, environment[it.key])}
                        }
        )
    }

    @Test
    fun `order must be lowest precedence`() {
        val jdpapiEnvironmentPostProcessor = JDPAPIEnvironmentPostProcessor()
        val order = jdpapiEnvironmentPostProcessor.order

        assertEquals(Ordered.LOWEST_PRECEDENCE, order, "order must be lowest precedence")
    }
}