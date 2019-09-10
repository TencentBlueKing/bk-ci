package com.tencent.devops.environment.pojo.thirdPartyAgent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Agent任务")
data class AgentTask(
    @ApiModelProperty("Task状态", required = true)
    val status: String
)