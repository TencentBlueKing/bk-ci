plugins {
    id("com.tencent.devops.boot") version "1.0.0"
    detektCheck
    nexusPublishing
    licenseReport // 检查License合规
}

allprojects {
    apply(plugin = "com.tencent.devops.boot")
    // 包路径
    group = "com.tencent.bk.devops.ci"
    // 版本
    version = (System.getProperty("ci_version") ?: "1.9.0") +
            if (System.getProperty("snapshot") == "true") "-SNAPSHOT" else ""

    // 加载boot的插件
    if (name.startsWith("boot-")) {
        pluginManager.apply("task-sharding-db-table-check") // 分区表检查插件
        pluginManager.apply("task-i18n-load") // i18n插件
        if (System.getProperty("devops.assemblyMode") == "KUBERNETES") {
            pluginManager.apply("task-docker-build") // Docker镜像构建
        }
    }

    // 新增maven 仓库
    repositories {
        add(maven { url = uri("https://repo.jenkins-ci.org/releases") })
        add(maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") })
    }

    // 版本管理
    dependencyManagement {
        setApplyMavenExclusions(false)
        dependencies {
            dependency("org.json:json:${Versions.orgJson}")
            dependency("org.bouncycastle:bcpkix-jdk18on:${Versions.BouncyCastle}")
            dependency("org.bouncycastle:bcprov-jdk18on:${Versions.BouncyCastle}")
            dependency("com.github.fge:json-schema-validator:${Versions.JsonSchema}")
            dependency("com.networknt:json-schema-validator:${Versions.YamlSchema}")
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
            dependency("net.sf.json-lib:json-lib:${Versions.JsonLib}")
            dependency("com.googlecode.plist:dd-plist:${Versions.DdPlist}")
            dependency("net.dongliu:apk-parser:${Versions.ApkParser}")
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
            dependency("org.apache.pulsar:pulsar-client:${Versions.Pulsar}")
            dependency("com.github.oshi:oshi-core:${Versions.Oshi}")
            dependency("com.tencent.devops.leaf:leaf-boot-starter:${Versions.Leaf}")
            dependency("com.github.xingePush:xinge:${Versions.Xinge}")
            dependency("org.reflections:reflections:${Versions.reflections}")
            dependency("org.dom4j:dom4j:${Versions.Dom4j}")
            dependency("org.apache.commons:commons-compress:${Versions.Compress}")
            dependency("org.bouncycastle:bcprov-ext-jdk15on:${Versions.BouncyCastle}")
            dependency("org.mybatis:mybatis:${Versions.MyBatis}")
            dependency("commons-io:commons-io:${Versions.CommonIo}")
            dependency("com.tencent.bk.sdk:crypto-java-sdk:${Versions.BkCrypto}")
            dependency("mysql:mysql-connector-java:${Versions.MysqlDriver}")
            dependency("org.apache.shardingsphere:shardingsphere-jdbc:${Versions.ShardingSphere}")
            dependency("org.apache.shardingsphere:shardingsphere-infra-algorithm-core:${Versions.ShardingSphere}")
            dependencySet("org.glassfish.jersey.containers:${Versions.Jersey}") {
                entry("jersey-container-servlet-core")
                entry("jersey-container-servlet")
            }
            dependencySet("org.glassfish.jersey.core:${Versions.Jersey}") {
                entry("jersey-server")
                entry("jersey-common")
                entry("jersey-client")
            }
            dependencySet("org.glassfish.jersey.ext:${Versions.Jersey}") {
                entry("jersey-bean-validation")
                entry("jersey-entity-filtering")
                entry("jersey-spring6")
            }
            dependencySet("org.glassfish.jersey.media:${Versions.Jersey}") {
                entry("jersey-media-multipart")
                entry("jersey-media-json-jackson")
            }
            dependency("org.glassfish.jersey.inject:jersey-hk2:${Versions.Jersey}")
            dependencySet("io.swagger.core.v3:${Versions.Swagger}") {
                entry("swagger-annotations-jakarta")
                entry("swagger-jaxrs2-jakarta")
                entry("swagger-models-jakarta")
            }
            dependencySet("com.github.docker-java:${Versions.DockerJava}") {
                entry("docker-java-core")
                entry("docker-java-transport-okhttp")
            }
            dependencySet("com.tencent.bk.repo:${Versions.TencentBkRepo}") {
                entry("api-generic")
                entry("api-repository")
                entry("api-webhook")
            }
            dependencySet("org.apache.poi:${Versions.Poi}") {
                entry("poi")
                entry("poi-ooxml")
            }
            dependency("com.perforce:p4java:${Versions.p4}")
            dependency("io.mockk:mockk:${Versions.mockk}")
            dependencySet("io.github.resilience4j:${Versions.Resilience4j}") {
                entry("resilience4j-circuitbreaker")
                entry("resilience4j-core")
            }
            dependencySet("org.eclipse.jgit:${Versions.jgit}") {
                entry("org.eclipse.jgit")
                entry("org.eclipse.jgit.ssh.jsch")
            }
            dependency("com.tencent.bk.sdk:iam-java-sdk:${Versions.iam}")
            dependency("com.tencent.bk.sdk:spring-boot-bk-audit-starter:${Versions.audit}")
            dependency("com.jakewharton:disklrucache:${Versions.disklrucache}")
            // worker需要依赖
            dependency("org.jvnet.winp:winp:${Versions.Winp}")
            dependency("net.java.dev.jna:jna:${Versions.Jna}")
            dependency("org.jenkins-ci:version-number:${Versions.JenkinsVersionNumber}")
            dependencySet("com.tencent.bk.devops.scm:${Versions.devopsScm}") {
                entry("devops-scm-api")
                entry("devops-scm-spring-boot-starter")
            }
        }
    }

    // 兼容 Log4j
    configurations.forEach {
        it.exclude("org.springframework.boot", "spring-boot-starter-tomcat")
        it.exclude("org.apache.tomcat", "tomcat-jdbc")
        it.exclude("org.slf4j", "log4j-over-slf4j")
        it.exclude("org.slf4j", "slf4j-log4j12")
        it.exclude("org.slf4j", "slf4j-nop")
        it.exclude("dom4j", "dom4j")
        it.exclude("com.flipkart.zjsonpatch", "zjsonpatch")
        it.exclude("com.zaxxer", "HikariCP-java7")
        it.exclude("com.tencent.devops", "devops-boot-starter-plugin")
        it.exclude("org.bouncycastle", "bcutil-jdk15on")
        it.exclude("io.swagger", "swagger-annotations")
        it.exclude("io.swagger", "swagger-models")
        it.exclude("commons-logging", "commons-logging")
        it.exclude("com.vaadin.external.google", "android-json")
        it.exclude("org.apache.shardingsphere", "shardingsphere-test-util")
    }
    dependencies {
        // 兼容dom4j 的 bug : https://github.com/gradle/gradle/issues/13656
        components {
            withModule("org.dom4j:dom4j") {
                allVariants { withDependencies { clear() } }
            }
        }
    }
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0,"seconds")
        resolutionStrategy.cacheDynamicVersionsFor(0,"seconds")
    }
}
