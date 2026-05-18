package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务状态")
enum class PipelineBatchTaskStatus(
    val desc: String
) {
    DRAFT("草稿"),
    EXECUTING("执行中"),
    SUCCESS("成功"),
    FAILED("失败"),
    PARTIAL_FAILED("部分失败"),
    CANCELED("取消")
}
