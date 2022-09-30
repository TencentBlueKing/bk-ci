package com.tencent.devops.common.webhook.service.code.pojo

data class ThirdFilterBody(
    private val projectId: String,
    private val pipelineId: String,
    val event: String,
    val changeFiles: Set<String>? = emptySet()
)
