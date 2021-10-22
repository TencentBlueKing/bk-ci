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
