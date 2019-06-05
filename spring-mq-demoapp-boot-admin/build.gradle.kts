import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version LibraryVersions.SPRING_BOOT_VERSION
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.31"
    id("idea")
}

group = "steffan"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8


repositories {
    jcenter()
    mavenCentral()
}

springBoot {
    buildInfo()
}

configurations.all {
    resolutionStrategy {
        dependencySubstitution {
            substitute(module("com.fasterxml.jackson.core:jackson-core"))
                    .with(module("com.fasterxml.jackson.core:jackson-core:[2.9.9,)"))
            substitute(module("com.fasterxml.jackson.core:jackson-databind"))
                    .with(module("com.fasterxml.jackson.core:jackson-databind:[2.9.9,)"))

            substitute(module("com.google.guava:guava"))
                    .with(module("com.google.guava:guava:[27.1-jre,)"))
        }
    }

    exclude("org.jboss.slf4j", "slf4j-jboss-logging")
}

dependencies {
    implementation(project(":spring-mq-demoapp-boot-common"))
    runtime("ch.qos.logback:logback-classic:1.2.3+")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("org.springframework.boot:spring-boot-starter-web:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("de.codecentric:spring-boot-admin-starter-server:${LibraryVersions.SPRING_BOOT_BASE_VERSION}")
    implementation("de.codecentric:spring-boot-admin-starter-client:${LibraryVersions.SPRING_BOOT_BASE_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-security:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("org.jetbrains.kotlin:kotlin-reflect:${LibraryVersions.KOTLIN_VERSION}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${LibraryVersions.KOTLIN_VERSION}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${LibraryVersions.SPRING_BOOT_VERSION}")
}

dependencyManagement {
    imports {
        mavenBom("de.codecentric:spring-boot-admin-dependencies:${LibraryVersions.SPRING_BOOT_BASE_VERSION}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks {

    bootRun {
        workingDir = project.projectDir
    }

    val copyConfig by creating(Copy::class) {
        from("config")
        into("$buildDir/libs/config")
    }

    bootJar {
        launchScript()

        dependsOn(copyConfig)
    }

    val distCopySpec = project.copySpec {
        from(copyConfig) {
            into("config")
        }
        from(file("${project.rootDir}/LICENSE"))
        from(bootJar)
    }

    val distZip by creating(Zip::class) {
        description = "Creates a distributable zip file for the project"
        group = ProjectSettings.DISTRIBUTION_GROUP_NAME

        with(distCopySpec)
    }

    val distTar by creating(Tar::class) {
        description = "Creates a distributable tgz file for the project"
        group = ProjectSettings.DISTRIBUTION_GROUP_NAME

        with(distCopySpec)
        compression = Compression.GZIP
    }

    val distAll by creating {
        description = "Creates distributable archive files for the project"
        group = ProjectSettings.DISTRIBUTION_GROUP_NAME

        dependsOn(distZip, distTar)
    }

}
