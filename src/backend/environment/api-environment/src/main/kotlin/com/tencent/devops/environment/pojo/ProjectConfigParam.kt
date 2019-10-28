package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目配置")
data class ProjectConfigParam(
    @ApiModelProperty("项目Id", required = true)
    val projectId: String,
    @ApiModelProperty("更新人", required = true)
    val updatedUser: String,
    @ApiModelProperty("允许使用BCS虚拟机功能", required = true)
    val bcsVmEnabled: Boolean,
    @ApiModelProperty("BCS虚拟机配额", required = true)
    val bcsVmQuota: Int,
    @ApiModelProperty("导入服务器配额", required = true)
    val importQuota: Int,
    @ApiModelProperty("允许使用DevCloud功能", required = true)
    val devCloudEnable: Boolean,
    @ApiModelProperty("DevCloud配额", required = true)
    val devCloudQuota: Int
)