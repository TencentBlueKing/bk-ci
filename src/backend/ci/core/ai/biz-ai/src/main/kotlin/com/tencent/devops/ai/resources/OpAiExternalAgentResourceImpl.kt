package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.op.OpAiExternalAgentResource
import com.tencent.devops.ai.pojo.ExternalAgentCreate
import com.tencent.devops.ai.pojo.ExternalAgentInfo
import com.tencent.devops.ai.pojo.ExternalAgentUpdate
import com.tencent.devops.ai.service.ExternalAgentService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAiExternalAgentResourceImpl @Autowired constructor(
    private val externalAgentService: ExternalAgentService
) : OpAiExternalAgentResource {
    override fun list(): Result<List<ExternalAgentInfo>> = Result(externalAgentService.listAllForOp())
    override fun create(request: ExternalAgentCreate): Result<ExternalAgentInfo> =
        Result(externalAgentService.createForOp(request))
    override fun update(configId: String, request: ExternalAgentUpdate): Result<Boolean> =
        Result(externalAgentService.updateForOp(configId, request))
    override fun delete(configId: String): Result<Boolean> = Result(externalAgentService.deleteForOp(configId))
}
