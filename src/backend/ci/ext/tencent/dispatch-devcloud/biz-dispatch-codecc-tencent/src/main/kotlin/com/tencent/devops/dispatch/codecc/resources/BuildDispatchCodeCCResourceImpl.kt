package com.tencent.devops.dispatch.codecc.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.codecc.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.codecc.api.BuildDispatchCodeCCResource
import com.tencent.devops.dispatch.codecc.pojo.codecc.DockerHostBuildInfo
import com.tencent.devops.dispatch.codecc.pojo.codecc.DockerResourceOptionsVO
import com.tencent.devops.dispatch.codecc.service.DispatchCodeCCService
import com.tencent.devops.dispatch.codecc.service.DispatchDockerService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildDispatchCodeCCResourceImpl @Autowired constructor(
    private val dispatchCodeCCService: DispatchCodeCCService,
    private val dispatchDockerService: DispatchDockerService
) : BuildDispatchCodeCCResource {

    override fun rollbackBuild(
        buildId: String,
        vmSeqId: Int,
        shutdown: Boolean?
    ): com.tencent.devops.common.api.pojo.Result<Boolean>? {
        return dispatchCodeCCService.rollbackBuild(buildId, vmSeqId, shutdown)
    }

    override fun reportContainerId(
        buildId: String,
        vmSeqId: String,
        containerId: String,
        hostTag: String?
    ): Result<Boolean> {
        return dispatchCodeCCService.reportContainerId(buildId, vmSeqId, containerId, hostTag)
    }

    override fun startBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        return dispatchCodeCCService.fetchBuildInfo(hostTag)
    }

    override fun endBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        return dispatchCodeCCService.endBuild(hostTag)
    }

    override fun postLog(buildId: String, red: Boolean, message: String, tag: String?, jobId: String?): Result<Boolean>? {
        dispatchCodeCCService.log(buildId, red, message, tag, jobId)
        return Result(0, "success")
    }

    override fun refresh(dockerIp: String, dockerIpInfoVO: DockerIpInfoVO): Result<Boolean> {
        return Result(dispatchDockerService.updateDockerIpLoad("dockerhost", dockerIp, dockerIpInfoVO))
    }

    override fun getResourceConfig(pipelineId: String, vmSeqId: String): Result<DockerResourceOptionsVO> {
        return Result(
            DockerResourceOptionsVO(
            memoryLimitBytes = 68719476736L,
            cpuPeriod = 10000,
            cpuQuota = 320000,
            blkioDeviceReadBps = 125829120,
            blkioDeviceWriteBps = 125829120,
            disk = 100,
            description = ""
        )
        )
    }

    override fun getQpcGitProjectList(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        poolNo: Int
    ): Result<List<String>> {
        return Result(emptyList())
    }
}
