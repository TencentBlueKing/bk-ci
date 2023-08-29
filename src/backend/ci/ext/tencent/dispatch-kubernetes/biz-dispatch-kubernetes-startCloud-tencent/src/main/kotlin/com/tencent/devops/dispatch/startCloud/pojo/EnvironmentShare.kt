package com.tencent.devops.dispatch.startCloud.pojo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnvironmentShare(
    @JsonProperty("cgsId")
    val cgsId: String,
    @JsonProperty("expireTime")
    val expireTime: Int,
    @JsonProperty("receivers")
    val receivers: List<String>,
    @JsonProperty("sharer")
    val sharer: String
)
