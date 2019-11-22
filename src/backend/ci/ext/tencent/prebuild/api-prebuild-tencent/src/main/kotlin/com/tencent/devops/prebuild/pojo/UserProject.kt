package com.tencent.devops.prebuild.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户项目")
data class UserProject(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("prebuild项目ID")
    val preProjectId: String,
    @ApiModelProperty("prebuild项目名称")
    val preProjectName: String,
    @ApiModelProperty("蓝盾项目代码")
    val projectCode: String,
    @ApiModelProperty("创建人")
    val creator: String?,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("英文缩写")
    val english_name: String,
    @ApiModelProperty("修改时间")
    val updated_at: String?
)
