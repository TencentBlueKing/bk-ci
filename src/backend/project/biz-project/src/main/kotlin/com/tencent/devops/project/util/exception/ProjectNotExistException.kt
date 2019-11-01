package com.tencent.devops.project.util.exception

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_INVALID
import com.tencent.devops.common.api.exception.UniqueIdException

/**
 * @Description
 * @Date 2019/9/23
 * @Version 1.0
 */
class ProjectNotExistException(message: String?, errorCode: String = PARAMETER_IS_INVALID) :
    UniqueIdException(message, errorCode) {
}