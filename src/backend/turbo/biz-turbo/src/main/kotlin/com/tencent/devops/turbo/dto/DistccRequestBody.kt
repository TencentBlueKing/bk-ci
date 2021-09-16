package com.tencent.devops.turbo.dto

data class DistccRequestBody<T>(
    val operator: String,
    val data: T?
)
