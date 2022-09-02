package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class PullRequestLabel(
    val id: Long,
    @JsonProperty("node_id")
    val nodeId: String,
    val url: String,
    val name: String,
    val description: String,
    val color: String,
    val default: Boolean
)
