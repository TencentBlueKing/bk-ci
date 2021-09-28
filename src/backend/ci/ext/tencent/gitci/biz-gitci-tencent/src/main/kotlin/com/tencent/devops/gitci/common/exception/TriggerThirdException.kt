package com.tencent.devops.gitci.common.exception

import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.v2.GitCIBasicSetting

class TriggerThirdException(
    requestEvent: GitRequestEvent,
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
            request: GitRequestEvent,
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
