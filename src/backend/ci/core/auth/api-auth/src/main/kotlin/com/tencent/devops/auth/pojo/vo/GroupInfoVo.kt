package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "用户组列表返回")
data class GroupInfoVo(
    @Schema(description = "用户组ID")
    val id: Int,
    @Schema(description = "用户组名称")
    val name: String,
    @Schema(description = "用户组别名")
    val displayName: String,
    @Schema(description = "用户组Code")
    val code: String,
    @Schema(description = "是否为默认分组")
    val defaultRole: Boolean,
    @Schema(description = "用户组人数")
    val userCount: Int,
    @Schema(description = "用户组部门数")
    val departmentCount: Int = 0
)
