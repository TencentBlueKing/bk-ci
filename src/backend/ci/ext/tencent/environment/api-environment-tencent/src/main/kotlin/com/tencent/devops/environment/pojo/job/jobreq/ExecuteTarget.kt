package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "执行目标信息")
data class ExecuteTarget(
    @get:Schema(title = "环境hashId列表")
    val envHashIdList: List<String>?,
    @get:Schema(title = "节点hashId列表")
    val nodeHashIdList: List<String>?,
    @get:Schema(title = "主机IP信息列表")
    val ipList: List<IpInfo>?,
    @get:Schema(title = "主机ID列表")
    val hostIdList: List<Long>?
)