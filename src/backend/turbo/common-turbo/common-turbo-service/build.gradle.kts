dependencies {
    api(project(":common-turbo:common-turbo-api"))
    api(project(":common-turbo:common-turbo-redis"))
    api("io.github.openfeign:feign-jackson")
    api("io.github.openfeign:feign-jaxrs")
    api("com.tencent.devops:devops-boot-starter-service")
    api("org.springframework.cloud:spring-cloud-starter-consul-discovery")
    api("org.springframework.cloud:spring-cloud-starter-consul-config")
    api("com.github.ben-manes.caffeine:caffeine")
    api("io.micrometer:micrometer-registry-prometheus")
    api("org.springframework.boot:spring-boot-starter-aop")
}
