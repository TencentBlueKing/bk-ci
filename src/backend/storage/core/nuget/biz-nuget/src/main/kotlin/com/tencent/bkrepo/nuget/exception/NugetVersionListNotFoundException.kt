package com.tencent.bkrepo.nuget.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.nuget.constant.NugetMessageCode

/**
 * nuget查询包的所有版本异常
 */
class NugetVersionListNotFoundException(
    reason: String
) : ErrorCodeException(NugetMessageCode.PACKAGE_VERSIONS_NOT_EXISTED, reason, HttpStatus.NOT_FOUND)
