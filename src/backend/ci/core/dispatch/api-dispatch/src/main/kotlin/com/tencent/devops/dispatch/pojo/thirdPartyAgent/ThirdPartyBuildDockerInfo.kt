package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.common.pipeline.type.agent.Credential
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoDispatch

// 用来下发给agent的docker信息，用来处理一些调度时和下发时的差异数据
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ThirdPartyBuildDockerInfo(
    val agentId: String,
    val secretKey: String,
    val image: String,
    val credential: ThirdPartyBuildDockerInfoCredential?
) {
    constructor(input: ThirdPartyAgentDockerInfoDispatch) : this(
        agentId = input.agentId,
        secretKey = input.secretKey,
        image = input.image,
        credential = ThirdPartyBuildDockerInfoCredential(input.credential)
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ThirdPartyBuildDockerInfoCredential(
    var user: String?,
    var password: String?,
    // 获取凭据失败的错误信息用来给agent上报使用
    var errMsg: String?
) {
    constructor(input: Credential?) : this(
        user = input?.user,
        password = input?.password,
        errMsg = null
    )
}
