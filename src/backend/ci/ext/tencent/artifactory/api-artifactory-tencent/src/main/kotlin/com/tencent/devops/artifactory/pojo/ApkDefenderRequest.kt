package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "APK加固请求")
data class ApkDefenderRequest(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @get:Schema(title = "仓库路径", required = true)
    val fullPath: String,
    @get:Schema(title = "用户列表", required = true)
    val userIds: Collection<String>,
    @get:Schema(title = "每批多少个", required = true)
    val batchSize: Int
)
