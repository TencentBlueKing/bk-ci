package com.tencent.devops.dispatch.docker.pojo

data class Pool(
    val container: String?,
    val credential: Credential?,
    val env: Map<String, String>?,
    val buildType: BuildType? = BuildType.DEVCLOUD
)

enum class BuildType {
    DOCKER_VM,
    DEVCLOUD
}

data class Credential(
    val user: String,
    val password: String
)
