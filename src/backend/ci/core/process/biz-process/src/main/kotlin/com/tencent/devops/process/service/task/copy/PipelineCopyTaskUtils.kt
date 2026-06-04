package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType

object PipelineCopyTaskUtils {
    fun resourceKey(
        resourceType: PipelineDependentResourceType,
        resourceId: String
    ): String {
        return "${resourceType.name}_$resourceId"
    }

    fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is ErrorCodeException -> I18nUtil.getCodeLanMessage(
                messageCode = exception.errorCode,
                params = exception.params
            )

            is RemoteServiceException -> exception.errorMessage
            else -> exception.message
        }?.takeIf { it.isNotBlank() } ?: exception.javaClass.simpleName
    }
}
