package com.tencent.devops.common.sdk.tapd

data class TapdResult<T>(
    val status: Int,
    val info: String,
    val data: T?
)
