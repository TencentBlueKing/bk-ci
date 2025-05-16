package com.tencent.devops.common.api.enums.log

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "日志状态")
enum class LogStatus(val status: Int) {
    @Schema(title = "查询成功")
    SUCCEED(0),
    @Schema(title = "日志为空")
    EMPTY(1),
    @Schema(title = "日志已过期")
    CLEAN(2),
    @Schema(title = "日志已清理")
    CLOSED(3),
    @Schema(title = "查询异常")
    FAIL(999);

    @JsonValue
    fun jsonValue(): Int {
        return status
    }
    companion object {
        fun parse(status: Int): LogStatus {
            return when (status) {
                0 -> SUCCEED
                1 -> EMPTY
                2 -> CLEAN
                3 -> CLOSED
                999 -> FAIL
                else -> FAIL
            }
        }
    }
}
