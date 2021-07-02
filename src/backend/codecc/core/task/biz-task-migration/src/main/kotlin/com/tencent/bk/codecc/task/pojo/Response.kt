package com.tencent.bk.codecc.task.pojo

data class Response<T>(
    val code: String,
    val message: String,
    val data: T?
)
