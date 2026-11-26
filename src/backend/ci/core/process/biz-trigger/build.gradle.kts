dependencies {
    api(project(":core:common:common-service"))
    api(project(":core:common:common-web"))
    api(project(":core:common:common-client"))
    api(project(":core:common:common-redis"))
    api(project(":core:process:api-process"))
    api(project(":core:process:api-trigger"))
    api(project(":core:process:biz-base"))
    api(project(":core:process:biz-trigger-base"))
    implementation("org.quartz-scheduler:quartz")
    testImplementation(project(":core:common:common-test"))
}
