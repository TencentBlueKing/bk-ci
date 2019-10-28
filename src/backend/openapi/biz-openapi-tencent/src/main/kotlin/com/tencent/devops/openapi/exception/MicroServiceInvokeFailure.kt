package com.tencent.devops.openapi.exception

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.UniqueIdException

open class MicroServiceInvokeFailure(
    val serviceInterface: String,
    message: String,
    errorCode: String = CommonMessageCode.SYSTEM_ERROR,
    params: Array<String>? = null
) : UniqueIdException(message, errorCode, params) {
    override fun toString(): String {
        return "MicroService($serviceInterface) invoke fail,message:$message"
    }
}