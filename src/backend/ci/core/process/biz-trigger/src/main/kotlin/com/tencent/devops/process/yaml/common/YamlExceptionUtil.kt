package com.tencent.devops.process.yaml.common

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedErrorCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMsg
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail

object YamlExceptionUtil {
    fun getReasonDetail(exception: Exception): Pair<String, PipelineTriggerReasonDetail> {
        return when (exception) {
            is ErrorCodeException -> Pair(
                PipelineTriggerReason.TRIGGER_FAILED.name,
                PipelineTriggerFailedErrorCode(errorCode = exception.errorCode, params = exception.params?.toList())
            )
            is NotFoundException -> Pair(
                PipelineTriggerReason.TRIGGER_FAILED.name,
                PipelineTriggerFailedMsg(exception.message ?: PipelineTriggerReason.UNKNOWN_ERROR.detail)
            )
            else -> Pair(
                PipelineTriggerReason.TRIGGER_FAILED.name,
                PipelineTriggerFailedMsg(exception.message ?: PipelineTriggerReason.UNKNOWN_ERROR.detail)
            )
        }
    }
}
