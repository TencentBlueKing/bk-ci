plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-api"))
    api(project(":core:common:common-service"))
    api(project(":core:common:common-auth:common-auth-api"))
    api("org.springframework.boot:spring-boot-starter-amqp")
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework.boot:spring-boot-starter-web")
    api(group = "net.sf.json-lib", name = "json-lib", classifier = "jdk15")
}
