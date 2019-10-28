package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("VM虚拟机配额")
data class ProjectInfo(
    @ApiModelProperty("允许使用BCS虚拟机功能", required = true)
    val bcsVmEnabled: Boolean,
    @ApiModelProperty("BCS虚拟机配额", required = true)
    val bcsVmQuota: Int,
    @ApiModelProperty("BCS虚拟机已用数量", required = true)
    val bcsVmUsedCount: Int,
    @ApiModelProperty("BCS虚拟机可用数量", required = true)
    val bcsVmRestCount: Int,
    @ApiModelProperty("导入服务器配额", required = true)
    val importQuota: Int,
    @ApiModelProperty("允许使用DevCloud虚拟机功能", required = true)
    val devCloudVmEnabled: Boolean,
    @ApiModelProperty("DevCloud虚拟机配额", required = true)
    val devCloudVmQuota: Int,
    @ApiModelProperty("DevCloud虚拟机已用数量", required = true)
    val devCloudVmUsedCount: Int
)