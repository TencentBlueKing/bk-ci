package com.tencent.devops.remotedev.common.exception

import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {

    USER_NOT_EXISTS(
        errorType = ErrorType.USER,
        errorCode = 2129025,
        formatErrorMessage = "账号[%s]不存在，请先联系 DevOps-helper 注册"
    ),
    OAUTH_ILLEGAL(
        errorType = ErrorType.USER,
        errorCode = 401,
        formatErrorMessage = "[%s] oauth失效，需重新授权"
    )
}
