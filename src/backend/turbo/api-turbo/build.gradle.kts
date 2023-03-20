dependencies {
    api(project(":common-turbo:common-turbo-api"))
    api(project(":common-turbo:common-turbo-util"))
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.cloud:spring-cloud-openfeign-core")
    api("com.tencent.bk.devops.ci.common:common-event:${Versions.ciVersion}")
    api("com.tencent.bk.devops.ci.common:common-api:${Versions.ciVersion}")
}

plugins {
    `task-deploy-to-maven`
}
