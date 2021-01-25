package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModelProperty

class WeTestAtomHistory(
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
    @ApiModelProperty("启动用户")
    val startUserId: String
)
