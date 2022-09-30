package com.tencent.devops.common.webhook.service.code.pojo

import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent

data class ThirdFilterBody(
    private val projectId: String,
    private val pipelineId: String,
    val event: CodeWebhookEvent,
    val changeFiles: Set<String>? = emptySet()
)
