package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限对比结果")
data class PermissionCompareVO(
    @get:Schema(title = "用户A的ID")
    val userIdA: String,
    @get:Schema(title = "用户A的显示名")
    val userNameA: String,
    @get:Schema(title = "用户B的ID")
    val userIdB: String,
    @get:Schema(title = "用户B的显示名")
    val userNameB: String,
    @get:Schema(title = "共同拥有的用户组")
    val commonGroups: List<CompareGroupVO> = emptyList(),
    @get:Schema(title = "仅A拥有的用户组")
    val onlyInA: List<CompareGroupVO> = emptyList(),
    @get:Schema(title = "仅B拥有的用户组")
    val onlyInB: List<CompareGroupVO> = emptyList(),
    @get:Schema(title = "对比摘要")
    val summary: PermissionCompareSummaryVO
)

@Schema(title = "对比用户组信息")
data class CompareGroupVO(
    @get:Schema(title = "用户组ID")
    val groupId: Int,
    @get:Schema(title = "用户组名称")
    val groupName: String,
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "资源类型名称")
    val resourceTypeName: String,
    @get:Schema(title = "资源名称")
    val resourceName: String,
    @get:Schema(title = "包含的权限")
    val permissions: List<String> = emptyList()
)

@Schema(title = "权限对比摘要")
data class PermissionCompareSummaryVO(
    @get:Schema(title = "A的用户组总数")
    val totalGroupsA: Int,
    @get:Schema(title = "B的用户组总数")
    val totalGroupsB: Int,
    @get:Schema(title = "共同用户组数量")
    val commonCount: Int,
    @get:Schema(title = "仅A拥有的数量")
    val onlyInACount: Int,
    @get:Schema(title = "仅B拥有的数量")
    val onlyInBCount: Int,
    @get:Schema(title = "差异描述")
    val differenceDescription: String
)
