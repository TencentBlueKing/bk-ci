plugins {
    `kotlin-dsl`
}

// 仓库
repositories {
    var mavenRepoUrl: String? = System.getProperty("mavenRepoUrl")
    if (mavenRepoUrl == null) {
        mavenRepoUrl = System.getenv("mavenRepoUrl")
    }
    if (mavenRepoUrl == null) {
        mavenRepoUrl = project.extra["MAVEN_REPO_URL"]?.toString()
    }

    mavenLocal()
    mavenRepoUrl?.let { maven(url = it) }
    maven(url = "https://repo.spring.io/libs-milestone")
    mavenCentral()
    jcenter()
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.3.72")
    implementation("nu.studer:gradle-jooq-plugin:5.2.1")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.3.7.RELEASE")
    implementation("io.spring.gradle:dependency-management-plugin:1.0.10.RELEASE")
    implementation("com.github.jengelman.gradle.plugins:shadow:5.2.0")
    implementation("org.ajoberstar:grgit:1.1.0")
    implementation("org.apache.logging.log4j:log4j-core:2.17.1")
}
