dependencies {
    api(project(":core:common:common-api"))
    api(project(":core:process:api-process"))
}

plugins {
    `task-deploy-to-maven`
}
