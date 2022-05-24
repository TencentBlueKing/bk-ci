plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-mongodb")
    api("org.projectlombok:lombok")
    api(project(":core:common:common-api"))
}
