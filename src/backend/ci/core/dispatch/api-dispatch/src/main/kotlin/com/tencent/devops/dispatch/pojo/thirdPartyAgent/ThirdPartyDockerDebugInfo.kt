package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("第三方构建Docker登录调试信息")
data class ThirdPartyDockerDebugInfo(
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("构建id")
    val buildId: String,
    @ApiModelProperty("构建机编排序号")
    val vmSeqId: String,
    @ApiModelProperty("工作空间")
    val workspace: String,
    @ApiModelProperty("流水线ID")
    val pipelineId: String,
    @ApiModelProperty("调试用户")
    val debugUserId: String,
    @ApiModelProperty("debugId")
    val debugId: Long,
    val image: String,
    val credential: ThirdPartyBuildDockerInfoCredential?,
    val options: DockerOptions?
)
