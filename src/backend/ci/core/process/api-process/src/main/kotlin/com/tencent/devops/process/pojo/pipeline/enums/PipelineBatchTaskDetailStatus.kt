package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务明细状态")
enum class PipelineBatchTaskDetailStatus {
    @Schema(description = "排除")
    EXCLUDED,

    @Schema(description = "成功")
    SUCCESS,

    @Schema(description = "失败")
    FAILED,

    /**
     * 复制相关状态
     */
    @Schema(description = "待复制")
    WAIT_COPY,
}
