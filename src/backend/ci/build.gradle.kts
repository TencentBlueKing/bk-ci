plugins {
    kotlin("jvm")
}

apply("$rootDir/detekt.gradle.kts")

allprojects {
    // 包路径
    group = "com.tencent.bk.devops.ci"

    // 版本
    version = "1.5.5"
    val devopsVersion = System.getProperty("ci_version")
    if (devopsVersion != null) {
        version = devopsVersion
    }
    val snapshot = System.getProperty("snapshot")
    version = if (snapshot == "true") {
        version as String + "-SNAPSHOT"
    } else {
        version as String + "-RELEASE"
    }

    // 仓库
    var mavenRepoUrl = System.getProperty("mavenRepoUrl")
    if (mavenRepoUrl == null) {
        mavenRepoUrl = System.getenv("mavenRepoUrl")
    }
    if (mavenRepoUrl == null) {
        mavenRepoUrl = extra["MAVEN_REPO_URL"] as String
    }
    repositories {
        mavenLocal()
        maven(url = mavenRepoUrl)
        maven(url = "https://repo.spring.io/libs-milestone")
        mavenCentral()
        jcenter()
    }

    // 创建目录
    task("createCodeDirs") {
        val paths = listOf(
            "src/main/java",
            "src/main/kotlin",
            "src/main/resources",
            "src/test/java",
            "src/test/kotlin",
            "src/test/resources"
        )
        paths.forEach {
            val f = File(it)
            if (!f.exists()) {
                f.mkdirs()
            }
        }
    }
}

subprojects {
    apply(plugin = "idea")
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "maven")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "io.spring.dependency-management")

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
            sourceSets.main.get().allSource.files.isNotEmpty()
        }
    }

    tasks.getByName("uploadArchives") {
        onlyIf {
            sourceSets.main.get().allSource.files.isNotEmpty()
        }
    }

    tasks.getByName("install") {
        onlyIf {
            sourceSets.main.get().allSource.files.isNotEmpty() &&
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
        it.exclude("javax.ws.rs", "jsr311-api")
    }

    dependencies {
        testImplementation("junit:junit")
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        applyMavenExclusions(false)

        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${Versions.SpringBoot}")
        }

        dependencies {
            dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.Kotlin}")
            dependency("org.jetbrains.kotlin:kotlin-reflect:${Versions.Kotlin}")
            dependency("com.google.guava:guava:${Versions.Guava}")
            dependency("com.floragunn:search-guard-ssl:${Versions.SearchGuardSsl}")
            dependency("org.elasticsearch:elasticsearch:${Versions.Elasticsearch}")
            dependency("org.elasticsearch.client:elasticsearch-rest-client:${Versions.Elasticsearch}")
            dependency("org.elasticsearch.client:elasticsearch-rest-high-level-client:${Versions.Elasticsearch}")
            dependency("org.hashids:hashids:${Versions.HashIds}")
            dependency("javax.ws.rs:javax.ws.rs-api:${Versions.Jaxrs}")
            dependency("com.squareup.okhttp3:okhttp:${Versions.OkHttp}")
            dependency("org.apache.httpcomponents:httpclient:${Versions.Httpclient}")
            dependency("org.apache.commons:commons-exec:${Versions.CommonExec}")
            dependency("com.vmware:vijava:${Versions.Vmware}")
            dependency("org.bouncycastle:bcprov-jdk16:${Versions.BouncyCastle}")
            dependency("dom4j:dom4j:${Versions.Dom4j}")
            dependency("org.apache.commons:commons-compress:${Versions.Compress}")
            dependency("org.reflections:reflections:${Versions.Reflections}")
            dependency("com.github.fge:json-schema-validator:${Versions.JsonSchema}")
            dependency("com.github.ulisesbocchio:jasypt-spring-boot-starter:${Versions.Jasypt}")
            dependency("org.jolokia:jolokia-core:${Versions.Jolokia}")
            dependency("org.apache.commons:commons-lang3:${Versions.CommonLang3}")
            dependency("commons-codec:commons-codec:${Versions.CommonCodec}")
            dependency("org.jooq:jooq:${Versions.Jooq}")
            dependency("org.apache.lucene:lucene-core:${Versions.Lucene}")
            dependency("io.jsonwebtoken:jjwt:${Versions.Jjwt}")
            dependency("org.mockito:mockito-all:${Versions.Mockito}")
            dependency("net.sf.json-lib:json-lib:${Versions.JsonLib}")
            dependency("com.cronutils:cron-utils:${Versions.CronUtils}")
            dependency("com.amazonaws:aws-java-sdk-s3:${Versions.AwsS3}")
            dependency("org.asynchttpclient:async-http-client:${Versions.AsyncHttpClient}")
            dependency("com.tencent.devops.ci-plugins:sigar:${Versions.Sigar}")
            dependency("org.influxdb:influxdb-java:${Versions.InfluxDB}")
            dependency("com.github.ben-manes.caffeine:caffeine:${Versions.Caffeine}")
            dependency("com.github.ben-manes.caffeine:caffeine:${Versions.Caffeine}")
            dependency("org.apache.logging.log4j:log4j-core:${Versions.log4jVersion}")
            dependency("org.apache.logging.log4j:log4j-api:${Versions.log4jVersion}")

            dependencySet("org.springframework.cloud:${Versions.SpringConsul}") {
                entry("spring-cloud-starter-consul-discovery")
                entry("spring-cloud-starter-consul-core")
                entry("spring-cloud-starter-config")
                entry("spring-cloud-config-server")
            }

            dependencySet("io.swagger:${Versions.Swagger}") {
                entry("swagger-annotations")
                entry("swagger-jersey2-jaxrs")
            }

            dependencySet("com.fasterxml.jackson.module:${Versions.Jackson}") {
                entry("jackson-module-kotlin")
            }

            dependencySet("com.fasterxml.jackson.core:${Versions.Jackson}") {
                entry("jackson-core")
                entry("jackson-databind")
                entry("jackson-annotations")
            }

            dependencySet("com.fasterxml.jackson.jaxrs:${Versions.Jackson}") {
                entry("jackson-jaxrs-json-provider")
                entry("jackson-jaxrs-base")
            }

            dependencySet("io.github.openfeign:${Versions.Feign}") {
                entry("feign-jaxrs")
                entry("feign-okhttp")
                entry("feign-jackson")
            }

            dependencySet("io.github.openfeign.form:${Versions.FeignForm}") {
                entry("feign-form")
                entry("feign-form-spring")
            }

            dependencySet("org.slf4j:${Versions.Slf4j}") {
                entry("slf4j-api")
                entry("slf4j-simple")
            }

            dependencySet("ch.qos.logback:${Versions.Logback}") {
                entry("logback-core")
                entry("logback-classic")
            }

            dependencySet("org.apache.poi:${Versions.Poi}") {
                entry("poi")
                entry("poi-ooxml")
            }
        }
    }
}
