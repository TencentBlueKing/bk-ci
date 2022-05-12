plugins {
    id("com.tencent.devops.boot")
}
dependencies {
    api(project(":core:common:common-util"))
    api("javax.ws.rs:javax.ws.rs-api")
    api("io.swagger:swagger-annotations") {
        exclude(group = "org.json", module = "json")
    }
    api("org.hashids:hashids")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider")
    api("com.fasterxml.jackson.jaxrs:jackson-jaxrs-base")
    api("org.bouncycastle:bcprov-ext-jdk15on")
    api("com.squareup.okhttp3:okhttp")
    api("commons-codec:commons-codec:1.9")
    api("org.projectlombok:lombok")
    api("org.glassfish.jersey.media:jersey-media-multipart")
    api(group = "com.vdurmont", name = "emoji-java",version = "5.1.1")
    api(group = "org.apache.commons", name = "commons-csv",version = "1.9.0")
    api(group = "org.apache.lucene", name = "lucene-core", version = "8.6.0")
    api(group = "com.perforce", name = "p4java", version = "2021.1.2163843")
    api("com.github.taptap:pinyin-plus:1.0")
}

