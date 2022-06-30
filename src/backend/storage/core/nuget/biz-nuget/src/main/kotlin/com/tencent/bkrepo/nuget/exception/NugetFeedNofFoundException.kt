package com.tencent.bkrepo.nuget.exception

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.nuget.constant.NugetMessageCode

class NugetFeedNofFoundException(
    reason: String
) : NotFoundException(NugetMessageCode.RESOURCE_FEED_NOT_FOUND, reason)
