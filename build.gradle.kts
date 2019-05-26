import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.1.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    kotlin("jvm") version "1.3.31"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.31"
    id("idea")
    id("com.avast.gradle.docker-compose") version "0.9.4"
}

group = "steffan"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

repositories {
    jcenter()
    mavenCentral()
}

springBoot {
    buildInfo()
}

extra["springBootAdminVersion"] = "2.1.5"

val generatedJavaSrcPath = ProjectSettings.GENERATED_JAVA_SRC_PATH

sourceSets {
    getByName("main").java.srcDirs(generatedJavaSrcPath)
}

val jaxb = configurations.create("jaxb")
dependencies {
    implementation("org.slf4j:slf4j-api:1.7.26")
    runtime("ch.qos.logback:logback-classic:1.2.3")

    implementation("org.springframework.boot:spring-boot-starter-activemq")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-integration")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("de.codecentric:spring-boot-admin-starter-server")
    implementation("de.codecentric:spring-boot-admin-starter-client")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.30+")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.30+")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")
    implementation("org.springframework:spring-oxm")
    implementation("org.reflections:reflections:0.9.11")
    runtime("org.springframework.boot:spring-boot-starter-jta-atomikos")

    implementation("org.apache.camel:camel-spring-boot:2.24.0")
    implementation("org.apache.camel:camel-spring-boot-starter:2.24.0")
    runtime("org.apache.activemq:activemq-camel:5.15.9")
    runtime("org.apache.camel:camel-jms:2.24.0")
    implementation("org.apache.camel:camel-jaxb:2.24.0")
    runtime("com.fasterxml.woodstox:woodstox-core:5.2.1")

    jaxb("org.glassfish.jaxb:jaxb-xjc:2.3.2")
    jaxb("org.glassfish.jaxb:jaxb-runtime:2.3.2")
    jaxb("javax.activation:activation:1.1")
}

dependencyManagement {
    imports {
        mavenBom("de.codecentric:spring-boot-admin-dependencies:${property("springBootAdminVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

dockerCompose {
    useComposeFiles = listOf("docker-fixtures/docker-compose.yml")
    waitForTcpPorts = true
    stopContainers = false
}

tasks {

    bootRun {
        dependsOn(composeUp)
        workingDir = project.projectDir
    }
    test {
        dependsOn(composeUp)
    }

    val generateJaxb by creating {
        description = "Converts xsds to classes"
        val jaxbTargetDir = file(generatedJavaSrcPath)
        val xsdDir = file("src/main/resources/steffan/springmqdemoapp/api/xsd")

        inputs.dir(xsdDir)
        outputs.dir(jaxbTargetDir)

        doLast {
            jaxbTargetDir.mkdirs()

            ant.withGroovyBuilder {
                "taskdef"("name" to "xjc", "classname" to "com.sun.tools.xjc.XJC2Task", "classpath" to jaxb.asPath)
                "xjc"("destdir" to "$jaxbTargetDir", "package" to "steffan.springmqdemoapp.api.bindings") {
                    "schema"("dir" to "$xsdDir", "includes" to "*.xsd")
                }
            }
        }
    }

    val cleanGeneratedSources by creating(Delete::class) {
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

    val copyConfig by creating(Copy::class) {
        from("config")
        into("$buildDir/libs/config")

        dependsOn(bootJar)
    }

    val distCopySpec = project.copySpec {
        from(copyConfig) {
            into("config")
        }
        from(bootJar)
    }

    val distZip by creating(Zip::class) {
        with(distCopySpec)
    }

    val distTar by creating(Tar::class) {
        with(distCopySpec)
        compression = Compression.GZIP
    }

    val distAll by creating {
        dependsOn(distZip, distTar)
    }

}


