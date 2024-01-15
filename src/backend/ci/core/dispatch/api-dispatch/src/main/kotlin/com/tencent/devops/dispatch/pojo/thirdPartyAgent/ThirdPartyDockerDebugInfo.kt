package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "第三方构建Docker登录调试信息")
data class ThirdPartyDockerDebugInfo(
    @Schema(name = "项目id")
    val projectId: String,
    @Schema(name = "构建id")
    val buildId: String,
    @Schema(name = "构建机编排序号")
    val vmSeqId: String,
    @Schema(name = "工作空间")
    val workspace: String,
    @Schema(name = "流水线ID")
    val pipelineId: String,
    @Schema(name = "调试用户")
    val debugUserId: String,
    @Schema(name = "debugId")
    val debugId: Long,
    val image: String,
    val credential: ThirdPartyBuildDockerInfoCredential?,
    val options: DockerOptions?
)
