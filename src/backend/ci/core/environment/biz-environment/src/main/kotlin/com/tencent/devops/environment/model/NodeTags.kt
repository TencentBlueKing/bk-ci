package com.tencent.devops.environment.model

data class NodeTags(
    val nodeId: Long,
    val tagValueId: Long,
    val tagKeyId: Long,
    val projectId: String
)
