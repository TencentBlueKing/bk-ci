package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目--权限")
data class ProjectWithPermission(
    @ApiModelProperty("项目名称")
    val projectName: String,
    @ApiModelProperty("项目英文名称")
    val englishName: String,
    @ApiModelProperty("权限")
    val permission: Boolean,
    @ApiModelProperty("环境路由")
    val routerTag: String?
)
