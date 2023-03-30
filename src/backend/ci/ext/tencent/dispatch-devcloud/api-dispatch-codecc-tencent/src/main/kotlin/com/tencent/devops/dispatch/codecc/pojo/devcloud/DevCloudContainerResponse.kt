package com.tencent.devops.dispatch.codecc.pojo.devcloud

data class DevCloudContainerResponse(
    val code: String,
    val message: ContainerType,
    val data: DevCloudContainerResponseData
)

data class DevCloudContainerResponseData(
    val taskId: String
)
