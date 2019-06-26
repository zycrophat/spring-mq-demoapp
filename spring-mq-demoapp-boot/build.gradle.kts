import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.redundent.kotlin.xml.PrintOptions

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.spring")
    id("idea")
    id("com.avast.gradle.docker-compose") version "0.9.4"
    application
    distribution
}

project.evaluationDependsOn(":spring-mq-demoapp-boot-stopper")

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

val generatedJavaSrcPath = ProjectSettings.GENERATED_JAVA_SRC_PATH

sourceSets {
    getByName("main").java.srcDirs(generatedJavaSrcPath)
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

val jaxb = configurations.create("jaxb")
val winsw = configurations.create("winsw")
dependencies {
    implementation(project(":spring-mq-demoapp-boot-common"))
    runtime("ch.qos.logback:logback-classic:1.2.3+")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("org.springframework.boot:spring-boot-starter-activemq:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-integration:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("org.springframework.boot:spring-boot-starter-web:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("de.codecentric:spring-boot-admin-starter-client:${LibraryVersions.SPRING_BOOT_BASE_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-security:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("org.jetbrains.kotlin:kotlin-reflect:${LibraryVersions.KOTLIN_VERSION}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${LibraryVersions.KOTLIN_VERSION}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("javax.xml.bind:jaxb-api:2.3.1")
    
    implementation("org.glassfish.jaxb:jaxb-runtime:${LibraryVersions.JAXB_VERSION}")

    implementation("org.springframework:spring-oxm:${LibraryVersions.SPRING_FRAMEWORK_VERSION}")
    implementation("org.reflections:reflections:0.9.11")
    implementation("org.springframework.boot:spring-boot-starter-jta-atomikos:${LibraryVersions.SPRING_BOOT_VERSION}")


    implementation("org.apache.camel:camel-spring-boot:${LibraryVersions.CAMEL_VERSION}")
    implementation("org.apache.camel:camel-spring-boot-starter:${LibraryVersions.CAMEL_VERSION}")
    runtime("org.apache.activemq:activemq-camel:5.15.9")
    implementation("org.apache.camel:camel-jms:${LibraryVersions.CAMEL_VERSION}")
    implementation("org.apache.camel:camel-jaxb:${LibraryVersions.CAMEL_VERSION}")
    runtime("com.fasterxml.woodstox:woodstox-core:5.2.1")
    implementation("org.apache.camel:camel-sql:${LibraryVersions.CAMEL_VERSION}")


    implementation("org.apache.camel:camel-infinispan:${LibraryVersions.CAMEL_VERSION}")
    implementation("org.infinispan:infinispan-spring-boot-starter:2.1.5.Final")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-mock:2.0.8")
    implementation("org.infinispan:infinispan-cachestore-jdbc:9.4.14.Final")
    implementation("com.h2database:h2:1.4.199")
    runtime("org.springframework.boot:spring-boot-starter-jdbc:${LibraryVersions.SPRING_BOOT_VERSION}")

    jaxb("org.glassfish.jaxb:jaxb-xjc:${LibraryVersions.JAXB_VERSION}")
    jaxb("org.glassfish.jaxb:jaxb-runtime:${LibraryVersions.JAXB_VERSION}")
    jaxb("javax.activation:activation:1.1")
    implementation("com.migesok:jaxb-java-time-adapters:1.1.3")

    runtime("org.springframework.boot:spring-boot-starter-aop:${LibraryVersions.SPRING_BOOT_VERSION}")
    runtime("org.springframework:spring-aop:${LibraryVersions.SPRING_FRAMEWORK_VERSION}")
    runtime("org.springframework:spring-aspects:${LibraryVersions.SPRING_FRAMEWORK_VERSION}")

    winsw("com.sun.winsw:winsw:${LibraryVersions.WINSW_VERSION}")
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

dockerCompose {
    useComposeFiles = listOf("docker-fixtures/docker-compose.yml")
    waitForTcpPorts = true
    stopContainers = false
}

val jmxPort = 9012
tasks {

    bootRun {
        dependsOn(composeUp)
        workingDir = project.projectDir

        jvmArgs(getBootRunJvmArgs(jmxPort))
    }
    test {
        dependsOn(composeUp)
    }

    val generateJaxb by registering {
        description = "Converts xsds to classes"
        val jaxbTargetDir = file(generatedJavaSrcPath)
        val xsdDir = file("src/main/resources/steffan/springmqdemoapp/api/xsd")

        inputs.dir(xsdDir)
        outputs.dir(jaxbTargetDir)

        doLast {
            jaxbTargetDir.mkdirs()

            ant.withGroovyBuilder {
                "taskdef"("name" to "xjc", "classname" to "com.sun.tools.xjc.XJC2Task", "classpath" to jaxb.asPath)
                "xjc"("destdir" to "$jaxbTargetDir",
                        "binding" to "$xsdDir/binding.xml"
                ) {
                    "schema"("dir" to "$xsdDir", "includes" to "*.xsd")
                    "arg"("value" to "-extension")
                    "produces"("dir" to jaxbTargetDir, "includes" to "**/*.java")
                }
            }
        }
    }

    val cleanGeneratedSources by registering(Delete::class) {
        delete(file(generatedJavaSrcPath))
    }

    clean {
        dependsOn(cleanGeneratedSources)
    }

    compileJava {
        dependsOn(generateJaxb)
    }

    compileKotlin {
        dependsOn(generateJaxb)
    }

    val idea by getting {
        dependsOn(generateJaxb)
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
                .writeText(winswConfig.toString(PrintOptions(singleLineTextElements = true)))
    }
}

val distCopySpec = project.copySpec {
    from("config") {
        into("config")
    }
    from(file("${project.rootDir}/LICENSE"))
}

application {
    mainClassName = "steffan.springmqdemoapp.MainKt"
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
