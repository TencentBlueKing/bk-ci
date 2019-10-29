package com.tencent.devops.common.api.exception

import com.tencent.devops.common.api.constant.CommonMessageCode.SYSTEM_ERROR
import com.tencent.devops.common.api.util.UUIDUtil

/**
 * @Description 加上uniqueId便于日志中定位异常，加上错误码便于统计异常与国际化
 * 默认最粗粒度错误为系统错误500
 * @Date 2019/9/3
 * @Version 1.0
 */
open class UniqueIdException(
    message: String?,
    errorCode: String = SYSTEM_ERROR,
    params: Array<String>? = null,
    val uniqueId: String? = UUIDUtil.generate()
) :
    ErrorCodeException(errorCode, "uniqueId=$uniqueId:errorCode=$errorCode:$message", params)