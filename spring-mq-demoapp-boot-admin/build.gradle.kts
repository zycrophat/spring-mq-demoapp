import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.spring")
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

val winsw = configurations.create("winsw")
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
    winsw("com.sun.winsw:winsw:2.2.0:bin@exe")
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

    val copyConfig by registering(Copy::class) {
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

    val distZip by registering(Zip::class) {
        description = "Creates a distributable zip file for the project"
        group = ProjectSettings.DISTRIBUTION_GROUP_NAME

        with(distCopySpec)
    }

    val distTar by registering(Tar::class) {
        description = "Creates a distributable tgz file for the project"
        group = ProjectSettings.DISTRIBUTION_GROUP_NAME

        with(distCopySpec)
        compression = Compression.GZIP
    }

    val distAll by registering {
        description = "Creates distributable archive files for the project"
        group = ProjectSettings.DISTRIBUTION_GROUP_NAME

        dependsOn(distZip, distTar)
    }

    val createWindowsService by registering {
        description = "Creates installer to deploy the application as a Windows service"
        group = ProjectSettings.DISTRIBUTION_GROUP_NAME

        dependsOn(bootJar)
        val windowsServiceDir = file("${project.buildDir}/windows-service")
        outputs.dir(windowsServiceDir)

        doLast {
            windowsServiceDir.mkdirs()

            copy {
                from(bootJar)
                into("$windowsServiceDir/lib")
            }
            copy {
                from("config")
                into("$windowsServiceDir/config")
            }
            copy {
                from(winsw)
                into(windowsServiceDir)
                rename("winsw-2.2.0-bin.exe", "${project.name}-${project.version}.exe")
            }
            val winswConfig = createWinswConfig(project, bootJar.get().archiveFile.orNull?.asFile?.name, 9011)
            file("$windowsServiceDir/${project.name}-${project.version}.xml")
                    .writeText(winswConfig.toString(PrintOptions(singleLineTextElements = true)))
        }
    }

}
