plugins {
    id("com.tencent.devops.boot")
}


dependencies {
    api(project(":core:task:biz-task"))
    api(group = "org.apache.lucene", name = "lucene-core", version = "8.6.0")
    api(group = "com.tencent.bk.devops.ci.process", name = "api-process", version = Versions.devopsVersion){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.repository", name = "api-repository", version = Versions.devopsVersion){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.project", name = "api-project", version = Versions.devopsVersion){
        isChanging=true
        exclude(group="com.tencent.bk.devops.ci.project", module = "model-project")
        exclude(group="com.tencent.bk.devops.ci.common", module = "common-api")
        exclude(group="com.tencent.bk.devops.ci.common", module = "common-auth")
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name = "api-plugin", version = Versions.devopsVersion){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.notify", name = "api-notify", version = Versions.devopsVersion){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name = "common-codecc", version = Versions.devopsVersion){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.plugin", name = "api-codecc", version = Versions.devopsVersion){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.project", name = "api-project", version = Versions.devopsVersion){
        isChanging=true
    }
    api(group = "com.tencent.bk.devops.ci.common", name = "common-pipeline", version = Versions.devopsVersion)
    api(group = "com.vdurmont", name = "emoji-java",version = "5.1.1")
    api(group = "org.apache.commons", name = "commons-csv",version = "1.9.0")
}
