plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:common:common-service"))
    api(project(":core:common:common-web"))
    api(project(":core:common:common-client"))
    api(project(":core:common:common-storage"))
    api(project(":core:common:common-redis"))
    api(project(":core:defect:model-defect"))
    api(project(":core:defect:api-defect"))
    api(project(":core:task:api-task"))
    api(project(":core:common:common-auth:common-auth-api"))
    api(project(":core:schedule:api-schedule"))
    api("org.apache.httpcomponents:httpclient")
    api("org.redisson:redisson")
    api(group = "com.tencent.bk.devops.ci.process", name = "api-process"){
        isChanging = true
    }
}
