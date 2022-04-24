import java.net.URI

@Suppress("NewLineAtEndOfFile")
plugins {
    `kotlin-dsl`
}

repositories {
    maven { url = URI("https://mirrors.tencent.com/nexus/repository/maven-public/") }
    maven { url = URI("https://mirrors.tencent.com/nexus/repository/gradle-plugins/") }
//    mavenCentral()
//    jcenter()
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
