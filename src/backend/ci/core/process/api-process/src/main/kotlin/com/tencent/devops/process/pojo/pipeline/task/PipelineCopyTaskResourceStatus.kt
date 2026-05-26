package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务资源状态")
enum class PipelineCopyTaskResourceStatus {
    @Schema(description = "未处理")
    UNPROCESSED,

    @Schema(description = "已处理")
    PROCESSED,

    @Schema(description = "复制成功")
    SUCCESS,

    @Schema(description = "复制失败")
    FAILED
}
