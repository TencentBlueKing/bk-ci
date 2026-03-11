package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "查询任务状态的结果")
data class JobCloudQueryJobInstanceStatusResult(
    @get:Schema(title = "作业是否结束", required = true)
    val finished: Boolean,
    @get:Schema(title = "作业实例基本信息")
    @JsonProperty("job_instance")
    val jobCloudJobInstance: JobCloudJobInstance?,
    @get:Schema(title = "作业步骤列表")
    @JsonProperty("step_instance_list")
    val stepInstanceList: List<JobCloudJobStepInstance>?
) {
    constructor() : this(false, null, null)
}