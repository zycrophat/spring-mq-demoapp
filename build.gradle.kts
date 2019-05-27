import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.unbrokendome.gradle.plugins.gitversion.model.HasObjectId

plugins {
    id("org.springframework.boot") version "2.1.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    kotlin("jvm") version "1.3.31"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.31"
    id("idea")
    id("com.avast.gradle.docker-compose") version "0.9.4"
    id ("org.unbroken-dome.gitversion") version "0.10.0"
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

gitVersion.rules {
    val versionPattern = """v?(\d+)\.(\d+)\.(\d+)""".toPattern()

    before {
        val tag = findLatestTag(versionPattern)
        version.major = tag?.matches?.getAt(1)?.toInt() ?: 0
        version.minor = tag?.matches?.getAt(2)?.toInt() ?: 0
    }

    onBranch("master") {
        val tag = findLatestTag(versionPattern)
        version.patch = countCommitsSince(tag as HasObjectId)

        isSkipOtherRules = true
    }

    always {
        version.patch = countCommitsSince(branchPoint() as HasObjectId)
        version.setPrereleaseTag("$branchName-SNAPSHOT")
    }
}

version = gitVersion.determineVersion()

val generatedJavaSrcPath = ProjectSettings.GENERATED_JAVA_SRC_PATH

sourceSets {
    getByName("main").java.srcDirs(generatedJavaSrcPath)
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
    runtime("org.springframework.boot:spring-boot-starter-jta-atomikos:${LibraryVersions.SPRING_BOOT_VERSION}")


    implementation("org.apache.camel:camel-spring-boot:${LibraryVersions.CAMEL_VERSION}")
    implementation("org.apache.camel:camel-spring-boot-starter:${LibraryVersions.CAMEL_VERSION}")
    runtime("org.apache.activemq:activemq-camel:5.15.9")
    runtime("org.apache.camel:camel-jms:${LibraryVersions.CAMEL_VERSION}")
    implementation("org.apache.camel:camel-jaxb:${LibraryVersions.CAMEL_VERSION}")
    runtime("com.fasterxml.woodstox:woodstox-core:5.2.1")
    implementation("org.apache.camel:camel-sql:${LibraryVersions.CAMEL_VERSION}")
    runtime("com.h2database:h2:1.4.199")
    runtime("org.springframework.boot:spring-boot-starter-jdbc:${LibraryVersions.SPRING_BOOT_VERSION}")

    jaxb("org.glassfish.jaxb:jaxb-xjc:${LibraryVersions.JAXB_VERSION}")
    jaxb("org.glassfish.jaxb:jaxb-runtime:${LibraryVersions.JAXB_VERSION}")
    jaxb("javax.activation:activation:1.1")
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

    val printVersion by creating {
        doLast {
            println(gitVersion.determineVersion())
        }
    }

}


