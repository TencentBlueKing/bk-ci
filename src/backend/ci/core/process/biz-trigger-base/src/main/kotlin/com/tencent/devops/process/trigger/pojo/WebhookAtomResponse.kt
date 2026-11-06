package com.tencent.devops.process.trigger.pojo

import com.tencent.devops.process.trigger.enums.MatchStatus

data class WebhookAtomResponse(
    val matchStatus: MatchStatus,
    val outputVars: Map<String, Any> = emptyMap(),
    val failedReason: String? = null
)
