package com.tencent.devops.common.api.exception

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_INVALID

class InvalidParamException(message: String, errorCode: String = PARAMETER_IS_INVALID, params: Array<String>? = null) :
    UniqueIdException(message, errorCode, params)