package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源权限矩阵")
data class ResourcePermissionsMatrixVO(
    @get:Schema(title = "资源名称", required = true)
    val resourceName: String,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "关联用户组列表", required = true)
    val groups: List<ResourceGroupMatrixVO>,
    @get:Schema(title = "关联用户组总数", required = true)
    val totalGroupCount: Int
)
