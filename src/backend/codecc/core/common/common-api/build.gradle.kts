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
    api("org.bouncycastle:bcprov-jdk16")
    api("com.squareup.okhttp3:okhttp")
    api("commons-codec:commons-codec:1.9")
    api("org.projectlombok:lombok")
    api("org.glassfish.jersey.media:jersey-media-multipart")
}

