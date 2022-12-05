package com.tencent.devops.common.sdk.tapd.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.tapd.pojo.ShardIdMap

data class ShardIdResponse(
    @JsonProperty("ShardIdMap")
    val shardIdMap: ShardIdMap
)
