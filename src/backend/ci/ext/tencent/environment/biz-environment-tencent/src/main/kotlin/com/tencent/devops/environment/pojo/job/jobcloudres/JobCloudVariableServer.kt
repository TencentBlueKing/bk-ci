package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudVariableServer(
    @get:Schema(title = "引用的全局变量名称")
    val variable: String?,
    @get:Schema(title = "主机列表")
    @JsonProperty("ip_list")
    val jobCloudHostList: List<JobCloudHostIpv6>?,
    @get:Schema(title = "拓扑节点列表")
    @JsonProperty("topo_node_list")
    val jobCloudTopoNodeList: List<JobCloudTopoNode>?,
    @get:Schema(title = "动态分组列表")
    @JsonProperty("dynamic_group_list")
    val jobCloudDynamicGroupList: List<JobCloudDynamicGroup>?
)