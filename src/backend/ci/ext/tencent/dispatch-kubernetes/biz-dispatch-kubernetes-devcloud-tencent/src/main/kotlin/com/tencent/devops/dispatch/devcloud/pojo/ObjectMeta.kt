package com.tencent.devops.dispatch.devcloud.pojo

data class ObjectMeta(
    val name: String? = null,
    val uid: String? = null,
    val resourceVersion: String? = null,
    val generation: Long = 0L,
    val labels: Map<String, String>,
    val annotations: Map<String, String>
)
