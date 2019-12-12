package com.tencent.devops.store.pojo.image.exception

import com.tencent.devops.common.api.constant.CommonMessageCode.PERMISSION_DENIED
import com.tencent.devops.common.api.exception.ErrorCodeException

class ImageNotInstalledException(
    message: String?,
    errorCode: String = PERMISSION_DENIED,
    params: Array<String>? = null
) :
    ErrorCodeException(errorCode = errorCode, defaultMessage = message, params = params)