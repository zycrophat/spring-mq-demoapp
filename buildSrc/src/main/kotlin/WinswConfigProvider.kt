import org.gradle.api.Project
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.xml

fun createWinswConfig(theProject: Project, bootJarName: String?, jmxPort: Int): Node {
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
            -"java"
        }
        "startargument" {
            -"-Dcom.sun.management.jmxremote"
        }
        "startargument" {
            -"-Dcom.sun.management.jmxremote.host=localhost"
        }
        "startargument" {
            -"-Dcom.sun.management.jmxremote.port=$jmxPort"
        }
        "startargument" {
            -"-Dcom.sun.management.jmxremote.local.only=true"
        }
        "startargument" {
            -"-Dcom.sun.management.jmxremote.authenticate=false"
        }
        "startargument" {
            -"-Dcom.sun.management.jmxremote.ssl=false"
        }
        "startargument" {
            -"-Dcom.sun.management.jmxremote.rmi.port=$jmxPort"
        }
        "startargument" {
            -"-Djava.rmi.server.hostname=localhost"
        }
        "startargument" {
            -"-jar"
        }
        "startargument" {
            -"%BASE%/lib/$bootJarName"
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