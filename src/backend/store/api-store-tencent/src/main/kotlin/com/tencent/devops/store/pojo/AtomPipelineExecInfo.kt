package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class AtomPipelineExecInfo(
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = true)
    var pipelineName: String,
    @ApiModelProperty("项目标识", required = true)
    val projectCode: String,
    @ApiModelProperty("拥有者", required = true)
    val owner: String,
    @ApiModelProperty("最近执行时间", required = true)
    val latestExecTime: String
)