plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api("javax.ws.rs:javax.ws.rs-api")
    api("org.hashids:hashids")
    //注意 新加的依赖
//    api("org.glassfish.jersey.bundles.repackaged:jersey-guava")
    api("com.google.guava:guava")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider")
    api("com.fasterxml.jackson.jaxrs:jackson-jaxrs-base")
    api("org.bouncycastle:bcprov-ext-jdk15on")
    api("commons-collections:commons-collections")
    api("commons-lang:commons-lang:2.6")
    api("commons-codec:commons-codec:1.9")
    api("com.google.code.gson:gson:2.8.2")
    api("com.alibaba:fastjson")
    api("com.squareup.okhttp3:okhttp")
    api("org.springframework.boot:spring-boot-starter-jersey")
    api("org.springframework.boot:spring-boot-starter-undertow")
//    api("org.springframework.boot:spring-boot-starter-web")
    api(group = "org.json", name = "json", version = "20180130")
    api(group = "org.slf4j", name = "slf4j-api")
    api(group = "org.apache.poi", name = "poi")
    api(group = "org.apache.poi", name = "poi-ooxml")
    api(group = "org.apache.commons", name = "commons-exec")
}
