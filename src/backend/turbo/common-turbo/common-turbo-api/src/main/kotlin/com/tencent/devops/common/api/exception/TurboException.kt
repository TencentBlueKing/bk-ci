package com.tencent.devops.common.api.exception

import com.tencent.devops.common.api.exception.code.TURBO_GENERAL_SYSTEM_FAIL

class TurboException(
    val errorCode: String = TURBO_GENERAL_SYSTEM_FAIL,
    errorMessage: String
) : RuntimeException(errorMessage)
