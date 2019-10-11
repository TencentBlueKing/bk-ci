package com.tencent.devops.process.pojo.pipeline

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线最新构建详情")
data class PipelineLatestBuild(
    @ApiModelProperty("ID", required = true)
    val buildId: String,
    @ApiModelProperty("启动用户", required = true)
    val startUser: String,
    @ApiModelProperty("Start time", required = true)
    val startTime: String,
    @ApiModelProperty("End time", required = false)
    val endTime: String?,
    @ApiModelProperty("Build status", required = false)
    val status: String?
)