package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "APK加固请求")
data class ApkDefenderRequest(
    @Schema(description = "项目ID", required = true)
    val projectId: String,
    @Schema(description = "仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @Schema(description = "仓库路径", required = true)
    val fullPath: String,
    @Schema(description = "用户列表", required = true)
    val userIds: Collection<String>,
    @Schema(description = "每批多少个", required = true)
    val batchSize: Int
)
