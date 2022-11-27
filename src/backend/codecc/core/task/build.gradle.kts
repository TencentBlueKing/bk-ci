plugins {
    id("com.tencent.devops.boot")
}

subprojects {
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-spring")

    dependencies {
        api("org.glassfish.jersey.ext:jersey-bean-validation")
        api(group = "com.tencent.bk.devops.ci.repository", name = "api-repository"){
            isChanging = true
        }
    }
}
