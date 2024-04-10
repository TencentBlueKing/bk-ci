package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class VariableServer(
    @get:Schema(title = "引用的全局变量名称")
    val variable: String?,
    @get:Schema(title = "主机列表")
    val hostList: List<HostIpv6>?,
    @get:Schema(title = "拓扑节点列表")
    val topoNodeList: List<TopoNode>?,
    @get:Schema(title = "动态分组列表")
    val dynamicGroupList: List<DynamicGroup>?
)