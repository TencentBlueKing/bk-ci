package com.tencent.devops.stream.common.exception

import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

class TriggerException(
    requestEvent: GitRequestEventForHandle,
    reasonParams: List<String>? = null,
    gitEvent: GitEvent? = null,
    commitCheck: CommitCheck? = null,
    basicSetting: GitCIBasicSetting? = null,
    pipeline: GitProjectPipeline? = null,
    yamls: Yamls? = null,
    version: String? = null,
    filePath: String? = null,
    val triggerReason: TriggerReason
) : TriggerBaseException(
    requestEvent = requestEvent,
    reasonParams = reasonParams,
    gitEvent = gitEvent,
    commitCheck = commitCheck,
    basicSetting = basicSetting,
    pipeline = pipeline,
    yamls = yamls,
    version = version,
    filePath = filePath
) {
    companion object {
        fun triggerError(
            request: GitRequestEventForHandle,
            reason: TriggerReason,
            reasonParams: List<String>? = null,
            event: GitEvent? = null,
            commitCheck: CommitCheck? = null,
            basicSetting: GitCIBasicSetting? = null,
            pipeline: GitProjectPipeline? = null,
            yamls: Yamls? = null,
            version: String? = null,
            filePath: String? = null
        ): Nothing {
            throw TriggerException(
                requestEvent = request,
                triggerReason = reason,
                reasonParams = reasonParams,
                gitEvent = event,
                commitCheck = commitCheck,
                basicSetting = basicSetting,
                pipeline = pipeline,
                yamls = yamls,
                version = version,
                filePath = filePath
            )
        }
    }
}
