package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "用户组列表返回")
data class IamGroupInfoVo(
    @Schema(description = "分级管理员或二级管理员ID")
    val managerId: Int,
    @Schema(description = "是否是默认组")
    val defaultGroup: Boolean,
    @Schema(description = "用户组ID")
    val groupId: Int,
    @Schema(description = "用户组名称")
    val name: String,
    @Schema(description = "用户组别名")
    val displayName: String,
    @Schema(description = "用户组人数")
    val userCount: Int,
    @Schema(description = "用户组部门数")
    val departmentCount: Int = 0
)
