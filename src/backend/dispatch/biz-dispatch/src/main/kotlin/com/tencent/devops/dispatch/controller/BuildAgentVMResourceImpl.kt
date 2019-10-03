package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.BuildAgentVMResource
import com.tencent.devops.dispatch.pojo.VM
import com.tencent.devops.dispatch.service.PipelineDispatchService
import com.tencent.devops.dispatch.service.VMService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildAgentVMResourceImpl @Autowired constructor(
    private val pipelineDispatchService: PipelineDispatchService,
    private val vMService: VMService
) : BuildAgentVMResource {

    override fun getVmByPipeLine(projectId: String, buildId: String, agentId: String, secretKey: String, vmSeqId: String): Result<VM> {
        val pipeline = pipelineDispatchService.queryPipelineByBuildAndSeqId(buildId, vmSeqId)
        return Result(vMService.queryVMById(pipeline.vmId))
    }
}