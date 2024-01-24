package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文件分发的结果")
data class JobCloudFileDistributeResult(
    @ApiModelProperty(value = "作业实例ID", required = true)
    @JsonProperty("job_instance_id")
    val jobInstanceId: Long,
    @ApiModelProperty(value = "作业实例名称", required = true)
    @JsonProperty("job_instance_name")
    val jobInstanceName: String,
    @ApiModelProperty(value = "步骤实例ID", required = true)
    @JsonProperty("step_instance_id")
    val stepInstanceId: Long
) {
    constructor() : this(-1L, "null", -1L)
}