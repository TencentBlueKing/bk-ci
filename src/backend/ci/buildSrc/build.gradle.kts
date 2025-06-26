// gradle使用kts
plugins {
    `kotlin-dsl`
}

// 插件使用仓库
repositories {
    if (System.getenv("GITHUB_WORKFLOW") == null) { // 普通环境
        maven(url = "https://mirrors.tencent.com/nexus/repository/maven-public")
        maven(url = "https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
    } else { // GitHub Action 环境
        mavenCentral()
        gradlePluginPortal()
    }
    mavenLocal()
}

// 依赖插件
dependencies {
    implementation("nu.studer.jooq:nu.studer.jooq.gradle.plugin:8.2.3")
    implementation("com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:7.1.2")
    implementation("com.google.cloud.tools:jib-gradle-plugin:3.3.1")
    implementation("com.github.jk1:gradle-license-report:2.1")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.apache.commons:commons-compress:1.26.2")// 解决冲突
    implementation("io.github.gradle-nexus.publish-plugin:io.github.gradle-nexus.publish-plugin.gradle.plugin:2.0.0")
}
