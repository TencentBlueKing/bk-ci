package com.tencent.devops.dispatch.devcloud.pojo

data class DevCloudContainerResponse(
    val code: String,
    val message: ContainerType,
    val data: DevCloudContainerResponseData
)

data class DevCloudContainerResponseData(
    val taskId: String
)
