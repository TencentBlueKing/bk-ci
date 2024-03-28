package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组列表返回")
data class GroupInfoVo(
    @get:Schema(title = "用户组ID")
    val id: Int,
    @get:Schema(title = "用户组名称")
    val name: String,
    @get:Schema(title = "用户组别名")
    val displayName: String,
    @get:Schema(title = "用户组Code")
    val code: String,
    @get:Schema(title = "是否为默认分组")
    val defaultRole: Boolean,
    @get:Schema(title = "用户组人数")
    val userCount: Int,
    @get:Schema(title = "用户组部门数")
    val departmentCount: Int = 0
)
