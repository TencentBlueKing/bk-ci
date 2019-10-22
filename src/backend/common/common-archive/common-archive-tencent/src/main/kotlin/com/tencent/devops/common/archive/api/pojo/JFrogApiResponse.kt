package com.tencent.devops.common.archive.api.pojo

data class JFrogApiResponse<out T>(
    val status: Int,
    val data: T?,
    val message: String
)