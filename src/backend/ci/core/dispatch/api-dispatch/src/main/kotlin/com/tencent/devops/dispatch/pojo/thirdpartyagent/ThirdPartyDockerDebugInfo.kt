package com.tencent.devops.dispatch.pojo.thirdpartyagent

import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "第三方构建Docker登录调试信息")
data class ThirdPartyDockerDebugInfo(
    @get:Schema(title = "项目id")
    val projectId: String,
    @get:Schema(title = "构建id")
    val buildId: String,
    @get:Schema(title = "构建机编排序号")
    val vmSeqId: String,
    @get:Schema(title = "工作空间")
    val workspace: String,
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "调试用户")
    val debugUserId: String,
    @get:Schema(title = "debugId")
    val debugId: Long,
    val image: String,
    val credential: ThirdPartyBuildDockerInfoCredential?,
    val options: DockerOptions?
)
