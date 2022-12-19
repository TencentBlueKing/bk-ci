dependencies {
    api(project(":core:common:common-service"))
    api(project(":core:common:common-web"))
    api(project(":core:common:common-client"))
    api(project(":core:task:api-task"))
    api(project(":core:defect:api-defect"))
    api(group = "com.tencent.bk.devops.ci.process", name="api-process"){
        isChanging=true
    }
}

