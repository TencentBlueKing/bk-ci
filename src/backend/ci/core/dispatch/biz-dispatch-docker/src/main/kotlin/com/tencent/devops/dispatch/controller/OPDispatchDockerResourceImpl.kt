package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OPDispatchDockerResource
import com.tencent.devops.dispatch.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.pojo.DockerIpListPage
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

    override fun createDispatchDocker(userId: String, dockerIpInfoVO: DockerIpInfoVO): Result<Boolean> {
        return Result(dispatchDockerService.create(userId, dockerIpInfoVO))
    }

    override fun updateDispatchDocker(userId: String, dockerIpInfoId: Long, enable: Boolean): Result<Boolean> {
        return Result(dispatchDockerService.update(userId, dockerIpInfoId, enable))
    }

    override fun deleteDispatchDocker(userId: String, dockerIpInfoId: Long): Result<Boolean> {
        return Result(dispatchDockerService.delete(userId, dockerIpInfoId))
    }
}
