package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("BCS集群")
data class BcsCluster(
    @ApiModelProperty("集群ID", required = true)
    val clusterId: String,
    @ApiModelProperty("集群名称", required = true)
    val clusterName: String
)