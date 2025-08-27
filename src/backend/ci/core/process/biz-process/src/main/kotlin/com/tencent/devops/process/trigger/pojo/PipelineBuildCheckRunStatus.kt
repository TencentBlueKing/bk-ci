package com.tencent.devops.process.trigger.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "构建检查运行写入状态")
enum class PipelineBuildCheckRunStatus {
    @Schema(description = "待处理")
    PENDING,
    @Schema(description = "写入-运行中")
    RUNNING,
    @Schema(description = "写入-成功")
    SUCCESS,
    @Schema(description = "写入-失败")
    FAILED,
    @Schema(description = "跳过,不需要写入")
    SKIP;

    fun isFinished(): Boolean {
        return this == SUCCESS || this == FAILED || this == SKIP
    }
}
