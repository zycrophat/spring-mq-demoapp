import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version LibraryVersions.SPRING_BOOT_VERSION
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    kotlin("jvm") version "1.3.31"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.31"
    id("idea")
    id("com.avast.gradle.docker-compose") version "0.9.4"
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
dependencies {
    implementation("org.slf4j:slf4j-api:1.7.26+")
    runtime("ch.qos.logback:logback-classic:1.2.3+")

    implementation("org.springframework.boot:spring-boot-starter-activemq:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-integration:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("org.springframework.boot:spring-boot-starter-web:${LibraryVersions.SPRING_BOOT_VERSION}")
    implementation("de.codecentric:spring-boot-admin-starter-server:${LibraryVersions.SPRING_BOOT_BASE_VERSION}")
    implementation("de.codecentric:spring-boot-admin-starter-client:${LibraryVersions.SPRING_BOOT_BASE_VERSION}")
    implementation("org.springframework.boot:spring-boot-starter-security:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("org.jetbrains.kotlin:kotlin-reflect:${LibraryVersions.KOTLIN_VERSION}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${LibraryVersions.KOTLIN_VERSION}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${LibraryVersions.SPRING_BOOT_VERSION}")

    implementation("javax.xml.bind:jaxb-api:2.3.1")
    
    implementation("org.glassfish.jaxb:jaxb-runtime:${LibraryVersions.JAXB_VERSION}")
    implementation("org.springframework:spring-oxm:5.1.7.RELEASE")
    implementation("org.reflections:reflections:0.9.11")
    implementation("org.springframework.boot:spring-boot-starter-jta-atomikos:${LibraryVersions.SPRING_BOOT_VERSION}")


    implementation("org.apache.camel:camel-spring-boot:${LibraryVersions.CAMEL_VERSION}")
    implementation("org.apache.camel:camel-spring-boot-starter:${LibraryVersions.CAMEL_VERSION}")
    runtime("org.apache.activemq:activemq-camel:5.15.9")
    runtime("org.apache.camel:camel-jms:${LibraryVersions.CAMEL_VERSION}")
    implementation("org.apache.camel:camel-jaxb:${LibraryVersions.CAMEL_VERSION}")
    runtime("com.fasterxml.woodstox:woodstox-core:5.2.1")
    implementation("org.apache.camel:camel-sql:${LibraryVersions.CAMEL_VERSION}")

    implementation("org.apache.camel:camel-infinispan:${LibraryVersions.CAMEL_VERSION}")
    implementation("org.infinispan:infinispan-spring-boot-starter:2.1.5.Final")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-mock:2.0.8")
    implementation("org.infinispan:infinispan-cachestore-jdbc:9.4.14.Final")
    runtime("com.h2database:h2:1.4.199")
    runtime("org.springframework.boot:spring-boot-starter-jdbc:${LibraryVersions.SPRING_BOOT_VERSION}")

    jaxb("org.glassfish.jaxb:jaxb-xjc:${LibraryVersions.JAXB_VERSION}")
    jaxb("org.glassfish.jaxb:jaxb-runtime:${LibraryVersions.JAXB_VERSION}")
    jaxb("javax.activation:activation:1.1")
    implementation("com.migesok:jaxb-java-time-adapters:1.1.3")
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
                "xjc"("destdir" to "$jaxbTargetDir",
                        "binding" to "${xsdDir}/binding.xml"
                ) {
                    "schema"("dir" to "$xsdDir", "includes" to "*.xsd")
                    "arg"("value" to "-extension")
                    "produces"("dir" to jaxbTargetDir, "includes" to "**/*.java")
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
