package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class GetStepInstanceDetailResult(
    @get:Schema(title = "作业步骤ID")
    val id: Long,
    @get:Schema(title = "步骤类型：1-脚本，2-文件，3-人工确认")
    val type: Int,
    @get:Schema(title = "name")
    val name: String,
    @get:Schema(title = "脚本步骤信息")
    val scriptStepInfo: ScriptStepInfo?,
    @get:Schema(title = "文件步骤信息")
    val fileStepInfo: FileStepInfo?,
    @get:Schema(title = "审批步骤信息")
    val approvalStepInfo: ApprovalStepInfo?
)