package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("执行目标信息")
data class ExecuteTarget(
    @ApiModelProperty(value = "环境hashId列表", required = true)
    val envHashIdList: List<String>,
    @ApiModelProperty(value = "节点hashId列表", required = true)
    val nodeHashIdList: List<String>,
    @ApiModelProperty(value = "主机列表", required = true)
    val hostList: List<Host>
)