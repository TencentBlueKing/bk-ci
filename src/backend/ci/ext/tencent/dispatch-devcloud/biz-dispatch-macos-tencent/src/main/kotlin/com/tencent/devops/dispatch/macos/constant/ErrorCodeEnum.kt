package com.tencent.devops.dispatch.macos.constant

import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2123001, "Dispatcher-macos系统错误"),
    NO_IDLE_MACOS_ERROR(ErrorType.THIRD_PARTY, 2123002,
                        "DEVCLOUD MACOS构建机启动失败，没有空闲的构建机"),
}
