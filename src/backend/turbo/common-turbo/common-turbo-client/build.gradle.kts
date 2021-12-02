dependencies {
    api(project(":common-turbo:common-turbo-api"))
    api(project(":common-turbo:common-turbo-service"))
    api(project(":common-turbo:common-turbo-util"))
    api("org.springframework.cloud:spring-cloud-starter-consul-discovery")
    api("org.springframework.cloud:spring-cloud-starter-openfeign")
    api("io.github.openfeign:feign-jackson")
    api("io.github.openfeign:feign-jaxrs")
    api("io.github.openfeign:feign-okhttp")
    api("io.github.openfeign.form:feign-form")
    api("io.github.openfeign.form:feign-form-spring")
}
