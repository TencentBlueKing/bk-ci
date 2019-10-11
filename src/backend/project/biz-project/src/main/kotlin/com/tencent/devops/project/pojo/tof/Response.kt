package com.tencent.devops.project.pojo.tof

data class Response<T>(
    val code: String,
    val message: String,
    val data: T?
)
