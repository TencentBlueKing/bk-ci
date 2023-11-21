package com.tencent.devops.environment.pojo.job.resp

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class GetStepInstanceDetailResult(
    @ApiModelProperty(value = "作业步骤ID")
    val id: Long,
    @ApiModelProperty(value = "步骤类型：1-脚本，2-文件，3-人工确认")
    val type: Int,
    @ApiModelProperty(value = "name")
    val name: String,
    @ApiModelProperty(value = "脚本步骤信息")
    val scriptStepInfo: ScriptStepInfo,
    @ApiModelProperty(value = "文件步骤信息")
    val fileStepInfo: FileStepInfo,
    @ApiModelProperty(value = "审批步骤信息")
    val approvalStepInfo: ApprovalStepInfo
)