package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.enum.ProjectGroupResourceMappingStrategy
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目级用户组迁移请求")
data class ProjectGroupMigrationDTO(
    @get:Schema(title = "源项目ID")
    val sourceProjectCode: String,
    @get:Schema(title = "目标项目ID")
    val targetProjectCode: String,
    @get:Schema(title = "是否仅预演")
    val dryRun: Boolean = false,
    @get:Schema(title = "是否迁移成员")
    val includeMembers: Boolean = true,
    @get:Schema(title = "单资源权限映射策略")
    val mappingStrategy: ProjectGroupResourceMappingStrategy =
        ProjectGroupResourceMappingStrategy.BY_RESOURCE_TYPE_AND_NAME
)
