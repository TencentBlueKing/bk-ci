package com.tencent.devops.common.api.exception

class RemoteServiceException(
    val errorMessage: String,
    val httpStatus: Int = 500,
    val responseContent: String? = null
) : RuntimeException(errorMessage)
