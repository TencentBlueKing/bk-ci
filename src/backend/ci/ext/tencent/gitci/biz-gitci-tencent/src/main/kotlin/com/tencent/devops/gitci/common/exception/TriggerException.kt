package com.tencent.devops.gitci.common.exception

import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.v2.GitCIBasicSetting

class TriggerException(
    requestEvent: GitRequestEvent,
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
            request: GitRequestEvent,
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
