import org.unbrokendome.gradle.plugins.gitversion.model.HasObjectId
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    id("base")
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("org.springframework.boot") version LibraryVersions.SPRING_BOOT_VERSION apply false
    id("io.spring.dependency-management") version "1.0.7.RELEASE" apply false
    id("org.jetbrains.kotlin.plugin.spring") version LibraryVersions.KOTLIN_VERSION apply false
    id("org.unbroken-dome.gitversion") version "0.10.0" apply true
    id("com.github.jk1.dependency-license-report") version "1.11" apply true
    id("org.owasp.dependencycheck") version "5.0.0-M3.1" apply false
    kotlin("jvm") version LibraryVersions.KOTLIN_VERSION apply false
    kotlin("kapt") version LibraryVersions.KOTLIN_VERSION apply false
}

gitVersion.rules {
    val minorVersionPattern = """v?(\d+)\.(\d+)\.(0)""".toPattern()
    val patchVersionPattern = """v?(\d+)\.(\d+)\.(\d+)""".toPattern()
    
    before {
        val tag = findLatestTag(minorVersionPattern)
        version.major = tag?.matches?.getAt(1)?.toInt() ?: 0
        version.minor = tag?.matches?.getAt(2)?.toInt() ?: 0
    }

    always {
        val latestMinorVersionTag = findLatestTag(minorVersionPattern)
        val countCommitsSinceLatestMinorVersionTag = countCommitsSince(latestMinorVersionTag as HasObjectId, true)
        val latestPatchVersionTag = findLatestTag(patchVersionPattern)
        val latestPatch = latestPatchVersionTag?.matches?.getAt(3)?.toInt() ?: 0

        val head = head
        if (head?.id != latestPatchVersionTag?.commit?.id) {
            if (countCommitsSinceLatestMinorVersionTag != latestPatch) {
                val timeStamp =
                        DateTimeFormatter
                        .ofPattern("YYYYMMddHHmmss")
                        .format(LocalDateTime.now(ZoneOffset.UTC))

                val tag = findLatestTag(patchVersionPattern)
                version.patch = tag?.matches?.getAt(3)?.toInt() ?: 0

                val label = if ((branchName ?: "HEAD") != "HEAD") branchName else head?.id(6)
                val countCommitsSinceTag = countCommitsSince(tag as HasObjectId, true)

                version.setPrereleaseTag("SNAPSHOT")
                version.setBuildMetadata("$countCommitsSinceTag-$label-$timeStamp")
            }
        } else {
            version.patch = latestPatch
        }
    }
}

version = gitVersion.determineVersion()

allprojects {
    apply(plugin = "idea")
    apply(plugin = "eclipse")
    apply(plugin = "com.github.jk1.dependency-license-report")
    apply(plugin = "org.owasp.dependencycheck")
    apply(plugin = "maven-publish")

    group = "steffan"
    version = rootProject.version

    repositories {
        jcenter()
        mavenCentral()
        maven (
                url = "http://repo.jenkins-ci.org/releases"
        )
    }

    publishing {
        publications.withType(MavenPublication::class){
            pom {
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://raw.githubusercontent.com/zycrophat/spring-mq-demoapp/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("zycrophat")
                        name.set("Andreas Steffan")
                        url.set("https://github.com/zycrophat")
                    }
                }
                scm {
                    url.set("https://github.com/zycrophat/spring-mq-demoapp")
                    connection.set("scm:git:git@github.com:zycrophat/spring-mq-demoapp.git")
                    developerConnection.set("scm:git:git@github.com:zycrophat/spring-mq-demoapp.git")
                }
            }
        }
    }

    licenseReport {
        allowedLicensesFile = file("$rootDir/config/allowed-licenses.json")
    }

}
