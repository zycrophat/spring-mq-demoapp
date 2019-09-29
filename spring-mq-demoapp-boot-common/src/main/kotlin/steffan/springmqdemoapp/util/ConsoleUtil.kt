package steffan.springmqdemoapp.util

import java.io.BufferedReader
import java.io.InputStreamReader


object ConsoleUtil : Logging {

    fun readPassword(prompt: String): CharArray = when {
        System.console() != null -> {
            System.console().readPassword(prompt)
        }
        else -> {
            logger().warn("Application is not connected to a console. Entered password may get printed in clear!")
            print(prompt)
            val reader = BufferedReader(InputStreamReader(System.`in`))
            reader.readLine().toCharArray()
        }
    }
}
