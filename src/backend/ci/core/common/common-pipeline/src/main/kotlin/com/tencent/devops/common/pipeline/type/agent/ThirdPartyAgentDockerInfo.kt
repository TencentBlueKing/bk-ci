package com.tencent.devops.common.pipeline.type.agent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.common.api.util.EnvUtils

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ThirdPartyAgentDockerInfo(
    var image: String,
    var credential: Credential?
)

fun ThirdPartyAgentDockerInfo.replaceField(variables: Map<String, String>) {
    image = EnvUtils.parseEnv(image, variables)
    if (!credential?.user.isNullOrBlank()) {
        credential?.user = EnvUtils.parseEnv(credential?.user, variables)
    }
    if (!credential?.password.isNullOrBlank()) {
        credential?.password = EnvUtils.parseEnv(credential?.password, variables)
    }
}

data class Credential(
    var user: String?,
    var password: String?,
    var credentialId: String?,
    // 跨项目使用凭据相关信息
    val acrossTemplateId: String?
)

// 第三方构建机docker类型，调度使用，会带有调度相关信息
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ThirdPartyAgentDockerInfoDispatch(
    // docker类型构建机需要，调度
    val agentId: String,
    val secretKey: String,
    val image: String,
    val credential: Credential?
) {
    constructor(agentId: String, secretKey: String, info: ThirdPartyAgentDockerInfo) : this(
        agentId, secretKey, info.image, info.credential
    )
}
