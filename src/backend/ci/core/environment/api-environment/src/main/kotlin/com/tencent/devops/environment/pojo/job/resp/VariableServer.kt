package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class VariableServer(
    @ApiModelProperty(value = "引用的全局变量名称")
    val variable: String?,
    @ApiModelProperty(value = "主机列表")
    val hostList: List<HostIpv6>?,
    @ApiModelProperty(value = "拓扑节点列表")
    val topoNodeList: List<TopoNode>?,
    @ApiModelProperty(value = "动态分组列表")
    val dynamicGroupList: List<DynamicGroup>?
)