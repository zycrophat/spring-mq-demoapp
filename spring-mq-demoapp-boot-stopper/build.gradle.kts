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
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3+")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClassName = "steffan.springmqdemoapp.stopper.Main"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}
