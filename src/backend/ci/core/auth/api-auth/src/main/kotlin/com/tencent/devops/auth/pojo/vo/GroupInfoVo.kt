package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户组列表返回")
data class GroupInfoVo(
    @ApiModelProperty("用户组ID")
    val id: Int,
    @ApiModelProperty("用户组名称")
    val name: String,
    @ApiModelProperty("用户组别名")
    val displayName: String,
    @ApiModelProperty("用户组Code")
    val code: String,
    @ApiModelProperty("是否为默认分组")
    val defaultRole: Boolean,
    @ApiModelProperty("用户组人数")
    val userCount: Int,
    @ApiModelProperty("用户组部门数")
    val departmentCount: Int = 0
)
