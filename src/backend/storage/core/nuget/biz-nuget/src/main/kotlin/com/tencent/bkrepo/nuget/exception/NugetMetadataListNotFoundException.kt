package com.tencent.bkrepo.nuget.exception

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.nuget.constant.NugetMessageCode

/**
 * nuget 查询包元数据集合异常
 */
class NugetMetadataListNotFoundException(
    reason: String
) : ErrorCodeException(NugetMessageCode.PACKAGE_METADATA_LIST_NOT_FOUND, reason, HttpStatus.NOT_FOUND)
