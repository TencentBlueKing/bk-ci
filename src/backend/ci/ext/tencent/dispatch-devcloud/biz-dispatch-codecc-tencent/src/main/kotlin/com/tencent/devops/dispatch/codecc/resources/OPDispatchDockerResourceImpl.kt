package com.tencent.devops.dispatch.codecc.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.codecc.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.codecc.pojo.DockerIpListPage
import com.tencent.devops.dispatch.codecc.api.OPDispatchDockerResource
import com.tencent.devops.dispatch.codecc.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.codecc.pojo.DockerIpUpdateVO
import com.tencent.devops.dispatch.codecc.pojo.SpecialDockerHostVO
import com.tencent.devops.dispatch.codecc.service.DispatchDockerService
import com.tencent.devops.dispatch.codecc.service.PipelineDockerHostService

@RestResource
class OPDispatchDockerResourceImpl constructor(
    private val dispatchDockerService: DispatchDockerService,
    private val pipelineDockerHostService: PipelineDockerHostService
) : OPDispatchDockerResource {

    override fun listDispatchDocker(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<DockerIpListPage<DockerIpInfoVO>> {
        return Result(dispatchDockerService.list(userId, page, pageSize))
    }

    override fun createDispatchDocker(userId: String, dockerIpInfoVOs: List<DockerIpInfoVO>): Result<Boolean> {
        return Result(dispatchDockerService.create(userId, dockerIpInfoVOs))
    }

    override fun updateDispatchDocker(userId: String, dockerIp: String, dockerIpUpdateVO: DockerIpUpdateVO): Result<Boolean> {
        return Result(dispatchDockerService.update(userId, dockerIp, dockerIpUpdateVO))
    }

    override fun updateAllDispatchDockerEnable(userId: String): Result<Boolean> {
        return Result(dispatchDockerService.updateAllDispatchDockerEnable(userId))
    }

    override fun deleteDispatchDocker(userId: String, dockerIp: String): Result<Boolean> {
        return Result(dispatchDockerService.delete(userId, dockerIp))
    }

    override fun createDockerHostLoadConfig(
        userId: String,
        dockerHostLoadConfigMap: Map<String, DockerHostLoadConfig>
    ): Result<Boolean> {
        return Result(dispatchDockerService.createDockerHostLoadConfig(userId, dockerHostLoadConfigMap))
    }

    override fun updateDockerDriftThreshold(userId: String, threshold: Int): Result<Boolean> {
        return Result(dispatchDockerService.updateDockerDriftThreshold(userId, threshold))
    }

    override fun deleteDockerBuildRedisAuth(
        userId: String,
        startTime: String,
        endTime: String,
        limit: Int
    ): Result<Boolean> {
        return Result(dispatchDockerService.deleteDockerBuildRedisAuth(userId, startTime, endTime, limit))
    }

    override fun createSpecialDockerHost(
        userId: String,
        specialDockerHostVOs: List<SpecialDockerHostVO>
    ): Result<Boolean> {
        return Result(pipelineDockerHostService.create(userId, specialDockerHostVOs))
    }

    override fun updateSpecialDockerHost(userId: String, specialDockerHostVO: SpecialDockerHostVO): Result<Boolean> {
        return Result(pipelineDockerHostService.update(userId, specialDockerHostVO))
    }

    override fun deleteSpecialDockerHost(userId: String, projectId: String): Result<Boolean> {
        return Result(pipelineDockerHostService.delete(userId, projectId))
    }
}
