package com.tencent.devops.dispatch.kubernetes.pojo

data class Capabilities(
    val add: List<String>,
    val drop: List<String>
)
