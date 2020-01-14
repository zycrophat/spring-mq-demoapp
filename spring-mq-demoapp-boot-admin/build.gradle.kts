import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.spring")
    id("idea")
    application
    distribution
}

project.evaluationDependsOn(":spring-mq-demoapp-boot-stopper")
project.evaluationDependsOn(":spring-mq-demoapp-boot-passwordhelper")
project.evaluationDependsOn(":spring-mq-demoapp-boot-encryptstringhelper")

group = "steffan"
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11


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
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3+")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("org.springframework.boot:spring-boot-starter-web:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("de.codecentric:spring-boot-admin-starter-server:${LibraryVersions.SPRING_BOOT_ADMIN_VERSION}")
    implementation("de.codecentric:spring-boot-admin-starter-client:${LibraryVersions.SPRING_BOOT_ADMIN_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-security:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation(kotlin("reflect:${LibraryVersions.KOTLIN_VERSION}"))
    implementation(kotlin("stdlib-jdk8:${LibraryVersions.KOTLIN_VERSION}"))

    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:${LibraryVersions.JASYPT_SPRING_BOOT_VERSION}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${LibraryVersions.SPRING_BOOT_VERSION}")
    winsw("com.sun.winsw:winsw:${LibraryVersions.WINSW_VERSION}")
}

dependencyManagement {
    imports {
        mavenBom("de.codecentric:spring-boot-admin-dependencies:${LibraryVersions.SPRING_BOOT_ADMIN_VERSION}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

val jmxPort = 9011
tasks {

    bootRun {
        workingDir = project.projectDir

        jvmArgs(getBootRunJvmArgs(jmxPort))
    }

    bootJar {
        launchScript()
    }

}

val createWindowsServiceConfig by tasks.registering {
    description = "Creates xml config for deploying the application as a Windows service via winsw"
    group = ProjectSettings.DISTRIBUTION_GROUP_NAME

    val windowsServiceDir = file("${project.buildDir}/windows-service")
    outputs.dir(windowsServiceDir)

    doLast {
        windowsServiceDir.mkdirs()

        val bootScriptFileName =
                fileTree(files(tasks.named("bootStartScripts")).singleFile) {
                    include("**/*.bat")
                }.singleFile.name

        val winswConfig = createWinswConfig(project, "bin/$bootScriptFileName", jmxPort)
        file("$windowsServiceDir/${project.name}-${project.version}.xml")
                .writeText(winswConfig)

        val installScript = createInstallScript(project)
        file("$windowsServiceDir/${project.name}-${project.version}-install.bat")
                .writeText(installScript)

        val uninstallScript = createUninstallScript(project)
        file("$windowsServiceDir/${project.name}-${project.version}-uninstall.bat")
                .writeText(uninstallScript)
    }
}

val passwordHelperInstallDistTask = project(":spring-mq-demoapp-boot-passwordhelper").tasks.named("installDist")
val encryptstringHelperInstallDistTask = project(":spring-mq-demoapp-boot-encryptstringhelper").tasks.named("installDist")
val distCopySpec = project.copySpec {
    from("config") {
        into("config")
    }
    from(file("${project.rootDir}/LICENSE"))
    from(file("${project.rootDir}/README.md"))
    from(passwordHelperInstallDistTask) {
        into("utils/passwordhelper")
    }
    from(encryptstringHelperInstallDistTask) {
        into("utils/encryptstringhelper")
    }
}

application {
    mainClassName = "steffan.springmqdemoapp.admin.MainKt"
    applicationDefaultJvmArgs = getBootRunJvmArgs(jmxPort)
}

val stopperInstallDistTask = project(":spring-mq-demoapp-boot-stopper").tasks.named("installDist")
distributions {
    boot {
        contents {
            with(distCopySpec)
        }
    }

    create("bootWinService") {
        contents {
            with(boot.get().contents)

            from(winsw)
            rename(winsw.resolve().single().name, "${project.name}-${project.version}.exe")

            from(createWindowsServiceConfig)

            from(stopperInstallDistTask) {
                into("stopper")
            }
        }
    }
}
