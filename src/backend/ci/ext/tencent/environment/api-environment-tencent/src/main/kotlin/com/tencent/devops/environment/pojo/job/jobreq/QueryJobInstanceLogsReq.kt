package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量查询日志的请求信息")
data class QueryJobInstanceLogsReq(
    @get:Schema(title = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @get:Schema(title = "步骤实例ID", required = true)
    val stepInstanceId: Long,
    @get:Schema(title = "主机ID列表")
    val hostIdList: List<Long>?,
    @get:Schema(title = "源/目标主机IP信息列表")
    val ipList: List<IpInfo>?
)