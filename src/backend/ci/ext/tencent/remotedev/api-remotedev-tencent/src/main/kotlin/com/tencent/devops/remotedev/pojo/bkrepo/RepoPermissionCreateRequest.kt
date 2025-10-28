package com.tencent.devops.remotedev.pojo.bkrepo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * BkRepo仓库权限创建请求
 */
@Schema(title = "BkRepo仓库权限创建请求")
data class RepoPermissionCreateRequest(
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "权限名称", required = true)
    val permName: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "仓库列表", required = true)
    val repos: List<String>,
    @get:Schema(title = "包含路径模式", required = true)
    val includePattern: List<String>,
    @get:Schema(title = "用户列表", required = true)
    val users: List<String>,
    @get:Schema(title = "操作权限列表", required = true)
    val actions: List<String>,
    @get:Schema(title = "创建人", required = true)
    val createBy: String,
    @get:Schema(title = "更新人", required = true)
    val updatedBy: String
)
