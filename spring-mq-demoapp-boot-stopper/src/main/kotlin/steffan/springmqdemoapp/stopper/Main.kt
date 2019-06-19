package steffan.springmqdemoapp.stopper

import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import kotlin.system.exitProcess


object Main : Logging {
    @JvmStatic
    fun main(args: Array<String>) {
        fun jmxConnector() = if (args.isNotEmpty()) { connect(port = args[0].toInt()) } else { connect() }
        try {
            jmxConnector().use { connector ->
                SpringBootApplicationStopper(connector = connector).stop()
            }
        } catch (e: Exception) {
            logger().error("Shutdown failed", e)
            exitProcess(1)
        }
    }
}


