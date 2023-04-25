package com.tencent.devops.dispatch.windows.constant

import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2123001, "Dispatch-windows系统错误"),
    NO_IDLE_WINDOWS_ERROR(ErrorType.THIRD_PARTY, 2123002, "DEVCLOUD WINDOWS构建机启动失败，没有空闲的构建机"),
}
