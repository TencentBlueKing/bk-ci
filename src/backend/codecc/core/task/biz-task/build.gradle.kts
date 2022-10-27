dependencies {
    api(project(":core:common:common-service"))
    api(project(":core:common:common-web"))
    api(project(":core:common:common-client"))
    api(project(":core:common:common-util"))
    api(project(":core:common:common-db"))
    api(project(":core:common:common-auth:common-auth-api"))
    api(project(":core:common:common-redis"))
    api(project(":core:task:model-task"))
    api(project(":core:task:api-task"))
    api(project(":core:defect:api-defect"))
    api(project(":core:quartz:api-quartz"))
    api("org.json:json:20180130")
    api("org.redisson:redisson")
    api(group = "com.tencent.bk.devops.ci.image", name = "api-image"){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.project", name = "api-project"){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.process", name = "api-process"){
        isChanging=true
    }
}
