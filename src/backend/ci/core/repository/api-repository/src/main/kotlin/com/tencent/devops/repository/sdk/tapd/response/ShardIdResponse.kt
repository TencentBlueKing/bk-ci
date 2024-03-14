package com.tencent.devops.repository.sdk.tapd.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.tapd.pojo.ShardIdMap

data class ShardIdResponse(
    @JsonProperty("ShardIdMap")
    val shardIdMap: ShardIdMap
)
