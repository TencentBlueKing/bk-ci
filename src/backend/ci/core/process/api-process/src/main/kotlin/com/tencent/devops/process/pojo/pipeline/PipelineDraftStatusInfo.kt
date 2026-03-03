package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "流水线草稿状态信息")
data class PipelineDraftStatusInfo(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "版本号", required = true)
    val version: Int,
    @get:Schema(title = "草稿版本号", required = true)
    val debugVersion: Int,
    @get:Schema(title = "来源的正式版本", required = false)
    val baseVersion: Int?,
    @get:Schema(title = "创建者", required = false)
    val creator: String?,
    @get:Schema(title = "创建时间", required = true)
    val createTime: LocalDateTime,
    @get:Schema(title = "最新正式版本", required = true)
    val latestReleaseVersion: Int? = null
)
