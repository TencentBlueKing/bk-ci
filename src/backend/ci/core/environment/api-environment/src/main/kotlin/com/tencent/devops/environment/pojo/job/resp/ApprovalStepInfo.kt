package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class ApprovalStepInfo(
    @ApiModelProperty(value = "确认消息")
    val approvalMessage: String
)