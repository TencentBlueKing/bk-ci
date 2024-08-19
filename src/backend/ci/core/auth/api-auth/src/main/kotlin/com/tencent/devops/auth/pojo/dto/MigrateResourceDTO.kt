package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "迁移资源请求实体")
data class MigrateResourceDTO(
    @get:Schema(title = "资源类型")
    val resourceType: String? = null,
    @get:Schema(title = "项目ID列表")
    val projectCodes: List<String>? = null,
    @get:Schema(title = "是否包含router_tag为null的项目")
    val includeNullRouterTag: Boolean? = false,
    @get:Schema(title = "是否迁移项目级资源")
    val migrateProjectResource: Boolean? = false,
    @get:Schema(title = "是否迁移项目级默认用户组")
    val migrateProjectDefaultGroup: Boolean? = false,
    @get:Schema(title = "是否迁移其他资源类型的资源")
    val migrateOtherResource: Boolean? = false
)
