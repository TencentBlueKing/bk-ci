package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限诊断结果")
data class PermissionDiagnoseVO(
    @get:Schema(title = "是否有权限")
    val hasPermission: Boolean,
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "资源Code")
    val resourceCode: String,
    @get:Schema(title = "资源名称")
    val resourceName: String,
    @get:Schema(title = "操作类型")
    val action: String,
    @get:Schema(title = "操作名称")
    val actionName: String,
    @get:Schema(title = "缺失原因")
    val missingReason: String? = null,
    @get:Schema(title = "可申请的用户组列表")
    val applicableGroups: List<ApplicableGroupVO> = emptyList(),
    @get:Schema(title = "用户组管理员（可联系申请）")
    val groupManagers: List<String> = emptyList(),
    @get:Schema(title = "建议操作")
    val suggestion: String? = null
)

@Schema(title = "可申请的用户组")
data class ApplicableGroupVO(
    @get:Schema(title = "用户组ID")
    val groupId: Int,
    @get:Schema(title = "用户组名称")
    val groupName: String,
    @get:Schema(title = "管理层级")
    val managementLevel: String,
    @get:Schema(title = "管理范围")
    val managementScope: String,
    @get:Schema(title = "包含的权限列表")
    val permissions: List<String> = emptyList(),
    @get:Schema(title = "推荐标签")
    val tags: List<PermissionTagVO> = emptyList()
)
