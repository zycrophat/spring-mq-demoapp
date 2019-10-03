import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm")
    application
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${LibraryVersions.KOTLIN_VERSION}")
    implementation(project(":spring-mq-demoapp-boot-common"))
    runtime("ch.qos.logback:logback-classic:1.2.3+")

    implementation("org.springframework.security:spring-security-crypto:5.1.6.RELEASE")
    runtime("commons-logging:commons-logging:1.2+")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

val distCopySpec = copySpec {
    from("config") {
        into("config")
    }
}
distributions {
    main {
        contents {
            with(distCopySpec)
        }
    }
}

application {
    mainClassName = "steffan.springmqdemoapp.passwordhelper.Main"
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