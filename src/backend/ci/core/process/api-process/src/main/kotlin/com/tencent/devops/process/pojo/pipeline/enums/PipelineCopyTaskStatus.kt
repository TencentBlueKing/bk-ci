package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务状态")
enum class PipelineCopyTaskStatus {
    @Schema(description = "草稿")
    DRAFT,

    @Schema(description = "执行中")
    EXECUTING,

    @Schema(description = "成功")
    SUCCESS,

    @Schema(description = "失败")
    FAILED,

    @Schema(description = "部分失败")
    PARTIAL_FAILED,

    @Schema(description = "取消")
    CANCELED,

    @Schema(description = "已删除")
    DELETED,

    /**
     * 复制相关状态
     */
    @Schema(description = "复制子流水线分析中")
    SUB_PIPELINE_ANALYZING,

    @Schema(description = "子流水线分析完成")
    SUB_PIPELINE_ANALYSIS_FINISHED,

    @Schema(description = "依赖资源分析中")
    RESOURCE_DEPEND_ANALYZING,

    @Schema(description = "依赖资源分析成功")
    RESOURCE_DEPEND_ANALYSIS_SUCCESS
}
