package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目权限信息")
data class ProjectPermissionInfoVO(
    @ApiModelProperty("项目ID")
    val projectCode: String,
    @ApiModelProperty("项目名称")
    val projectName: String,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("管理员")
    val owners: List<String>,
    @ApiModelProperty("项目成员")
    val members: List<String>
)
