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
}
