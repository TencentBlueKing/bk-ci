package com.tencent.devops.gitci.pojo

import com.tencent.devops.process.pojo.BuildHistory
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂历史构建模型-对应history页面")
data class GitCIBuildHistory(
    @ApiModelProperty("工蜂Event事件", required = true)
    val gitRequestEvent: GitRequestEvent,
    @ApiModelProperty("历史构建模型", required = false)
    val buildHistory: BuildHistory?
)