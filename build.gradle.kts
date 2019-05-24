import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.1.5.RELEASE"
	id("io.spring.dependency-management") version "1.0.7.RELEASE"
	kotlin("jvm") version "1.3.31"
	id ("org.jetbrains.kotlin.plugin.spring") version "1.3.31"
	id("idea")
	id("com.avast.gradle.docker-compose") version "0.9.4"
}

group = "steffan"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_1_8

repositories {
	mavenCentral()
}

extra["springBootAdminVersion"] = "2.1.5"

dependencies {
	api("org.slf4j:slf4j-api:1.7.26")
	runtime("ch.qos.logback:logback-classic:1.2.3")

	implementation("org.springframework.boot:spring-boot-starter-activemq")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-integration")
	implementation("de.codecentric:spring-boot-admin-starter-server")
	//implementation("org.apache.camel:camel-spring-boot-starter:2.24.0")
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.30+")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.30+")
	//implementation("org.jetbrains.kotlin:kotlinx-coroutines-core:1.2.1")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	runtime("com.fasterxml.jackson.module:jackson-module-kotlin")
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
		jvmArgs = listOf("-Dspring.config.location=config/application.properties")
	}
	test {
		dependsOn(composeUp)

	}
	dockerCompose {
		//isRequiredBy(bootRun.get())
		//isRequiredBy(test)
	}
}
