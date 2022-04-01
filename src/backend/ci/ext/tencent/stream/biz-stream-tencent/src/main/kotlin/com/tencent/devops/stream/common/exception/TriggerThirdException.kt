package com.tencent.devops.stream.common.exception

import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

class TriggerThirdException(
    requestEvent: GitRequestEventForHandle,
    gitEvent: GitEvent? = null,
    commitCheck: CommitCheck? = null,
    basicSetting: GitCIBasicSetting? = null,
    pipeline: GitProjectPipeline? = null,
    yamls: Yamls? = null,
    version: String? = null,
    filePath: String? = null,
    val errorCode: String,
    val errorMessage: String?,
    val messageParams: List<String>? = null
) : TriggerBaseException(
    requestEvent = requestEvent,
    reasonParams = messageParams,
    gitEvent = gitEvent,
    commitCheck = commitCheck,
    basicSetting = basicSetting,
    pipeline = pipeline,
    yamls = yamls,
    version = version,
    filePath = filePath
) {
    companion object {
        fun triggerThirdError(
            request: GitRequestEventForHandle,
            messageParams: List<String>? = null,
            event: GitEvent? = null,
            basicSetting: GitCIBasicSetting? = null,
            commitCheck: CommitCheck? = null,
            pipeline: GitProjectPipeline? = null,
            yamls: Yamls? = null,
            version: String? = null,
            code: String,
            message: String?,
            filePath: String? = null
        ): Nothing {
            throw TriggerThirdException(
                requestEvent = request,
                gitEvent = event,
                commitCheck = commitCheck,
                basicSetting = basicSetting,
                pipeline = pipeline,
                yamls = yamls,
                version = version,
                errorCode = code,
                errorMessage = message,
                messageParams = messageParams,
                filePath = filePath
            )
        }
    }
}
