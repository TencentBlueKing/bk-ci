package com.tencent.devops.dispatch.bcs.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatcher.bcs.api.service.ServiceBcsDebugResource
import com.tencent.devops.dispatch.bcs.pojo.BcsDebugResponse
import com.tencent.devops.dispatch.bcs.service.BcsDebugService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceBcsDebugResourceImpl @Autowired constructor(
    private val bcsDebugService: BcsDebugService
) : ServiceBcsDebugResource {
    override fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?
    ): Result<BcsDebugResponse> {
        return Result(bcsDebugService.startDebug(userId, projectId, pipelineId, buildId, vmSeqId))
    }

    override fun stopDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): Result<Boolean> {
        return Result(bcsDebugService.stopDebug(userId, pipelineId, containerName, vmSeqId))
    }
}
