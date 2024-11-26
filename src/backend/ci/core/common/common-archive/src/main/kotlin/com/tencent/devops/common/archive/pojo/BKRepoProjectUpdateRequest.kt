package com.tencent.devops.common.archive.pojo

import io.swagger.v3.oas.annotations.media.Schema

data class BKRepoProjectUpdateRequest(
    @get:Schema(title = "显示名")
    val displayName: String? = null,
    @get:Schema(title = "描述")
    val description: String? = null,
    @get:Schema(title = "项目元数据")
    val metadata: List<ProjectMetadata> = emptyList(),
    @get:Schema(title = "项目新建仓库默认使用的存储")
    val credentialsKey: String? = null,
    @get:Schema(title = "设置项目新建仓库默认使用默认存储")
    val useDefaultCredentialsKey: Boolean? = false,
)
