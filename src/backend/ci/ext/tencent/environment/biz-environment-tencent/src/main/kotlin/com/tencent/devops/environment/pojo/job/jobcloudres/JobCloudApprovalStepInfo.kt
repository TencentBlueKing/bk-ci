package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudApprovalStepInfo(
    @get:Schema(title = "确认消息")
    @JsonProperty("approval_message")
    val approvalMessage: String
)