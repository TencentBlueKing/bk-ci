package com.tencent.devops.repository.sdk.tapd

data class TapdResult<T>(
    val status: Int,
    val info: String,
    val data: T?
)
