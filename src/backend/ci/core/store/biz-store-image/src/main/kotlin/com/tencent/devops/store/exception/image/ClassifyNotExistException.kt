package com.tencent.devops.store.exception.image

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_INVALID
import com.tencent.devops.common.api.exception.ErrorCodeException

/**
 * @Description 类别不存在，一般为参数传递错误造成
 * @Date 2019/9/3
 * @Version 1.0
 */
class ClassifyNotExistException(
    message: String?,
    errorCode: String = PARAMETER_IS_INVALID,
    params: Array<String>? = null
) :
    ErrorCodeException(errorCode = errorCode, defaultMessage = message, params = params)