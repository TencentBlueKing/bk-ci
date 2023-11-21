package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudApprovalStepInfo(
    @ApiModelProperty(value = "确认消息")
    @JsonProperty("approval_message")
    val approvalMessage: String
)