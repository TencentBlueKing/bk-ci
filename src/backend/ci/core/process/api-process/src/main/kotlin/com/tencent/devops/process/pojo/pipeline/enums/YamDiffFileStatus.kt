package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "yaml变更文件状态")
enum class YamDiffFileStatus {
    @Schema(description = "待处理")
    PENDING,
    @Schema(description = "运行中")
    RUNNING,
    @Schema(description = "成功")
    SUCCESS,
    @Schema(description = "失败")
    FAILED;

    fun isFinished(): Boolean {
        return this == SUCCESS || this == FAILED
    }

    fun isPending(): Boolean {
        return this == PENDING
    }
}
