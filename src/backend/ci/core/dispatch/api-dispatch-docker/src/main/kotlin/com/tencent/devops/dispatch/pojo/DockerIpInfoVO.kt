package com.tencent.devops.dispatch.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IdcIpInfoVO")
data class DockerIpInfoVO(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("IDC构建机IP")
    val idcIp: String,
    @ApiModelProperty("IDC构建机容量")
    val capacity: Int,
    @ApiModelProperty("IDC构建机已使用量")
    val usedNum: Int,
    @ApiModelProperty("IDC构建机CPU负载")
    val averageCpuLoad: Int,
    @ApiModelProperty("IDC构建机内存负载")
    val averageMemLoad: Int,
    @ApiModelProperty("IDC构建机硬盘负载")
    val averageDiskLoad: Int,
    @ApiModelProperty("IDC构建机硬盘IO负载")
    val averageDiskIOLoad: Int,
    @ApiModelProperty("IDC构建机是否可用")
    val enable: Boolean,
    @ApiModelProperty("是否为灰度节点")
    val grayEnv: Boolean,
    @ApiModelProperty("创建时间")
    val createTime: String
)