package com.tencent.devops.dispatch.startCloud.pojo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnvironmentUnShare(
    @JsonProperty("receivers")
    val receivers: List<String>,
    @JsonProperty("resourceId")
    val resourceId: String,
    @JsonProperty("unsharer")
    val unSharer: String
)
