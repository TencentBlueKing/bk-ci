package com.tencent.devops.artifactory.service.pojo

data class JFrogAQLResponse<out T>(
    val results: List<T>
)