plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-api"))
    api(project(":core:common:common-redis"))
    api(project(":core:common:common-util"))
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("io.micrometer:micrometer-registry-prometheus")
    api("org.springframework.boot:spring-boot-starter-log4j2")
    api("org.springframework.cloud:spring-cloud-commons")
    api("io.github.openfeign:feign-okhttp")
    api("org.jolokia:jolokia-core")
    api("org.projectlombok:lombok")

}
