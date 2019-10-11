package com.tencent.devops.plugin.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("weTest邮件组")
data class WetestEmailGroup(
    @ApiModelProperty("ID")
    val id: Int,
    @ApiModelProperty("项目Id")
    val projectId: String,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("内部人员")
    val userInternal: String,
    @ApiModelProperty("外部QQ")
    val qqExternal: String?,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("创建时间")
    val createTime: String,
    @ApiModelProperty("修改时间")
    val updateTime: String,
    @ApiModelProperty("wetest邮件组ID")
    val wetestGroupId: String?,
    @ApiModelProperty("wetest邮件组名称")
    val wetestGroupName: String?
)
