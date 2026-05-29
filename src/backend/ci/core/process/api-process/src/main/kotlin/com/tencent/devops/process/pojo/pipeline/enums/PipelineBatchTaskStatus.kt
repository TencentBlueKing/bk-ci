package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务状态")
enum class PipelineBatchTaskStatus {
    @Schema(description = "草稿")
    DRAFT,

    @Schema(description = "流水线分析中")
    PIPELINE_ANALYZING,

    @Schema(description = "流水线资源分析中")
    PIPELINE_RESOURCE_ANALYZING,

    @Schema(description = "执行中")
    EXECUTING,

    @Schema(description = "成功")
    SUCCESS,

    @Schema(description = "失败")
    FAILED,

    @Schema(description = "部分失败")
    PARTIAL_FAILED,

    @Schema(description = "已删除")
    DELETED
}
