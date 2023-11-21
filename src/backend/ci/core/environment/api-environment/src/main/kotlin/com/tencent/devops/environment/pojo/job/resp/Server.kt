package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class Server(
    @ApiModelProperty(value = "脚本ID")
    val hostList: List<HostIpv6>,
    @ApiModelProperty(value = "脚本ID")
    val topoNodeList: List<TopoNode>,
    @ApiModelProperty(value = "脚本ID")
    val dynamicGroupList: List<DynamicGroup>
)