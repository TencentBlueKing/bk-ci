package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "执行脚本的结果")
data class JobCloudScriptExecuteResult(
    @get:Schema(title = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long,
    @get:Schema(title = "作业实例名称", required = true)
    @JsonProperty("job_instance_name")
    val jobInstanceName: String,
    @get:Schema(title = "步骤实例ID", required = true)
    @JsonProperty("step_instance_id")
    val stepInstanceId: Long
)