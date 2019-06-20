import org.gradle.api.Project
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.xml

fun createWinswConfig(theProject: Project, executable: String, jmxPort: Int): Node {
    return xml("configuration") {
        "id" {
            -"${theProject.name}-${theProject.version}"
        }
        "name" {
            -"${theProject.name}-${theProject.version}"
        }
        "description" {
            -"Spring Boot Sample application ${theProject.name}-${theProject.version}"
        }
        "executable" {
            -"$executable"
        }
        "priority" {
            -"Normal"
        }
        "stoptimeout" {
            -"15 sec"
        }
        "stopparentprocessfirst" {
            -"false"
        }
        "startmode" {
            -"Automatic"
        }
        "waithint" {
            -"15 sec"
        }
        "sleeptime" {
            -"1 sec"
        }
        "log" {
            attribute("mode", "append")
        }
        "stopexecutable" {
            -"%BASE%/stopper/bin/spring-mq-demoapp-boot-stopper.bat"
        }
        "stopargument" {
            -"$jmxPort"
        }
    }
}

fun getBootRunJvmArgs(jmxPort: Int) = listOf(
        "-Dcom.sun.management.jmxremote",
        "-Dcom.sun.management.jmxremote.host=localhost",
        "-Dcom.sun.management.jmxremote.port=$jmxPort",
        "-Dcom.sun.management.jmxremote.local.only=true",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-Dcom.sun.management.jmxremote.rmi.port=$jmxPort",
        "-Djava.rmi.server.hostname=localhost"
)