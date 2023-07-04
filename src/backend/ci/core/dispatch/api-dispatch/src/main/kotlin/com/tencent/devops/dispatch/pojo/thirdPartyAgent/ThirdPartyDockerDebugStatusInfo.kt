package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("第三方构建Docker状态信息")
data class ThirdPartyDockerDebugStatusInfo(
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("构建id")
    val buildId: String,
    @ApiModelProperty("构建环境id")
    val vmSeqId: String,
    @ApiModelProperty("流水线id")
    val pipelineId: String,
    @ApiModelProperty("调试用户")
    val debugUserId: String
)
