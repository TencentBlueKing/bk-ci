rootProject.name = "turbo"

include("api-turbo")
include("common-turbo")
include("common-turbo:common-turbo-api")
include("common-turbo:common-turbo-client")
include("common-turbo:common-turbo-service")
include("common-turbo:common-turbo-quartz")
include("common-turbo:common-turbo-util")
include("common-turbo:common-turbo-db")
include("common-turbo:common-turbo-web")
include("common-turbo:common-turbo-redis")
include("biz-turbo")
include("boot-turbo")
include("model-turbo")

pluginManagement {
    repositories {
        maven { url = java.net.URI("https://oss.sonatype.org/content/repositories/snapshots/") }
//        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
