plugins {
    id("com.tencent.devops.boot")
}

dependencies {

    api(project(":core:common:common-service"))
    api(project(":core:common:common-web"))
    api(project(":core:common:common-client"))
    api(project(":core:common:common-db"))
    api(project(":core:codeccjob:model-codeccjob"))
    api(project(":core:codeccjob:api-codeccjob"))
    api(project(":core:defect:api-defect"))
    api(project(":core:defect:model-defect"))
    api(project(":core:common:common-auth:common-auth-api"))
    api("org.springframework.boot:spring-boot-starter-websocket")
    api(group="javax.websocket", name="javax.websocket-api", version= "1.1")
    api("io.undertow:undertow-servlet")
    api("io.undertow:undertow-websockets-jsr")
    api("org.redisson:redisson")
    api(group= "com.tencent.bk.devops.ci.project", name= "api-project") {
        isChanging = true
        exclude(group = "com.tencent.bk.devops.ci.project", module = "model-project")
        exclude(group = "com.tencent.bk.devops.ci.project", module = "common-api")
        exclude(group = "com.tencent.bk.devops.ci.project", module = "common-auth")
    }
}

