package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.enum.ProjectGroupMigrationStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目级用户组迁移单项明细")
data class ProjectGroupMigrationDetailDTO(
    @get:Schema(title = "状态")
    val status: ProjectGroupMigrationStatus,
    @get:Schema(title = "动作")
    val action: String? = null,
    @get:Schema(title = "资源类型")
    val resourceType: String? = null,
    @get:Schema(title = "源资源ID")
    val sourceResourceCode: String? = null,
    @get:Schema(title = "源资源名称")
    val sourceResourceName: String? = null,
    @get:Schema(title = "目标资源ID")
    val targetResourceCode: String? = null,
    @get:Schema(title = "目标资源名称")
    val targetResourceName: String? = null,
    @get:Schema(title = "成员ID")
    val memberId: String? = null,
    @get:Schema(title = "成员类型")
    val memberType: String? = null,
    @get:Schema(title = "失败原因")
    val reason: String? = null
)
