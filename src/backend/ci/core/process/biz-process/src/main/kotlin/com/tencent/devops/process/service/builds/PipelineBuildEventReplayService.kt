package com.tencent.devops.process.service.builds

import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.repository.pojo.Repository
import org.springframework.stereotype.Service

@Service
class PipelineBuildEventReplayService {

    fun matchMrEvent(
        event: GitMergeRequestEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ) {

    }


}