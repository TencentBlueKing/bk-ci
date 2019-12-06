package com.tencent.devops.store.exception.image

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException

/**
 * @Description 镜像不存在的异常，一般为参数错误导致
 * @Date 2019/9/3
 * @Version 1.0
 */
class ImageNotExistException(
    message: String?,
    errorCode: String = CommonMessageCode.PARAMETER_IS_INVALID,
    params: Array<String>? = null
) :
    ErrorCodeException(errorCode, message, params)