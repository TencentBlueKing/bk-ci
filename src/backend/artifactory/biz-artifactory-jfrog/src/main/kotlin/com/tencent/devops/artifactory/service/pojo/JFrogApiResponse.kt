package com.tencent.devops.artifactory.service.pojo

data class JFrogApiResponse<out T>(
    val status: Int,
    val data: T?,
    val message: String
)