package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("批量查询日志的请求信息")
data class QueryLogsReq(
    @ApiModelProperty(value = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @ApiModelProperty(value = "步骤实例ID", required = true)
    private val stepInstanceId: Long,
    @ApiModelProperty(value = "源/目标主机IP列表", required = true)
    val ipList: List<IPInfo>
)