plugins {
    id("com.tencent.devops.boot")
}

dependencies {
    api(project(":core:defect:biz-defect"))
    api(group = "com.tencent.bk.devops.ci.log", name = "api-log"){
        isChanging = true
    }
    api(group = "com.tencent.bk.devops.ci.quality", name = "api-quality"){
        isChanging = true
    }
    api(group = "com.tencent.bk.devops.ci.process", name = "api-process"){
        isChanging = true
    }
    api(group= "com.tencent.bk.devops.ci.repository", name = "api-repository"){
        isChanging = true
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name="api-plugin"){
        isChanging = true
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name="api-codecc"){
        isChanging = true
    }
}

