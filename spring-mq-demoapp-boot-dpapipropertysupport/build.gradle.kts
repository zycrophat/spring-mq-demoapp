import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("idea")
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11


repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    api("org.slf4j:slf4j-api:1.7.26+")

    implementation(kotlin("reflect:${LibraryVersions.KOTLIN_VERSION}"))
    implementation(kotlin("stdlib-jdk8:${LibraryVersions.KOTLIN_VERSION}"))

    implementation(project(":spring-mq-demoapp-boot-common"))

    implementation("org.springframework.boot:spring-boot:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("com.github.peter-gergely-horvath:windpapi4j:1.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}
