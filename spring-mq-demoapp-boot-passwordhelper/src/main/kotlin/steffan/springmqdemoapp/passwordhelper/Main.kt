package steffan.springmqdemoapp.passwordhelper

import org.springframework.security.crypto.factory.PasswordEncoderFactories
import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import java.io.BufferedReader
import java.io.InputStreamReader


object Main : Logging {
    @JvmStatic
    fun main(args: Array<String>) {
        val password = readPassword("Enter password to encode: ")
        val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
        val encodedPassword = passwordEncoder.encode(String(password))
        println(encodedPassword)
    }

    private val readPassword: (String) -> CharArray = {
        when {
            System.console() != null -> {
                System.console().readPassword(it)
            }
            else -> {
                logger().warn("Application is not connected to a console. Entered password may get printed in clear!")
                print(it)
                val reader = BufferedReader(InputStreamReader(System.`in`))
                reader.readLine().toCharArray()
            }
        }
    }
}




