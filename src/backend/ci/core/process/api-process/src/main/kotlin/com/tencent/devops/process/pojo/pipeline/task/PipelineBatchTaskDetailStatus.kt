package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务明细状态")
enum class PipelineBatchTaskDetailStatus(
    val desc: String
) {
    WAIT_COPY("待复制"),
    EXCLUDED("排除"),
    SUCCESS("成功"),
    FAILED("失败")
}
