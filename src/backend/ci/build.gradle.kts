import com.tencent.devops.utils.findPropertyOrNull

plugins {
    id("com.tencent.devops.boot") version "0.0.4"
    detektCheck
}

allprojects {
    apply(plugin = "com.tencent.devops.boot")

    // 特殊的maven仓库
    project.findPropertyOrNull("mavenRepoUrl")?.let {
        project.repositories {
            all {
                if (this is MavenArtifactRepository && this.name == "TencentMirrors") {
                    this.setUrl(it)
                }
            }
        }
    }

    // 包路径
    group = "com.tencent.bk.devops.ci"
    // 版本
    version = (System.getProperty("ci_version") ?: "1.7.0") +
        if (System.getProperty("snapshot") == "true") "-SNAPSHOT" else "-RELEASE"

    // 版本管理
    dependencyManagement {
        setApplyMavenExclusions(false)
        dependencies {
            dependency("org.mockito:mockito-all:${Versions.Mockito}")
            dependency("com.nhaarman:mockito-kotlin-kt1.1:${Versions.MockitoKt}")
            dependency("javax.ws.rs:javax.ws.rs-api:${Versions.Jaxrs}")
            dependency("org.bouncycastle:bcprov-jdk15on:${Versions.BouncyCastle}")
            dependency("com.github.fge:json-schema-validator:${Versions.JsonSchema}")
            dependency("org.apache.commons:commons-exec:${Versions.CommonExec}")
            dependency("org.apache.commons:commons-text:${Versions.CommonText}")
            dependency("com.vdurmont:emoji-java:${Versions.EmojiJava}")
            dependency("org.apache.lucene:lucene-core:${Versions.Lucene}")
            dependency("org.apache.commons:commons-csv:${Versions.CommonCsv}")
            dependency("org.hashids:hashids:${Versions.HashIds}")
            dependency("com.github.ulisesbocchio:jasypt-spring-boot-starter:${Versions.Jasypt}")
            dependency("com.cronutils:cron-utils:${Versions.CronUtils}")
            dependency("org.apache.commons:commons-collections4:${Versions.CommonCollections4}")
            dependency("net.coobird:thumbnailator:${Versions.Thumbnailator}")
            dependency("com.vmware:vijava:${Versions.Vmware}")
            dependency("net.sf.json-lib:json-lib:${Versions.JsonLib}")
            dependency("com.googlecode.plist:dd-plist:${Versions.DdPlist}")
            dependency("net.dongliu:apk-parser:${Versions.ApkParser}")
            dependency("dom4j:dom4j:${Versions.Dom4j}")
            dependency("org.apache.ant:ant:${Versions.Ant}")
            dependency("cglib:cglib:${Versions.Cglib}")
            dependency("org.fusesource:sigar:${Versions.Sigar}")
            dependency("com.floragunn:search-guard-ssl:${Versions.SearchGuardSsl}")
            dependency("org.asynchttpclient:async-http-client:${Versions.AsyncHttpClient}")
            dependency("me.cassiano:ktlint-html-reporter:${Versions.KtlintHtmlReport}")
            dependency("com.github.shyiko:ktlint:${Versions.Ktlint}")
            dependency("org.elasticsearch:elasticsearch:${Versions.Elasticsearch}")
            dependency("org.elasticsearch.client:elasticsearch-rest-client:${Versions.Elasticsearch}")
            dependency("org.elasticsearch.client:elasticsearch-rest-high-level-client:${Versions.Elasticsearch}")
            dependency("com.github.oshi:oshi-core:${Versions.Oshi}")
            dependency("com.tencent.devops.leaf:leaf-boot-starter:${Versions.Leaf}")
            dependencySet("io.github.openfeign:${Versions.Feign}") {
                entry("feign-core")
                entry("feign-jackson")
                entry("feign-jaxrs")
                entry("feign-okhttp")
            }
            dependencySet("io.swagger:${Versions.Swagger}") {
                entry("swagger-annotations")
                entry("swagger-jersey2-jaxrs")
                entry("swagger-models")
            }
            dependencySet("com.github.docker-java:${Versions.DockerJava}") {
                entry("docker-java")
                entry("docker-java-transport-okhttp")
            }
            dependencySet("org.apache.logging.log4j:${Versions.log4j}"){
                entry("log4j-api")
                entry("log4j-core")
            }
            dependencySet("com.tencent.bkrepo:${Versions.TencentBkRepo}") {
                entry("api-generic")
                entry("api-repository")
            }
            dependencySet("org.apache.poi:${Versions.Poi}") {
                entry("poi")
                entry("poi-ooxml")
            }
            dependencySet("com.github.taptap:${Versions.PinyinPlus}") {
                entry("pinyin-plus")
            }
            dependency("com.perforce:p4java:${Versions.p4}")
        }
    }

    // 兼容Junit4
    dependencies {
        testImplementation("org.junit.vintage:junit-vintage-engine")
    }
    // 兼容 Log4j
    configurations.forEach {
        it.exclude("org.springframework.boot", "spring-boot-starter-logging")
        it.exclude("org.springframework.boot", "spring-boot-starter-tomcat")
        it.exclude("org.apache.tomcat", "tomcat-jdbc")
        it.exclude("org.slf4j", "log4j-over-slf4j")
        it.exclude("org.slf4j", "slf4j-log4j12")
        it.exclude("org.slf4j", "slf4j-nop")
        it.exclude("javax.ws.rs", "jsr311-api")
    }
}
