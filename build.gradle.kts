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
        version.patch = countCommitsSince(tag as HasObjectId, true)
    }

    onBranch("master") {
        val tag = findLatestTag(versionPattern)
        if (countCommitsSince(tag as HasObjectId) != 0) {
            version.setPrereleaseTag("SNAPSHOT")
        }
        isSkipOtherRules = true
    }

    always {
        version.setPrereleaseTag(branchName)
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

}
