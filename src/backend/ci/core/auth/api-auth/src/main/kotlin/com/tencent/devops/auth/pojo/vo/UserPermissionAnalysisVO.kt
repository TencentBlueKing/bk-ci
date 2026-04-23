package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户权限分析报告")
data class UserPermissionAnalysisVO(
    @get:Schema(title = "用户角色标识", required = true)
    val role: String,
    @get:Schema(title = "角色显示名称", required = true)
    val roleDisplayName: String,
    @get:Schema(title = "加入的用户组总数", required = true)
    val totalGroupCount: Long,
    @get:Schema(title = "已过期的用户组数量", required = true)
    val expiredGroupCount: Long,
    @get:Schema(title = "各资源类型权限概况", required = true)
    val resourceSummary: List<ResourceSummaryVO>,
    @get:Schema(title = "授权概况（按资源类型统计）")
    val authorizationSummary: List<AuthorizationSummaryVO> = emptyList(),
    @get:Schema(title = "授权总数")
    val totalAuthorizationCount: Long = 0,
    @get:Schema(title = "是否拥有项目全部权限")
    val hasAllPermissions: Boolean = false,
    @get:Schema(title = "告警提示列表")
    val warnings: List<String> = emptyList()
)
