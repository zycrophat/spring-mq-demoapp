package steffan.springmqdemoapp.passwordhelper

import org.springframework.security.crypto.factory.PasswordEncoderFactories
import steffan.springmqdemoapp.util.ConsoleUtil.readPassword
import steffan.springmqdemoapp.util.Logging


object Main : Logging {
    @JvmStatic
    fun main(args: Array<String>) {
        val password = readPassword("Enter password to encode: ")
        val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
        val encodedPassword = passwordEncoder.encode(String(password))
        println(encodedPassword)
    }
}




