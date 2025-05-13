package com.tencent.devops.common.api.enums.log

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "日志类型")
enum class LogType {
    @Schema(title = "提醒日志")
    WARN,
    @Schema(title = "错误日志")
    ERROR,
    @Schema(title = "调试日志")
    DEBUG,
    @Schema(title = "普通的日志")
    LOG
}
