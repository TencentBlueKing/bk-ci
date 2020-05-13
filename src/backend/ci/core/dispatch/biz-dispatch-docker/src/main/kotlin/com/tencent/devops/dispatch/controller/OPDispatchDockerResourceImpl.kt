package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.op.OPDispatchDockerResource
import com.tencent.devops.dispatch.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.pojo.DockerIpListPage
import com.tencent.devops.dispatch.pojo.DockerIpUpdateVO
import com.tencent.devops.dispatch.service.DispatchDockerService

@RestResource
class OPDispatchDockerResourceImpl constructor(
    private val dispatchDockerService: DispatchDockerService
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
}
