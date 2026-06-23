package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.enum.ProjectGroupMigrationStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目级用户组迁移结果")
data class ProjectGroupMigrationResultDTO(
    @get:Schema(title = "源项目ID")
    val sourceProjectCode: String,
    @get:Schema(title = "目标项目ID")
    val targetProjectCode: String,
    @get:Schema(title = "是否仅预演")
    val dryRun: Boolean,
    @get:Schema(title = "状态")
    val status: ProjectGroupMigrationStatus,
    @get:Schema(title = "受保护的目标用户组")
    val protectedTargetGroups: List<String> = emptyList(),
    @get:Schema(title = "计划或已删除的目标用户组")
    val deletedTargetGroups: List<String> = emptyList(),
    @get:Schema(title = "阻塞删除的目标用户组")
    val blockedTargetGroups: List<String> = emptyList(),
    @get:Schema(title = "迁移用户组结果")
    val groupResults: List<ProjectGroupMigrationGroupResultDTO> = emptyList(),
    @get:Schema(title = "错误信息")
    val errors: List<String> = emptyList()
)
