package com.tencent.devops.common.pipeline.type.agent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ThirdPartyAgentDockerInfo(
    val image: String,
    val credential: Credential?,
    val envs: Map<String, String>?
)

data class Credential(
    val user: String,
    val password: String,
)
