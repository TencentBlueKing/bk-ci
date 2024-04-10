package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

data class EnvironmentOperate(
    val uid: String,
    val userId: String? = null,
    val appName: String? = null,
    val pipelineId: String? = null,
    val env: Map<String, String>? = null,
    val cgsId: String? = null,
    val image: String? = null
)
