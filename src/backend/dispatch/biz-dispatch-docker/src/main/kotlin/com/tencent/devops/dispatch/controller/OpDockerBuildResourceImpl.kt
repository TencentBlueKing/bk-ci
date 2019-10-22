package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpDockerBuildResource
import com.tencent.devops.dispatch.service.DockerHostBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpDockerBuildResourceImpl @Autowired constructor(private val dockerHostBuildService: DockerHostBuildService)
    : OpDockerBuildResource {

    override fun enable(pipelineId: String, vmSeqId: Int?, enable: Boolean): Result<Boolean> {
        dockerHostBuildService.enable(pipelineId, vmSeqId, enable)
        return Result(true)
    }
}