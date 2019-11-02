package com.tencent.devops.dispatch.exception

import com.tencent.devops.common.api.constant.CommonMessageCode.PERMISSION_DENIED
import com.tencent.devops.common.api.exception.ErrorCodeException

class UnknownImageSourceType(
    message: String?,
    errorCode: String = PERMISSION_DENIED,
    params: Array<String>? = null
) :
    ErrorCodeException(errorCode, message, params)