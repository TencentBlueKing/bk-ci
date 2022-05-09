@Suppress("NewLineAtEndOfFile")
plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    maven(url = "https://plugins.gradle.org/m2/")
    mavenCentral()
    jcenter()
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

// 依赖插件
dependencies {
    implementation("org.owasp:dependency-check-gradle:7.0.0")
}
