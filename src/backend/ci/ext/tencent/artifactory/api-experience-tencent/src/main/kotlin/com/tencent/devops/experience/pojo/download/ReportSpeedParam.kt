package com.tencent.devops.experience.pojo.download

import com.tencent.devops.experience.constant.ExperienceDownloadType
import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "上报下载速度参数")
data class ReportSpeedParam(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "体验ID", required = false)
    val experienceId: String? = null,
    @get:Schema(title = "仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @get:Schema(title = "仓库路径", required = true)
    val path: String,
    @get:Schema(title = "下载速度", required = true)
    val downloadSpeed: Long,
    @get:Schema(title = "下载类型", required = true)
    val downloadType: ExperienceDownloadType
)
