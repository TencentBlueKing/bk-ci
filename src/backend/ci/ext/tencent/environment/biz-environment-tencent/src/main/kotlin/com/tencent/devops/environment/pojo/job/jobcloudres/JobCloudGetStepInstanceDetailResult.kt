package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudGetStepInstanceDetailResult(
    @get:Schema(title = "作业步骤ID")
    val id: Long,
    @get:Schema(title = "步骤类型：1-脚本，2-文件，3-人工确认")
    val type: Int,
    @get:Schema(title = "name")
    val name: String,
    @get:Schema(title = "脚本步骤信息")
    @JsonProperty("script_info")
    val jobCloudScriptStepInfo: JobCloudScriptStepInfo?,
    @get:Schema(title = "文件步骤信息")
    @JsonProperty("file_info")
    val jobCloudFileStepInfo: JobCloudFileStepInfo?,
    @get:Schema(title = "审批步骤信息")
    @JsonProperty("approval_info")
    val jobCloudApprovalStepInfo: JobCloudApprovalStepInfo?
)