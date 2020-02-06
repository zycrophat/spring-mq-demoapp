pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
rootProject.name = "spring-mq-demoapp"

include("spring-mq-demoapp-boot-sampleservice")
include("spring-mq-demoapp-boot-admin")
include("spring-mq-demoapp-boot-common")
include("spring-mq-demoapp-boot-stopper")
include("spring-mq-demoapp-boot-passwordhelper")
include("spring-mq-demoapp-boot-encryptstringhelper")
include("spring-mq-demoapp-boot-dpapipropertysupport")