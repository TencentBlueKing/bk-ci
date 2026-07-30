package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线草稿摘要")
data class PipelineDraftVersionSimple(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "版本号", required = true)
    val version: Int,
    @get:Schema(title = "草稿版本", required = true)
    val draftVersion: Int,
    @get:Schema(title = "来源的正式版本", required = false)
    val baseVersion: Int?,
    @get:Schema(title = "来源的正式版本名称", required = false)
    val baseVersionName: String?,
    @get:Schema(title = "来源的草稿版本", required = false)
    val baseDraftVersion: Int?,
    @get:Schema(title = "创建者", required = false)
    val creator: String?,
    @get:Schema(title = "创建时间", required = true)
    val createTime: Long,
    @get:Schema(title = "最近更新人", required = false)
    val updater: String?,
    @get:Schema(title = "更新时间", required = true)
    val updateTime: Long
)
