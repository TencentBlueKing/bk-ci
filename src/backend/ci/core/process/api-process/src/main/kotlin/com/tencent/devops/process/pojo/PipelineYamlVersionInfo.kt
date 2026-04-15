package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.enums.VersionStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线pac版本信息")
data class PipelineYamlVersionInfo(
    @get:Schema(title = "版本名称", required = false)
    val name: String,
    @get:Schema(title = "版本号", required = false)
    val version: Int? = null,
    @get:Schema(title = "版本状态", required = false)
    val versionStatus: VersionStatus,
    @get:Schema(title = "是否为默认分支", required = false)
    val defaultBranch: Boolean = false,
    @get:Schema(title = "当前分支最新commit sha", required = false)
    val sha: String? = null
)