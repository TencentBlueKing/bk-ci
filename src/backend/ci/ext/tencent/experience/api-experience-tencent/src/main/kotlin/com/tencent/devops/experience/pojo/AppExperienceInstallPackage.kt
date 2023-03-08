package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("体验--安装包")
data class AppExperienceInstallPackage(
    @ApiModelProperty("名称", required = true)
    val name: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("路径", required = true)
    val path: String,
    @ApiModelProperty("仓库类型", required = true)
    val artifactoryType: String,
    @ApiModelProperty("是否有跳转构件详情的权限", required = true)
    val detailPermission: Boolean,
    @ApiModelProperty("大小", required = true)
    val size: Long
)
