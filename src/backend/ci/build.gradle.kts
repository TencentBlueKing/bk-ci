plugins {
    id("com.tencent.devops.boot") apply false
}

apply("$rootDir/detekt.gradle.kts")

subprojects {
    apply(plugin = "com.tencent.devops.boot")
    apply(plugin = "maven")

    /************ https://github.com/bkdevops-projects/devops-framework/issues/73 ********/
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
            javaParameters = true
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    tasks.getByName("jar") {
        onlyIf {
            project.the<SourceSetContainer>()["main"].allSource.files.isNotEmpty()
        }
    }

    tasks.getByName("uploadArchives") {
        onlyIf {
            project.the<SourceSetContainer>()["main"].allSource.files.isNotEmpty()
        }
    }

    tasks.getByName("install") {
        onlyIf {
            project.the<SourceSetContainer>()["main"].allSource.files.isNotEmpty() &&
                    !name.startsWith("model-") &&
                    !name.startsWith("boot-") &&
                    !name.startsWith("biz-")
        }
    }

    configurations.forEach {
        it.exclude("org.springframework.boot", "spring-boot-starter-logging")
        it.exclude("org.springframework.boot", "spring-boot-starter-tomcat")
        it.exclude("org.apache.tomcat", "tomcat-jdbc")
        it.exclude("org.slf4j", "log4j-over-slf4j")
        it.exclude("org.slf4j", "slf4j-log4j12")
        it.exclude("org.slf4j", "slf4j-nop")
    }

    dependencies {
        "testImplementation"("junit:junit")
    }
    /*****************************************************************************************/

    // 包路径
    group = "com.tencent.bk.devops.ci"
    // 版本
    version = System.getProperty("ci_version") ?: "1.6.0"
    version = if (System.getProperty("snapshot") == "true") {
        version as String + "-SNAPSHOT"
    } else {
        version as String + "-RELEASE"
    }

    // 仓库
    val mavenRepoUrl = System.getProperty("mavenRepoUrl")
        ?: System.getenv("mavenRepoUrl")
        ?: extra["MAVEN_REPO_URL"] as String

    repositories {
        mavenLocal()
        maven(url = mavenRepoUrl)
        maven(url = "https://repo.spring.io/libs-milestone")
        mavenCentral()
        jcenter()
    }

    // 创建目录
    task("createCodeDirs") {
        listOf(
            "src/main/java",
            "src/main/kotlin",
            "src/main/resources",
            "src/test/java",
            "src/test/kotlin",
            "src/test/resources"
        ).forEach {
            val f = File(it)
            if (!f.exists()) {
                f.mkdirs()
            }
        }
    }


    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
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
}
