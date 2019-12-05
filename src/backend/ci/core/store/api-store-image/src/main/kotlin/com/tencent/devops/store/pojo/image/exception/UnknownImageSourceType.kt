package com.tencent.devops.store.pojo.image.exception

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException

class UnknownImageSourceType(
    override val message: String?,
    override val errorCode: String = CommonMessageCode.PARAMETER_IS_INVALID,
    params: Array<String>? = null
) :
    ErrorCodeException(errorCode, message, params)