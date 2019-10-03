package steffan.springmqdemoapp.encryptstringhelper

import com.google.common.base.CaseFormat
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig
import org.jasypt.encryption.pbe.config.PBEConfig
import org.jasypt.iv.RandomIvGenerator
import steffan.springmqdemoapp.encryptstringhelper.Main.PropertyType.ENVIRONMENT_VARIABLE
import steffan.springmqdemoapp.encryptstringhelper.Main.PropertyType.JVM_SYSTEM_PROPERTY
import steffan.springmqdemoapp.util.ConsoleUtil.readPassword
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.util.*
import java.util.regex.Matcher
import kotlin.reflect.KCallable


object Main : Logging {

    private const val DEFAULT_ALGORITHM = "PBEWITHHMACSHA512ANDAES_256"
    private const val ENVIRONMENT_PROPERTY_PREFIX = "ENC"
    private const val JVM_SYSTEM_PROPERTY_PREFIX = "enc"

    private val propertyNamePattern = """set(.*)(EnvName|SysPropertyName)""".toPattern()

    @JvmStatic
    fun main(args: Array<String>) {
        val config = createConfig()

        val clearText = readPassword("Enter value to encrypt: ")

        val encryptor = StandardPBEStringEncryptor()
        encryptor.setConfig(config)

        val cipherText = encryptor.encrypt(String(clearText))
        println(cipherText)
    }

    private fun createConfig(): PBEConfig {
        val config = EnvironmentStringPBEConfig()
        prepareEnvAndSysPropertySettings(config)

        config.apply {
            algorithm = when (algorithm) {
                null -> DEFAULT_ALGORITHM
                else -> algorithm
            }
            ivGenerator = when (ivGenerator) {
                null -> RandomIvGenerator()
                else -> ivGenerator
            }
        }
        if (passwordHasNotBeenSetViaEnvOrSysProperties(config)) {
            config.passwordCharArray = readPassword("Enter password: ")
        }
        return config
    }

    private fun passwordHasNotBeenSetViaEnvOrSysProperties(config: EnvironmentStringPBEConfig) =
            config.passwordSysPropertyName == null && config.passwordEnvName == null

    private fun prepareEnvAndSysPropertySettings(config: EnvironmentStringPBEConfig) {
        config.javaClass.kotlin.members
            .stream()
            .map { Pair(it, propertyNamePattern.matcher(it.name)) }
            .filter { it.second.matches() }
            .sorted(compareByPropertyNameThenByPropertyType)
            .forEach { pair ->
                val propertyName = pair.second.group(1)
                val propertyTypeDiscriminator = pair.second.group(2)
                val propertyType = propertyTypeDiscriminator.let {
                    when {
                        it.endsWith("EnvName") -> ENVIRONMENT_VARIABLE
                        it.endsWith("SysPropertyName") -> JVM_SYSTEM_PROPERTY
                        else -> throw RuntimeException("Cannot determine property propertyType")
                    }
                }

                val propertyConfigName = when(propertyType) {
                    ENVIRONMENT_VARIABLE ->
                        "${ENVIRONMENT_PROPERTY_PREFIX}_${CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE).convert(propertyName)}"
                    JVM_SYSTEM_PROPERTY ->
                        "$JVM_SYSTEM_PROPERTY_PREFIX${propertyName}"
                }

                when(propertyType) {
                    ENVIRONMENT_VARIABLE -> System.getenv(propertyConfigName)
                    JVM_SYSTEM_PROPERTY -> System.getProperty(propertyConfigName)
                }?.let {
                    logger().debug("Configuring $propertyName using $propertyConfigName ${propertyType.toHumanReadable()}")
                    pair.first.call(config, propertyConfigName)
                }

            }
    }

    private val compareByPropertyNameThenByPropertyType: Comparator<Pair<KCallable<*>, Matcher>>? =
        compareBy<Pair<KCallable<*>, Matcher>> { it.second.group(1) }
        .thenComparing(
                { it.second.group(2) },
                { o1, o2 ->
                    when {
                        "EnvName" == o1 -> 1
                        "SysPropertyName" == o2 -> -1
                        else -> 0
                    }
                }
        )

    private enum class PropertyType {
        ENVIRONMENT_VARIABLE {
            override fun toHumanReadable() = "environment variable"
        },
        JVM_SYSTEM_PROPERTY {
            override fun toHumanReadable() = "JVM system property"
        };

        abstract fun toHumanReadable(): String
    }

}
