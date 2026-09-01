package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.enum.ProjectGroupResourceMappingStrategy
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目级用户组增量迁移请求")
data class ProjectGroupIncrementalMigrationDTO(
    @get:Schema(title = "源项目ID")
    val sourceProjectCode: String,
    @get:Schema(title = "目标项目ID")
    val targetProjectCode: String,
    @get:Schema(title = "是否仅预演")
    val dryRun: Boolean = false,
    // 增量接口仍沿用与全量接口一致的单资源映射策略，
    // 这样资源分批迁移时，两条链路的资源重定位口径保持一致。
    @get:Schema(title = "单资源权限映射策略")
    val mappingStrategy: ProjectGroupResourceMappingStrategy =
        ProjectGroupResourceMappingStrategy.BY_RESOURCE_TYPE_AND_NAME
)
