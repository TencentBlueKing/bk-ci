package com.tencent.devops.scm.pojo.tapd

data class TapdResult<T>(
    val status: Int,
    val info: String,
    val data: T?
)
