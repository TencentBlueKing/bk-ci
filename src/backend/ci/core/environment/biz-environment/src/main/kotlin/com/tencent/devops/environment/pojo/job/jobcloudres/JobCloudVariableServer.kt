package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudVariableServer(
    @ApiModelProperty(value = "引用的全局变量名称")
    val variable: String?,
    @ApiModelProperty(value = "主机列表")
    @JsonProperty("ip_list")
    val jobCloudHostList: List<JobCloudHostIpv6>?,
    @ApiModelProperty(value = "拓扑节点列表")
    @JsonProperty("topo_node_list")
    val jobCloudTopoNodeList: List<JobCloudTopoNode>?,
    @ApiModelProperty(value = "动态分组列表")
    @JsonProperty("dynamic_group_list")
    val jobCloudDynamicGroupList: List<JobCloudDynamicGroup>?
)