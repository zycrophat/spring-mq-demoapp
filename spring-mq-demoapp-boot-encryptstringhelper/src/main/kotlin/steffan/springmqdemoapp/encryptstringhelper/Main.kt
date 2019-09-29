package steffan.springmqdemoapp.encryptstringhelper

import steffan.springmqdemoapp.util.ConsoleUtil.readPassword
import steffan.springmqdemoapp.util.Logging
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig
import org.jasypt.iv.RandomIvGenerator


object Main : Logging {

    private const val ALGORITHM = "PBEWITHHMACSHA512ANDAES_256"

    @JvmStatic
    fun main(args: Array<String>) {
        val password = readPassword("Enter password: ")
        val clearText = readPassword("Enter value to encrypt: ")

        val config = EnvironmentStringPBEConfig().apply {
            algorithm = ALGORITHM
            ivGenerator = RandomIvGenerator()
            passwordCharArray = password
        }

        val encryptor = StandardPBEStringEncryptor()
        encryptor.setConfig(config)

        val cipherText = encryptor.encrypt(String(clearText))
        println(cipherText)
    }
}




