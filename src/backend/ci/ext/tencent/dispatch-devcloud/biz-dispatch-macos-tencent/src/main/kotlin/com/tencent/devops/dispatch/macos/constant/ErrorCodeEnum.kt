package com.tencent.devops.dispatch.macos.constant

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2133021, "Dispatcher-macos系统错误"),
    NO_IDLE_MACOS_ERROR(
        ErrorType.THIRD_PARTY,
        2133022,
        "DEVCLOUD MACOS构建机启动失败，没有空闲的构建机"
    )
}
