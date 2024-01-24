package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("终止任务的结果")
data class JobCloudTaskTerminateResult(
    @ApiModelProperty(value = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long,
    @ApiModelProperty(value = "作业实例名称")
    @JsonProperty("job_instance_name")
    val jobInstanceName: String?,
    @ApiModelProperty(value = "步骤实例ID")
    @JsonProperty("step_instance_id")
    val stepInstanceId: Long?
) {
    constructor() : this(-1L, null, null)
}