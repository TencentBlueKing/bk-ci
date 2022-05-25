plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:defect:biz-defect"))
    api(group = "com.tencent.bk.devops.ci.log", name = "api-log", version = Versions.devopsVersion){
        isChanging = true
    }
    api(group = "com.tencent.bk.devops.ci.quality", name = "api-quality", version = Versions.devopsVersion){
        isChanging = true
    }
    api(group = "com.tencent.bk.devops.ci.process", name = "api-process", version = Versions.devopsVersion){
        isChanging = true
    }
    api(group= "com.tencent.bk.devops.ci.repository", name = "api-repository", version = Versions.devopsVersion){
        isChanging = true
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name="api-plugin", version = Versions.devopsVersion){
        isChanging = true
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name="api-codecc", version = Versions.devopsVersion){
        isChanging = true
    }
}

