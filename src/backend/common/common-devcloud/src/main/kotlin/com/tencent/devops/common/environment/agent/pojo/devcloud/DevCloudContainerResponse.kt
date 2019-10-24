package com.tencent.devops.common.environment.agent.pojo.devcloud

data class DevCloudContainerResponse(
    val code: String,
    val message: ContainerType,
    val data: DevCloudContainerResponseData
)

data class DevCloudContainerResponseData(
    val taskId: String
)