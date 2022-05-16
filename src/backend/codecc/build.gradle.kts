import com.tencent.devops.utils.findPropertyOrEmpty
import com.tencent.devops.enums.AssemblyMode

plugins {
    id("com.tencent.devops.boot")
}

buildscript {
    dependencies {
        classpath("org.owasp:dependency-check-gradle:7.1.0.1")
    }

}

allprojects {
    group = "com.tencent.bk.codecc"
    version = "0.0.2"

    apply(plugin = "com.tencent.devops.boot")
    apply(plugin = "org.owasp.dependencycheck")


//    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
//        kotlinOptions {
//            val list = mutableListOf<String>(
//                "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes",
//                "-XXLanguage:+InlineClasses")
//            list.addAll(freeCompilerArgs)
//            freeCompilerArgs = list
//        }
//    }

    val property = project.findPropertyOrEmpty("devops.assemblyMode").trim()
    if (project.name.startsWith("boot-")) {
        when (AssemblyMode.ofValueOrDefault(property)) {
            AssemblyMode.CONSUL -> {
                project.configurations.all {
                    exclude(group = "org.springframework.cloud", module = "spring-cloud-starter-kubernetes-client")
                    exclude(group = "org.springframework.cloud",
                        module = "spring-cloud-starter-kubernetes-client-config")
                }
            }
            AssemblyMode.K8S, AssemblyMode.KUBERNETES -> {
                project.configurations.all {
                    exclude(group = "org.springframework.cloud", module = "spring-cloud-starter-config")
                    exclude(group = "org.springframework.cloud", module = "spring-cloud-starter-consul-config")
                    exclude(group = "org.springframework.cloud", module = "spring-cloud-starter-consul-discovery")
                }
            }
        }
    }


    configurations {
        all {
            exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
            exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
            exclude(group = "org.apache.tomcat", module = "tomcat-jdbc")
            exclude(group = "org.slf4j", module = "log4j-over-slf4j")
            exclude(group = "org.slf4j", module = "slf4j-log4j12")
            exclude(group = "org.slf4j", module = "slf4j-nop")
            exclude(group = "ch.qos.logback", module = "logback-classic")
            exclude(group = "com.tencent.bk.devops.ci.common", module = "common-archive-tencent")
            exclude(group = "com.tencent.bk.devops.ci.common", module = "common-client")
            exclude(group = "com.github.ulisesbocchio", module = "jasypt-spring-boot-starter")
        }
        if (project.name.contains("biz-codeccjob") && project.name != "boot-codeccjob") {
//			all { exclude(group = "org.springframework.boot", module = "spring-boot-starter-websocket") }
            all { exclude(group = "io.undertow", module = "undertow-websockets-jsr") }
        }
    }

    dependencyManagement {

        applyMavenExclusions(false)


        dependencies {
            dependency("org.hashids:hashids:${Versions.hashidsVersion}")
            dependency("javax.ws.rs:javax.ws.rs-api:${Versions.jaxrsVersion}")
            dependency("org.tmatesoft.svnkit:svnkit:${Versions.svnkitVersion}")
            dependency("com.squareup.okhttp3:okhttp:${Versions.okHttpVersion}")
            dependency("org.apache.httpcomponents:httpclient:${Versions.httpclientVersion}")
            dependency("org.apache.commons:commons-exec:${Versions.commonExecVersion}")
            dependency("org.apache.commons:commons-pool2:${Versions.commonPool2Version}")
            dependency("com.vmware:vijava:${Versions.vmwareVersion}")
            dependency("org.bouncycastle:bcprov-ext-jdk15on:${Versions.bouncyCastleVersion}")
            dependency("dom4j:dom4j:${Versions.dom4jVersion}")
            dependency("org.apache.commons:commons-compress:${Versions.compressVersion}")
            dependency("org.reflections:reflections:${Versions.reflectionsVersion}")
            dependency("com.github.fge:json-schema-validator:${Versions.jsonSchemaVersion}")
            //dependency "com.github.ulisesbocchio:jasypt-spring-boot-starter:$jasyptVersion"
            dependency("org.jolokia:jolokia-core:${Versions.jolokiaVersion}")
            dependency("org.projectlombok:lombok:${Versions.lombokVersion}")
            dependency("org.apache.tomcat.embed:tomcat-embed-core:${Versions.tomcatEmbedCoreVersion}")
            dependency("commons-collections:commons-collections:${Versions.commonCollection}")
            dependency("biz.paluch.redis:lettuce:${Versions.lettuceVersion}")
//			dependency("org.springframework.data:spring-data-redis:${Versions.springRedisVersion}")
            dependency("org.glassfish.jersey.ext:jersey-bean-validation:${Versions.jerseyValidationVersion}")
            dependency("commons-io:commons-io:${Versions.commonsIOVersion}")
            dependency("commons-httpclient:commons-httpclient:${Versions.commonsHttpclientVersion}")
            dependency("com.alibaba:easyexcel:${Versions.easyexcel}")
            dependency("org.redisson:redisson:${Versions.redisson}")
            dependency("org.apache.lucene:lucene-core:${Versions.lucene}")
            dependencySet("io.swagger:${Versions.swaggerVersion}") {
                entry("swagger-annotations")
                entry("swagger-jersey2-jaxrs")
                entry("swagger-models")
                entry("swagger-core")
                entry("swagger-jaxrs")
            }
//            dependencySet("com.fasterxml.jackson.module:${Versions.jacksonVersion}") {
//                entry("jackson-module-kotlin")
//            }
//            dependencySet("com.fasterxml.jackson.core:${Versions.jacksonVersion}") {
//                entry("jackson-core")
//                entry("jackson-databind")
//                entry("jackson-annotations")
//            }
//            dependencySet("com.fasterxml.jackson.jaxrs:${Versions.jacksonVersion}") {
//                entry("jackson-jaxrs-json-provider")
//                entry("jackson-jaxrs-base")
//            }
            dependencySet("io.github.openfeign:${Versions.feignVersion}") {
                entry("feign-jaxrs")
                entry("feign-okhttp")
                entry("feign-jackson")
            }
            dependencySet("org.slf4j:${Versions.slf4jVersion}") {
                entry("slf4j-api")
                entry("slf4j-simple")
            }
            dependencySet("io.jsonwebtoken:${Versions.jjwtVersion}") {
                entry("jjwt")
            }

            dependencySet("org.mockito:${Versions.mockitoVersion}") {
                entry("mockito-all")
            }
            dependencySet("net.sf.json-lib:${Versions.jsonLibVersion}") {
                entry("json-lib")
            }
            dependencySet("com.cronutils:${Versions.cronutilsVersion}") {
                entry("cron-utils")
            }
            dependencySet("ch.qos.logback:${Versions.logbackVersion}") {
                entry("logback-core")
                entry("logback-classic")
            }
            dependencySet("com.amazonaws:${Versions.awsS3Version}") {
                entry("aws-java-sdk-s3")
            }
            dependencySet("org.apache.poi:${Versions.poiVersion}") {
                entry("poi")
                entry("poi-ooxml")
            }
            dependencySet("org.apache.logging.log4j:${Versions.log4j}"){
                entry("log4j-api")
                entry("log4j-core")
                entry("log4j-slf4j-impl")
            }
            dependency("com.google.guava:guava:${Versions.guava}")
            dependency("commons-beanutils:commons-beanutils:${Versions.beanUtils}")
            dependencySet("org.glassfish.jersey.core:${Versions.jerseyCommon}"){
                entry("jersey-common")
            }

        }
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        implementation("org.springframework.boot:spring-boot-starter-jersey")
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
        implementation("org.glassfish.jersey.media:jersey-media-multipart")
        implementation("org.glassfish.jersey.ext:jersey-bean-validation")

        testImplementation(group = "com.github.shyiko", name = "ktlint", version = Versions.ktlintVersion)
        testImplementation("junit:junit")
        testImplementation("org.mockito:mockito-all")
        testImplementation("com.nhaarman:mockito-kotlin-kt1.1:1.6.0")
    }
}




