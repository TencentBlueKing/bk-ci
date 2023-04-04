package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户组列表返回")
data class IamGroupInfoVo(
    @ApiModelProperty("分级管理员或二级管理员ID")
    val managerId: Int,
    @ApiModelProperty("是否是默认组")
    val defaultGroup: Boolean,
    @ApiModelProperty("用户组ID")
    val groupId: Int,
    @ApiModelProperty("用户组名称")
    val name: String,
    @ApiModelProperty("用户组别名")
    val displayName: String,
    @ApiModelProperty("用户组人数")
    val userCount: Int,
    @ApiModelProperty("用户组部门数")
    val departmentCount: Int = 0
)
