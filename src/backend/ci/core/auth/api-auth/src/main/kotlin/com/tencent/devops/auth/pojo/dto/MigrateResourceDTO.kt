package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "迁移资源请求实体")
data class MigrateResourceDTO(
    @get:Schema(title = "筛选项目列表条件")
    val conditions: ProjectConditionDTO,
    @get:Schema(title = "是否迁移资源-若为true且filterResourceTypes不为空，将迁移对应资源类型的资源")
    val migrateResource: Boolean,
    @get:Schema(title = "若filterResourceTypes不为空，则本次新增的组权限，只和该资源类型有关")
    val filterResourceTypes: List<String> = emptyList(),
    @get:Schema(title = "若filterActions不为空，则本次新增的组权限，只和该操作有关")
    val filterActions: List<String> = emptyList()
)
