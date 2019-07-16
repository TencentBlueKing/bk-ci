package com.tencent.devops.environment.pojo.thirdPartyAgent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Agent活动（上下线）")
data class ThirdPartyAgentAction(
    @ApiModelProperty("Agent Hash Id", required = true)
    val agentId: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("活动", required = true)
    val action: String,
    @ApiModelProperty("活动时间", required = true)
    val actionTime: Long
)