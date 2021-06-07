package com.tencent.devops.common.api.pojo

data class Response<T>(
    val code: String,
    val message: String,
    val data: T?
)
