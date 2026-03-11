package com.tencent.devops.dispatch.windows.constant

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.web.utils.I18nUtil

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2133028, "Dispatch-windows系统错误"),
    NO_IDLE_WINDOWS_ERROR(
        ErrorType.THIRD_PARTY,
        2133029,
        "DEVCLOUD WINDOWS构建机启动失败，没有空闲的构建机"
    );

    fun getErrorMessage(): String {
        return I18nUtil.getCodeLanMessage(this.errorCode.toString())
    }
}
