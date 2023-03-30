package com.tencent.devops.dispatch.codecc.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DockerIpUpdateVO")
data class DockerIpUpdateVO(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("构建机IP")
    val dockerIp: String,
    @ApiModelProperty("构建机PORT")
    val dockerHostPort: Int,
    @ApiModelProperty("构建机是否可用")
    val enable: Boolean,
    @ApiModelProperty("是否为灰度节点")
    val grayEnv: Boolean,
    @ApiModelProperty("是否为专用机独占")
    val specialOn: Boolean,
    @ApiModelProperty("是否为专用机独占")
    val clusterName: CodeccClusterEnum
)
