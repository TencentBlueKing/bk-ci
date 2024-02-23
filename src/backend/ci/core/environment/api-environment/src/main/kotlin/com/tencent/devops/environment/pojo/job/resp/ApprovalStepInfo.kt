package com.tencent.devops.environment.pojo.job.resp



data class ApprovalStepInfo(
    @get:Schema(title = "确认消息")
    val approvalMessage: String
)