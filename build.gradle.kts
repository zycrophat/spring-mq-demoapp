import org.unbrokendome.gradle.plugins.gitversion.model.HasObjectId

plugins {
    id("base")
    id("idea")
    id("org.unbroken-dome.gitversion") version "0.10.0" apply true
    id("com.github.jk1.dependency-license-report") version "1.6" apply false
    id("org.owasp.dependencycheck") version "5.0.0-M3.1" apply false
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

allprojects {
    apply(plugin = "com.github.jk1.dependency-license-report")
    apply(plugin = "org.owasp.dependencycheck")

    group = "steffan"
    version = rootProject.version

    repositories {
        jcenter()
        mavenCentral()
    }

    configurations.all {
        resolutionStrategy {
            preferProjectModules()
            dependencySubstitution {
                substitute(module("com.fasterxml.jackson.core:jackson-core"))
                        .with(module("com.fasterxml.jackson.core:jackson-core:[2.9.9,)"))

                substitute(module("com.fasterxml.jackson.core:jackson-databind"))
                        .with(module("com.fasterxml.jackson.core:jackson-databind:[2.9.9,)"))

                substitute(module("com.google.guava:guava"))
                        .with(module("com.google.guava:guava:[27.1-jre,)"))
            }
        }
    }

}


