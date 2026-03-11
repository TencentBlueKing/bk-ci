package com.tencent.devops.remotedev.pojo.bkrepo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * BkRepo仓库权限模式切换请求
 */
@Schema(title = "BkRepo仓库权限模式切换请求")
data class RepoToggleRequest(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "仓库名称", required = true)
    val repoName: String,
    @get:Schema(title = "访问控制模式", required = true)
    val accessControlMode: String
)
