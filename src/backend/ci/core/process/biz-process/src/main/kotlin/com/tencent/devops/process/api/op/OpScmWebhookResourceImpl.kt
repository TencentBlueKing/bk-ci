package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineWebhookUpgradeService

@RestResource
class OpScmWebhookResourceImpl(
    private val pipelineWebhookUpgradeService: PipelineWebhookUpgradeService
) : OpScmWebhookResource {

    override fun updateProjectNameAndTaskId(): Result<Boolean> {
        pipelineWebhookUpgradeService.updateProjectNameAndTaskId()
        return Result(true)
    }

    override fun updateWebhookSecret(scmType: String): Result<Boolean> {
        pipelineWebhookUpgradeService.updateWebhookSecret(ScmType.valueOf(scmType))
        return Result(true)
    }

    override fun updateWebhookEventInfo(
        projectId: String?
    ): Result<Boolean> {
        pipelineWebhookUpgradeService.updateWebhookEventInfo(
            projectId = projectId
        )
        return Result(true)
    }
}
