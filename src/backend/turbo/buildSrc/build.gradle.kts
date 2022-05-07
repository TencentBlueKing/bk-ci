@Suppress("NewLineAtEndOfFile")
plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    if (System.getenv("GITHUB_WORKFLOW") == null) {
        // 普通环境
        maven(url = "https://mirrors.tencent.com/nexus/repository/maven-public")
        maven(url = "https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
    } else {
        // GitHub Action 环境
        mavenCentral()
        gradlePluginPortal()
    }
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
