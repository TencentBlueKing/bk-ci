package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "终止任务的结果")
data class JobCloudTaskTerminateResult(
    @get:Schema(title = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long,
    @get:Schema(title = "作业实例名称")
    @JsonProperty("job_instance_name")
    val jobInstanceName: String?,
    @get:Schema(title = "步骤实例ID")
    @JsonProperty("step_instance_id")
    val stepInstanceId: Long?
) {
    constructor() : this(-1L, null, null)
}