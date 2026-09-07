package com.tencent.devops.process.pojo.creative

import com.tencent.devops.process.enums.CreativeFlowCopyStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流跨空间复制结果")
data class CreativeFlowCopyResult(
    @get:Schema(title = "复制状态：CREATED / OVERWRITTEN / SKIPPED")
    val status: CreativeFlowCopyStatus,
    @get:Schema(title = "目标流水线ID")
    val targetPipelineId: String? = null,
    @get:Schema(title = "目标创作流名称")
    val targetPipelineName: String? = null,
    @get:Schema(title = "目标版本号（内部整型）")
    val targetVersion: Int? = null,
    @get:Schema(title = "目标发布版本号，形如 V1")
    val targetVersionNum: String? = null,
    @get:Schema(title = "实际复制的源版本号（内部整型）")
    val resolvedSourceVersion: Int? = null,
    @get:Schema(title = "实际复制的源发布版本号，形如 V208")
    val resolvedSourceVersionNum: String? = null,
    @get:Schema(title = "SKIPPED 时的原因")
    val skippedReason: String? = null
)
