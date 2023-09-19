package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class JobCloudExecuteTarget(
    @ApiModelProperty(value = "动态分组ID列表")
    @JsonProperty("dynamic_group_list")
    val dynamicGroupList: List<String>?,
    @ApiModelProperty(value = "动态topo节点列表")
    @JsonProperty("topo_node_list")
    val topoNodeList: List<String>?,
    @ApiModelProperty(value = "静态IP列表")
    @JsonProperty("ip_list")
    val ipList: List<JobCloudHost>?
)