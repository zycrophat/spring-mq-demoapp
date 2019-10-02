import org.gradle.api.Project
import org.stringtemplate.v4.STGroupFile


fun createWinswConfig(theProject: Project, executable: String, jmxPort: Int): String {
    val stg = STGroupFile("${theProject.rootDir}/templates/winsw/winswconfig.stg")
    val winswConfigTemplate = stg.getInstanceOf("winswconfig").apply {
        add("project", theProject)
        add("executable", executable)
        add("jmxPort", jmxPort)
    }
    return winswConfigTemplate.render()
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

fun createInstallScript(theProject: Project): String {
    val stg = STGroupFile("${theProject.rootDir}/templates/winsw/winswconfig.stg")
    val winswConfigTemplate = stg.getInstanceOf("installBat").apply {
        add("project", theProject)
    }
    return winswConfigTemplate.render()
}

fun createUninstallScript(theProject: Project): String {
    val stg = STGroupFile("${theProject.rootDir}/templates/winsw/winswconfig.stg")
    val winswConfigTemplate = stg.getInstanceOf("uninstallBat").apply {
        add("project", theProject)
    }
    return winswConfigTemplate.render()
}