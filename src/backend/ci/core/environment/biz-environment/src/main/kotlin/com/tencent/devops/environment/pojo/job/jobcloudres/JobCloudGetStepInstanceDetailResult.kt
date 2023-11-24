package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudGetStepInstanceDetailResult(
    @ApiModelProperty(value = "作业步骤ID")
    val id: Long,
    @ApiModelProperty(value = "步骤类型：1-脚本，2-文件，3-人工确认")
    val type: Int,
    @ApiModelProperty(value = "name")
    val name: String,
    @ApiModelProperty(value = "脚本步骤信息")
    val jobCloudScriptStepInfo: JobCloudScriptStepInfo?,
    @ApiModelProperty(value = "文件步骤信息")
    @JsonProperty("file_step_info")
    val jobCloudFileStepInfo: JobCloudFileStepInfo?,
    @ApiModelProperty(value = "审批步骤信息")
    @JsonProperty("approval_step_info")
    val jobCloudApprovalStepInfo: JobCloudApprovalStepInfo?
)