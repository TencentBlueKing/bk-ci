dependencies {
    api(project(":core:common:common-auth-api"))
    api(group = "com.tencent.bk.devops.ci.process", name = "api-process", version = Versions.devopsVersion){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.common", name = "common-auth-v3", version = Versions.devopsVersion){
        isChanging=true
    }
}
