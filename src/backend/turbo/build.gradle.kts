plugins {
    id("com.tencent.devops.boot") version "0.0.7-SNAPSHOT"
    id("org.owasp.dependencycheck") version "7.1.0.1"
}

allprojects {
    group = "com.tencent.bk.devops.turbo"

    version = (System.getProperty("turbo_version") ?: "0.0.2") +
        if (System.getProperty("snapshot") == "true") "-SNAPSHOT" else "-RELEASE"

    apply(plugin = "com.tencent.devops.boot")

    repositories {
        maven(url = "https://mirrors.tencent.com/repository/maven/bkdevops_maven")
        maven(url = "https://mirrors.tencent.com/nexus/repository/maven-public")
        maven(url = "https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
    }

    configurations.all {
        exclude(group = "org.slf4j", module = "log4j-over-slf4j")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "org.slf4j", module = "slf4j-nop")
        resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.MINUTES)
    }

    dependencyManagement {
        imports {
            mavenBom("com.tencent.bk.devops.ci.common:common-dependencies:${Versions.ciVersion}")
        }
        dependencies {
            dependency("javax.ws.rs:javax.ws.rs-api:${Versions.jaxrsVersion}")
            dependency("com.github.ulisesbocchio:jasypt-spring-boot-starter:${Versions.jasyptVersion}")
            dependency("org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastleVersion}")
            dependency("com.google.guava:guava:${Versions.guavaVersion}")
            dependency("io.jsonwebtoken:jjwt:${Versions.jjwtVersion}")
            dependency("commons-io:commons-io:${Versions.commonIo}")

            Versions.ciVersion.let {
                dependency("com.tencent.bk.devops.ci.project:api-project:$it")
                dependency("com.tencent.bk.devops.ci.common:common-api:$it")
                dependency("com.tencent.bk.devops.ci.metrics:api-metrics:$it")
                dependency("com.tencent.bk.devops.ci.auth:api-auth:$it")
                dependency("com.tencent.bk.devops.ci.common:common-auth-api:$it")
                dependency("com.tencent.bk.devops.ci.common:common-event:$it")
                dependency("com.tencent.bk.devops.ci.common:common-api:$it")
            }
        }
    }
}
