package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户搜索结果")
data class UserSearchResultVO(
    @get:Schema(title = "搜索到的用户列表")
    val users: List<UserInfoVO> = emptyList(),
    @get:Schema(title = "总数")
    val totalCount: Int = 0,
    @get:Schema(title = "是否精确匹配")
    val exactMatch: Boolean = false,
    @get:Schema(title = "搜索关键词")
    val keyword: String
)

@Schema(title = "用户信息")
data class UserInfoVO(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "显示名称")
    val displayName: String,
    @get:Schema(title = "是否是项目成员")
    val isProjectMember: Boolean = false,
    @get:Schema(title = "邮箱")
    val email: String? = null,
    @get:Schema(title = "部门")
    val department: String? = null
)
