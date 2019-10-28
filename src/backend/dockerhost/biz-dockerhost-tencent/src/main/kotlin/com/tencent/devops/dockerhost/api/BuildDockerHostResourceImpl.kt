package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dockerhost.pojo.DockerBuildParamNew
import com.tencent.devops.dockerhost.pojo.Status
import com.tencent.devops.dockerhost.service.BuildDockerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildDockerHostResourceImpl @Autowired constructor(
    private val dockerService: BuildDockerService
) : BuildDockerHostResource {
    override fun dockerBuild(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, dockerBuildParamNew: DockerBuildParamNew): Result<Boolean> {
        logger.info("Enter ServiceDockerHostResourceImpl.dockerBuild...")
        return Result(dockerService.buildImage(projectId, pipelineId, vmSeqId, buildId, dockerBuildParamNew))
    }

    override fun getDockerBuildStatus(vmSeqId: String, buildId: String): Result<Pair<Status, String>> {
        logger.info("Enter ServiceDockerHostResourceImpl.getDockerBuildStatus...")
        return Result(dockerService.getBuildResult(vmSeqId, buildId))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildDockerHostResourceImpl::class.java)
    }
}