package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "迁移资源请求实体")
data class MigrateResourceDTO(
    @Schema(description = "资源类型")
    val resourceType: String? = null,
    @Schema(description = "项目ID列表")
    val projectCodes: List<String>? = null,
    @Schema(description = "是否迁移项目级资源")
    val migrateProjectResource: Boolean? = false,
    @Schema(description = "是否迁移项目级默认用户组")
    val migrateProjectDefaultGroup: Boolean? = false,
    @Schema(description = "是否迁移其他资源类型的资源")
    val migrateOtherResource: Boolean? = false
)
