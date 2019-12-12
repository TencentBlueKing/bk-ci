package com.tencent.devops.store.pojo.image.exception

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException

class UnknownImageSourceType(
    override val message: String?,
    errorCode: String = CommonMessageCode.PARAMETER_IS_INVALID,
    params: Array<String>? = null
) :
    ErrorCodeException(errorCode = errorCode, defaultMessage = message, params = params)