package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class Capabilities(
    val add: List<String>,
    val drop: List<String>
)
