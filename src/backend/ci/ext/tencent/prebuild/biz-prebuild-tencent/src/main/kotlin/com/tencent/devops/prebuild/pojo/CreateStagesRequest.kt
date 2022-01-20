package com.tencent.devops.prebuild.pojo

import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo

/**
 * 生成流水线Stages请求Model
 */
data class CreateStagesRequest(
    val userId: String,
    val startUpReq: StartUpReq,
    val scriptBuildYaml: ScriptBuildYaml,
    val agentInfo: ThirdPartyAgentStaticInfo,
    val channelCode: ChannelCode
)