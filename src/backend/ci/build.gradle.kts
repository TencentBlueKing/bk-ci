plugins {
    id("com.tencent.devops.boot")
    detektCheck
}

allprojects {
    apply(plugin = "com.tencent.devops.boot")

    // 包路径
    group = "com.tencent.bk.devops.ci"
    // 版本
    version = (System.getProperty("ci_version") ?: "1.6.0") +
            if (System.getProperty("snapshot") == "true") "-SNAPSHOT" else "-RELEASE"
    // 仓库
    repositories {
        mavenLocal()
        maven(
            url = System.getProperty("mavenRepoUrl")
                ?: System.getenv("mavenRepoUrl")
                ?: extra["MAVEN_REPO_URL"]?.toString()
                ?: "https://repo.spring.io/libs-milestone"
        )
        mavenCentral()
        jcenter()
    }

    // 版本管理
    dependencyManagement {
        dependencies {
            dependency("org.mockito:mockito-all:${Versions.Mockito}")
            dependency("com.nhaarman:mockito-kotlin-kt1.1:${Versions.MockitoKt}")
            dependency("javax.ws.rs:javax.ws.rs-api:${Versions.Jaxrs}")
            dependency("org.bouncycastle:bcprov-jdk16:${Versions.BouncyCastle}")
            dependency("com.github.fge:json-schema-validator:${Versions.JsonSchema}")
            dependency("org.apache.commons:commons-exec:${Versions.CommonExec}")
            dependency("com.vdurmont:emoji-java:${Versions.EmojiJava}")
            dependency("org.apache.lucene:lucene-core:${Versions.Lucene}")
            dependency("org.apache.commons:commons-csv:${Versions.CommonCsv}")
            dependency("org.hashids:hashids:${Versions.HashIds}")
            dependency("com.github.ulisesbocchio:jasypt-spring-boot-starter:${Versions.Jasypt}")
            dependency("com.cronutils:cron-utils:${Versions.CronUtils}")
            dependency("org.apache.commons:commons-collections4:${Versions.CommonCollections4}")
            dependency("net.coobird:thumbnailator:${Versions.Thumbnailator}")
            dependency("com.vmware:vijava:${Versions.Vmware}")
            dependency("com.tencent.devops.ci-plugins:sigar:${Versions.Sigar}")
            dependency("net.sf.json-lib:json-lib:${Versions.JsonLib}")
            dependency("com.googlecode.plist:dd-plist:${Versions.DdPlist}")
            dependency("com.github.oshi:oshi-core:${Versions.OshiCore}")
            dependency("net.dongliu:apk-parser:${Versions.ApkParser}")
            dependency("dom4j:dom4j:${Versions.Dom4j}")

            dependencySet("io.swagger:${Versions.Swagger}") {
                entry("swagger-annotations")
                entry("swagger-jersey2-jaxrs")
                entry("swagger-models")
            }

            dependencySet("com.github.docker-java:${Versions.DockerJava}") {
                entry("docker-java")
                entry("docker-java-transport-okhttp")
            }

            dependencySet("com.tencent.bkrepo:${Versions.TencentBkRepo}") {
                entry("api-generic")
                entry("api-repository")
            }
        }
    }

    // 兼容Junit4
    dependencies {
        testImplementation("org.junit.vintage:junit-vintage-engine")
    }
    // 兼容 Log4j
    configurations.forEach {
        it.exclude("org.springframework.boot", "spring-boot-starter-logging")
    }
}
