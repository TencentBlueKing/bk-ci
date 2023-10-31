package com.tencent.devops.environment.pojo.job.req

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("批量查询日志的请求信息")
data class QueryJobInstanceLogsReq(
    @ApiModelProperty(value = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @ApiModelProperty(value = "步骤实例ID", required = true)
    val stepInstanceId: Long,
    @ApiModelProperty(value = "源/目标主机列表")
    val hostList: List<Host>?
)