package com.tencent.devops.artifactory.pojo.artifact

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 流水线产出物查询条件
 */
@Schema(title = "流水线产出物查询条件")
data class PipelineArtifactInfoQuery(
    @get:Schema(title = "蓝盾项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "产出物类型：FILE/IMAGE/REPORT/PACKAGE等（不传返回全部类型）", required = false)
    val artifactType: String? = null,
    @get:Schema(title = "产出物名称，如文件名、镜像名（BKM溯源场景必传）", required = false)
    val artifactName: String? = null,
    @get:Schema(title = "产出物版本，如镜像Tag、包版本（BKM溯源场景必传）", required = false)
    val artifactVersion: String? = null,
    @get:Schema(title = "流水线ID")
    val pipelineId: String? = null,
    @get:Schema(title = "构建ID")
    val buildId: String? = null,
    @get:Schema(title = "执行次数")
    val executeCount: Int? = null
)
