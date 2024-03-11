package com.tencent.devops.repository.sdk.tapd.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class ShardIdMap(
    val type: String,
    // 短ID
    @JsonProperty("short_id")
    val shortId: String,
    // 项目ID
    @JsonProperty("workspace_id")
    val workspaceId: String,
    @JsonProperty("long_id")
    val longId: String
)
