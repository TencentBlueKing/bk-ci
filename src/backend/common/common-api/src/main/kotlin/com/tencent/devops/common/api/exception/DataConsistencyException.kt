package com.tencent.devops.common.api.exception

import com.tencent.devops.common.api.constant.CommonMessageCode.SYSTEM_ERROR

/**
 * @Description 应当有的数据没有，数据不一致的异常，默认最粗粒度错误为系统错误
 * @Date 2019/9/3
 * @Version 1.0
 */
class DataConsistencyException(
    srcData: String,
    targetData: String,
    message: String?,
    errorCode: String = SYSTEM_ERROR
) :
    UniqueIdException("srcData:$srcData,targetData:$targetData,message:$message", errorCode)