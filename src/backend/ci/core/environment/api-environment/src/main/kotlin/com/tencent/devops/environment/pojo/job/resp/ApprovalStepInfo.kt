package com.tencent.devops.environment.pojo.job.resp

import io.swagger.v3.oas.annotations.media.Schema

data class ApprovalStepInfo(
    @get:Schema(title = "确认消息")
    val approvalMessage: String
)