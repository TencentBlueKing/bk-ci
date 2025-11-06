package com.tencent.devops.remotedev.pojo.bkrepo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * BkRepo临时Token创建请求
 */
@Schema(title = "BkRepo临时Token创建请求")
data class TemporaryTokenCreateRequest(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "仓库名称", required = true)
    val repoName: String,
    @get:Schema(title = "文件路径集合", required = true)
    val fullPathSet: List<String>,
    @get:Schema(title = "过期时间（秒）", required = true)
    val expireSeconds: Int,
    @get:Schema(title = "Token类型（UPLOAD, DOWNLOAD, ALL）", required = true)
    val type: String
)
