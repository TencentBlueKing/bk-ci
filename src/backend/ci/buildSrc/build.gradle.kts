// gradle使用kts
plugins {
    `kotlin-dsl`
}

// 插件使用仓库
repositories {
    var mavenRepoUrl: String? = System.getProperty("mavenRepoUrl")
    if (mavenRepoUrl == null) {
        mavenRepoUrl = System.getenv("mavenRepoUrl")
    }
    if (mavenRepoUrl == null) {
        mavenRepoUrl = project.extra["MAVEN_REPO_URL"]?.toString()
    }

    mavenLocal()
    maven(url = mavenRepoUrl ?: "https://repo.spring.io/libs-milestone")
    mavenCentral()
    jcenter()
}

// 依赖插件
dependencies {
    implementation("nu.studer:gradle-jooq-plugin:5.2.1")
    implementation("com.github.jengelman.gradle.plugins:shadow:5.2.0")
    implementation("org.ajoberstar:grgit:1.1.0")
}
