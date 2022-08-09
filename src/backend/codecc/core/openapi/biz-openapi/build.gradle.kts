plugins {
    id("com.tencent.devops.boot")
}
dependencies {
    api(project(":core:openapi:api-openapi"))
    api(project(":core:openapi:model-openapi"))
    api(project(":core:common:common-client"))
    api(group = "com.tencent.bk.devops.ci.project", name="api-project"){
        isChanging=true
        exclude(group="com.tencent.bk.devops.ci.project", module = "model-project")
        exclude(group="com.tencent.bk.devops.ci.common", module = "common-api")
        exclude(group="com.tencent.bk.devops.ci.common", module = "common-auth")
    }
    api("io.jsonwebtoken:jjwt")
    api(group = "net.sf.json-lib", name = "json-lib", classifier="jdk15")
}
