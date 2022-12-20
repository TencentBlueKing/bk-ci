package com.tencent.devops.dispatch.devcloud.pojo

data class ObjectMeta(
    val name: String,
    val uid: String,
    val resourceVersion: String,
    val generation: Long,
    val labels: Map<String, String>,
    val annotations: Map<String, String>
)
