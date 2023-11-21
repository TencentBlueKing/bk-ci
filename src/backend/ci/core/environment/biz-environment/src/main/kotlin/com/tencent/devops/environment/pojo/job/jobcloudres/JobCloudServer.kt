package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudServer(
    @ApiModelProperty(value = "脚本ID")
    @JsonProperty("host_list")
    val jobCloudHostList: List<JobCloudHostIpv6>,
    @ApiModelProperty(value = "脚本ID")
    @JsonProperty("topo_node_list")
    val jobCloudTopoNodeList: List<JobCloudTopoNode>,
    @ApiModelProperty(value = "脚本ID")
    @JsonProperty("dynamic_group_list")
    val jobCloudDynamicGroupList: List<JobCloudDynamicGroup>
)