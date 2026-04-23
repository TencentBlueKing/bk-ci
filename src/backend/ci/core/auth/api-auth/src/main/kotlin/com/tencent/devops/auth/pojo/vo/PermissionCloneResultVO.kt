package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限克隆结果")
data class PermissionCloneResultVO(
    @get:Schema(title = "来源用户ID")
    val sourceUserId: String,
    @get:Schema(title = "目标用户ID")
    val targetUserId: String,
    @get:Schema(title = "是否预检查模式")
    val dryRun: Boolean,
    @get:Schema(title = "将要克隆的用户组列表")
    val groupsToClone: List<GroupCloneInfoVO> = emptyList(),
    @get:Schema(title = "已跳过的用户组（目标用户已有）")
    val skippedGroups: List<GroupCloneInfoVO> = emptyList(),
    @get:Schema(title = "克隆成功的用户组数量")
    val successCount: Int = 0,
    @get:Schema(title = "克隆失败的用户组数量")
    val failedCount: Int = 0,
    @get:Schema(title = "失败详情")
    val failedDetails: List<CloneFailedDetailVO> = emptyList(),
    @get:Schema(title = "执行摘要")
    val summary: String? = null
)

@Schema(title = "用户组克隆信息")
data class GroupCloneInfoVO(
    @get:Schema(title = "用户组ID")
    val groupId: Int,
    @get:Schema(title = "用户组名称")
    val groupName: String,
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "资源名称")
    val resourceName: String,
    @get:Schema(title = "包含的权限")
    val permissions: List<String> = emptyList()
)

@Schema(title = "克隆失败详情")
data class CloneFailedDetailVO(
    @get:Schema(title = "用户组ID")
    val groupId: Int,
    @get:Schema(title = "用户组名称")
    val groupName: String,
    @get:Schema(title = "失败原因")
    val reason: String
)
