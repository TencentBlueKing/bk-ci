package com.tencent.devops.process.pojo.third.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("wetest 回调模型")
data class WetestCallback(
    @ApiModelProperty(value = "相当于projectId", required = true)
    val productID: String,
    @ApiModelProperty(value = "wetest的jobID", required = true)
    val jobID: String,
    @ApiModelProperty(value = "buildID", required = true)
    val buildID: String,
    @ApiModelProperty(value = "wetest的taskID", required = true)
    val taskID: String,
    @ApiModelProperty(value = "wetest的sodaID，相当于pipelineId,", required = true)
    val sodaId: String,
    @ApiModelProperty(value = "result_quality", required = false)
    val resultQuality: String,
    @ApiModelProperty(value = "result_devnum", required = false)
    val resultDevNum: String,
    @ApiModelProperty(value = "result_Rate", required = false)
    val resultRate: String,
    @ApiModelProperty(value = "result_Problems", required = false)
    val resultProblems: String,
    @ApiModelProperty(value = "result_Serious", required = false)
    val resultSerious: String,
    @ApiModelProperty(value = "starttime", required = false)
    val startTime: String,
    @ApiModelProperty(value = "endtime", required = false)
    val endTime: String
)