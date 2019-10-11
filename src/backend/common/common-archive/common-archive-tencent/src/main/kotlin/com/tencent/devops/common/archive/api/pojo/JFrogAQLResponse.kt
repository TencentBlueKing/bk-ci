package com.tencent.devops.common.archive.api.pojo

data class JFrogAQLResponse<out T>(
    val results: List<T>
)