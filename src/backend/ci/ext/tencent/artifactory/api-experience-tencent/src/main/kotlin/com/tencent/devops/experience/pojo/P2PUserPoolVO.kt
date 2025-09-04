package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "P2P用户池")
data class P2PUserPoolVO(
    @get:Schema(title = "最近使用用户", required = true)
    val recentUsers: Set<String>,
    @get:Schema(title = "已下载用户", required = true)
    val downloadedUsers: Set<String>,
    @get:Schema(title = "有权限的用户", required = true)
    val permissionUsers: Set<String>
)
