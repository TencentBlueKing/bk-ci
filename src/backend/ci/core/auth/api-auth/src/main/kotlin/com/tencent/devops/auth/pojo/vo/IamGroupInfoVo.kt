package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组列表返回")
data class IamGroupInfoVo(
    @get:Schema(title = "分级管理员或二级管理员ID")
    val managerId: Int,
    @get:Schema(title = "是否是默认组")
    val defaultGroup: Boolean,
    @get:Schema(title = "用户组ID")
    val groupId: Int,
    @get:Schema(title = "用户组名称")
    val name: String,
    @get:Schema(title = "用户组别名")
    val displayName: String,
    @get:Schema(title = "用户组人数")
    val userCount: Int,
    @get:Schema(title = "用户组部门数")
    val departmentCount: Int = 0,
    @get:Schema(title = "用户组模板数")
    val templateCount: Int? = 0,
    @get:Schema(title = "是否为项目成员组")
    val projectMemberGroup: Boolean? = null
)
