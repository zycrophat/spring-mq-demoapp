import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm")
    application
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11


dependencies {
    implementation(kotlin("reflect:${LibraryVersions.KOTLIN_VERSION}"))
    implementation(kotlin("stdlib-jdk8:${LibraryVersions.KOTLIN_VERSION}"))
    implementation("com.google.guava:guava:28.1-jre")
    implementation(project(":spring-mq-demoapp-boot-common"))
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3+")

    implementation("org.jasypt:jasypt:1.9.3")

    // Use the Kotlin test library.
    testImplementation(kotlin("test"))

    // Use the Kotlin JUnit integration.
    testImplementation(kotlin("test-junit"))
}

val distCopySpec = copySpec {
    from("config") {
        into("config")
    }
    from("README.md")
}
distributions {
    main {
        contents {
            with(distCopySpec)
        }
    }
}

application {
    mainClassName = "steffan.springmqdemoapp.encryptstringhelper.Main"
    applicationDefaultJvmArgs = listOf(
            "-Dlogback.configurationFile=X_APP_HOME/config/logback.xml"
    )
}

tasks.withType<CreateStartScripts>{
    doLast {
        val unixScriptText = unixScript.readText()
        unixScript.writeText(unixScriptText.replace("X_APP_HOME", "\$APP_HOME"))

        val windowsScriptText = windowsScript.readText()
        windowsScript.writeText(windowsScriptText.replace("X_APP_HOME", "%~dp0.."))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}
