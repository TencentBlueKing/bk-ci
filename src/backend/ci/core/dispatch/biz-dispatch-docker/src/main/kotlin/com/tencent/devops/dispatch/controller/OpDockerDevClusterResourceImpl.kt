package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.op.OpDockerDevClusterResource
import com.tencent.devops.dispatch.pojo.DockerDevCluster
import com.tencent.devops.dispatch.service.DockerDevClusterService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpDockerDevClusterResourceImpl : OpDockerDevClusterResource {

    @Autowired
    private lateinit var dockerDevClusterService: DockerDevClusterService

    override fun listDockerCluster(
        userId: String,
        page: Int?,
        pageSize: Int?,
        includeDisable: Boolean?
    ): Result<Page<DockerDevCluster>> {
        return Result(
            dockerDevClusterService.list(userId, page, pageSize, includeDisable)
        )
    }

    override fun createDockerCluster(
        userId: String, dockerDevCluster: DockerDevCluster
    ): Result<Boolean> {
        return Result(dockerDevClusterService.create(userId, dockerDevCluster))
    }

    override fun updateDispatchDocker(
        userId: String, dockerDevCluster: DockerDevCluster
    ): Result<Boolean> {
        return Result(dockerDevClusterService.update(userId, dockerDevCluster))
    }

    override fun deleteDispatchDocker(
        userId: String, clusterId: String
    ): Result<Boolean> {
        return Result(dockerDevClusterService.delete(userId, clusterId))
    }
}