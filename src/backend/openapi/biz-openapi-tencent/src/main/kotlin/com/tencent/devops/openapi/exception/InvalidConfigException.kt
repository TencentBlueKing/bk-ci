package com.tencent.devops.openapi.exception

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.UniqueIdException

open class InvalidConfigException(
    message: String,
    errorCode: String = CommonMessageCode.ERROR_INVALID_CONFIG,
    params: Array<String>? = null
) : UniqueIdException(message, errorCode, params) {
    override fun toString(): String {
        return "Config is invalid,message:$message"
    }
}