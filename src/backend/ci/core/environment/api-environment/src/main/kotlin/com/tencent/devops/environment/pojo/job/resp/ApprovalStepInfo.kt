package com.tencent.devops.environment.pojo.job.resp

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class ApprovalStepInfo(
    @ApiModelProperty(value = "确认消息")
    val approvalMessage: String
)