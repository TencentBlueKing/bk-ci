package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModelProperty

data class WeTestAtomRecord(
    @ApiModelProperty("id")
    val Id: Int,
    @ApiModelProperty("project id")
    val projectId: String,
    @ApiModelProperty("atom name chinese")
    val atomNameCN: String,
    @ApiModelProperty("atom name english")
    val atomNameEN: String,
    @ApiModelProperty("流水线id")
    val pipelineId: String,
    @ApiModelProperty("构建id")
    val buildId: String,
    @ApiModelProperty("testid")
    val testId: String?,
    @ApiModelProperty("启动用户")
    val startUserId: String,
    @ApiModelProperty("开始上传的时间")
    val beginUploadTime: Long?,
    @ApiModelProperty("开始提交测试的时间")
    val beginTestTime: Long?,
    @ApiModelProperty("插件执行状态")
    val result: String?,
    @ApiModelProperty("进入插件的时间")
    val enterAtomTime: Long
)
