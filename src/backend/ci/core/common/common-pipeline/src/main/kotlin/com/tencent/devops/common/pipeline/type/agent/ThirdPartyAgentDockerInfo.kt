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
    val password: String
)

// 第三方构建机docker类型，调度使用，会带有调度相关信息
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ThirdPartyAgentDockerInfoDispatch(
    // docker类型构建机需要，调度
    val agentId: String,
    val secretKey: String,
    val image: String,
    val credential: Credential?,
    val envs: Map<String, String>?
) {
    constructor(agentId: String, secretKey: String, info: ThirdPartyAgentDockerInfo) : this(
        agentId, secretKey, info.image, info.credential, info.envs
    )
}
