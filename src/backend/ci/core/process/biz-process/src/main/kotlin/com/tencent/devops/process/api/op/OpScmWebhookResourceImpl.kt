package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineWebhookService

@RestResource
class OpScmWebhookResourceImpl(
    private val pipelineWebhookService: PipelineWebhookService
) : OpScmWebhookResource {

    override fun updateWebhookSecret(scmType: String): Result<Boolean> {
        pipelineWebhookService.updateWebhookSecret(ScmType.valueOf(scmType))
        return Result(true)
    }
}
