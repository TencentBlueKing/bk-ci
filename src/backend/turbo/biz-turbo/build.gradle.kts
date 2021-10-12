dependencies {
    api(project(":api-turbo"))
    api(project(":model-turbo"))
    api(project(":common-turbo:common-turbo-client"))
    api(project(":common-turbo:common-turbo-web"))
    api(project(":common-turbo:common-turbo-quartz"))
    api(project(":common-turbo:common-turbo-db"))
    api(project(":common-turbo:common-turbo-util"))
    api("org.springframework.boot:spring-boot-starter-amqp")
    api("com.github.ben-manes.caffeine:caffeine")
    api("com.google.guava:guava")
    api("com.tencent.bk.devops.ci.project:api-project:${Versions.ciVersion}"){
        exclude("com.github.fge", "json-schema-validator")
        exclude("org.hashids","hashids")
        exclude("org.apache.commons","commons-exec")
        exclude("org.apache.lucene","lucene-core")
        exclude("io.swagger","swagger-jersey2-jaxrs")
        exclude("org.hashids","hashids")
        exclude("org.hashids","hashids")
    }
}
