package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "第三方构建Docker登录调试信息")
data class ThirdPartyDockerDebugInfo(
    @Schema(title = "项目id")
    val projectId: String,
    @Schema(title = "构建id")
    val buildId: String,
    @Schema(title = "构建机编排序号")
    val vmSeqId: String,
    @Schema(title = "工作空间")
    val workspace: String,
    @Schema(title = "流水线ID")
    val pipelineId: String,
    @Schema(title = "调试用户")
    val debugUserId: String,
    @Schema(title = "debugId")
    val debugId: Long,
    val image: String,
    val credential: ThirdPartyBuildDockerInfoCredential?,
    val options: DockerOptions?
)
