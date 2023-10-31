package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("查询任务状态的结果")
data class JobCloudQueryJobInstanceStatusResult(
    @ApiModelProperty(value = "作业是否结束", required = true)
    val finished: Boolean,
    @ApiModelProperty(value = "作业实例基本信息")
    @JsonProperty("job_instance")
    val jobCloudJobInstance: JobCloudJobInstance?,
    @ApiModelProperty(value = "作业步骤列表")
    @JsonProperty("step_instance_list")
    val stepInstanceList: List<JobCloudJobStepInstance>?
) {
    constructor() : this(false, null, null)
}