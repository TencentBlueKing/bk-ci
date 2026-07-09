package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.enum.ProjectGroupMigrationStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目级用户组迁移结果")
data class ProjectGroupMigrationGroupResultDTO(
    @get:Schema(title = "源用户组ID")
    val sourceGroupId: Int,
    @get:Schema(title = "源用户组名")
    val sourceGroupName: String,
    @get:Schema(title = "源用户组编码")
    val sourceGroupCode: String,
    @get:Schema(title = "目标用户组ID")
    val targetGroupId: Int? = null,
    @get:Schema(title = "目标用户组名")
    val targetGroupName: String? = null,
    @get:Schema(title = "状态")
    val status: ProjectGroupMigrationStatus,
    @get:Schema(title = "项目级权限动作")
    val projectActions: List<String> = emptyList(),
    @get:Schema(title = "单资源权限数量")
    val singleResourcePermissionCount: Int = 0,
    @get:Schema(title = "迁移成员数量")
    val migratedMemberCount: Int = 0,
    @get:Schema(title = "跳过成员数量")
    val skippedMemberCount: Int = 0,
    @get:Schema(title = "明细")
    val details: List<ProjectGroupMigrationDetailDTO> = emptyList(),
    @get:Schema(title = "错误信息")
    val errors: List<String> = emptyList()
)
