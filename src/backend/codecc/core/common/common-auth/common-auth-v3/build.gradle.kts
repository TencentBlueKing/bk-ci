dependencies {
    api(project(":core:common:common-auth:common-auth-api"))
    api(group = "com.tencent.bk.devops.ci.process", name = "api-process"){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.common", name = "common-auth-v3"){
        isChanging=true
    }
}
