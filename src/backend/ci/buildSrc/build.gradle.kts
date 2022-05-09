// gradle使用kts
plugins {
    `kotlin-dsl`
}

// 插件使用仓库
repositories {
    mavenLocal()
    maven(url = "https://plugins.gradle.org/m2/")
    mavenCentral()
    jcenter()
}

// 依赖插件
dependencies {
    implementation("nu.studer:gradle-jooq-plugin:5.2.1")
    implementation("com.github.jengelman.gradle.plugins:shadow:6.1.0")
    implementation("org.apache.logging.log4j:log4j-core:2.17.1")
    implementation("org.owasp:dependency-check-gradle:7.0.0")
}
