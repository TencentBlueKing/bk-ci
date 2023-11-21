package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentUnShare(
    @JsonProperty("receivers")
    val receivers: List<String>,
    @JsonProperty("resourceId")
    val resourceId: String,
    @JsonProperty("unsharer")
    val unSharer: String
)
