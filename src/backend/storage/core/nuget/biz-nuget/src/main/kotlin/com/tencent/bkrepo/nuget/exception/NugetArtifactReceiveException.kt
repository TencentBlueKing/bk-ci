package com.tencent.bkrepo.nuget.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.nuget.constant.NugetMessageCode

/**
 * nuget 构件接收异常
 */
class NugetArtifactReceiveException(
    reason: String
) : ErrorCodeException(NugetMessageCode.PACKAGE_CONTENT_INVALID, reason, HttpStatus.BAD_REQUEST)
