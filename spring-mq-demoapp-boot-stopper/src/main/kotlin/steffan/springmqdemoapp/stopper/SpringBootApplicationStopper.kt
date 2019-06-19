package steffan.springmqdemoapp.stopper

import steffan.springmqdemoapp.util.Logging
import steffan.springmqdemoapp.util.logger
import javax.management.ObjectName
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

internal const val DEFAULT_JMX_HOST = "localhost"
internal const val DEFAULT_JMX_PORT = 9010
internal const val DEFAULT_ADMIN_MBEAN_NAME = "org.springframework.boot:type=Admin,name=SpringApplication"

open class SpringBootApplicationStopper(private val connector: JMXConnector,
                                        private val mBeanName: String = DEFAULT_ADMIN_MBEAN_NAME): Logging {

    fun stop() {
        logger().info("Stopping application")
        val mBeanServerConnection = connector.mBeanServerConnection
        mBeanServerConnection.invoke(
                ObjectName(mBeanName), "shutdown", null, null
        )
        logger().info("Application stopped")
    }
}

fun connect(host: String = DEFAULT_JMX_HOST, port: Int = DEFAULT_JMX_PORT): JMXConnector {
    val url = "service:jmx:rmi:///jndi/rmi://$host:$port/jmxrmi"
    val serviceUrl = JMXServiceURL(url)
    return JMXConnectorFactory.connect(serviceUrl, null)
}
