package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("BCS虚拟机参数")
data class BcsVmParam(
    @ApiModelProperty("集群ID", required = true)
    val clusterId: String,
    @ApiModelProperty("节点数量", required = true)
    val instanceCount: Int,
    @ApiModelProperty("镜像Id", required = true)
    val imageId: String,
    @ApiModelProperty("机型", required = true)
    val vmModelId: String,
    @ApiModelProperty("有效期", required = true)
    val validity: Int
)