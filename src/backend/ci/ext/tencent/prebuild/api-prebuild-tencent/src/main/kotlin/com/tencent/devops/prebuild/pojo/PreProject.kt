package com.tencent.devops.prebuild.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("prebuild项目")
data class PreProject(
    @ApiModelProperty("prebuild项目ID")
    val preProjectId: String,
    @ApiModelProperty("用户项目ID")
    val projectId: String,
    @ApiModelProperty("工作空间")
    val workspace: String,
    @ApiModelProperty("account")
    val account: String,
    @ApiModelProperty("password")
    val password: String
)
