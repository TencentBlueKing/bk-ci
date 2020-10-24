package com.tencent.devops.process.pojo.transfer

data class TransferRequest(
    val projectId: String,
    val pipelineIds: Set<String>?,
    val channelCode: String?,
    val langRuleMap: Map<String, List<String>>
)