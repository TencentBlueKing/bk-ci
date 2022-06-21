package com.tencent.devops.dispatch.docker.pojo

import com.tencent.devops.common.pipeline.type.docker.ImageType

data class Pool(
    val container: String?,
    val credential: Credential?,
    val env: Map<String, String>?,
    val imageType: String? = ImageType.THIRD.type,
    val buildType: BuildType? = BuildType.DEVCLOUD
)

enum class BuildType {
    DOCKER_VM,
    DEVCLOUD,
    BCS
}

data class Credential(
    val user: String,
    val password: String
)
