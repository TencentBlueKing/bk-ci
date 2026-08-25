package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyResult
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyTraceVo
import com.tencent.devops.process.service.creative.CreativeFlowCopyService
import com.tencent.devops.process.service.creative.CreativeFlowCopyTraceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCreativeFlowCopyResourceImpl @Autowired constructor(
    private val creativeFlowCopyService: CreativeFlowCopyService,
    private val creativeFlowCopyTraceService: CreativeFlowCopyTraceService
) : ServiceCreativeFlowCopyResource {

    override fun copyAcrossProject(
        userId: String,
        projectId: String,
        request: CreativeFlowCopyRequest
    ): Result<CreativeFlowCopyResult> {
        return Result(creativeFlowCopyService.copyAcrossProject(userId, projectId, request))
    }

    override fun listCopyTraces(
        userId: String,
        projectId: String,
        shareId: String?,
        flowId: String?,
        targetPipelineId: String?
    ): Result<List<CreativeFlowCopyTraceVo>> {
        return Result(
            creativeFlowCopyTraceService.listByTargetProject(
                targetProjectId = projectId,
                shareId = shareId,
                flowId = flowId,
                targetPipelineId = targetPipelineId
            )
        )
    }
}
