package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.user.UserDockerDevClusterResource
import com.tencent.devops.dispatch.pojo.DockerDevCluster
import com.tencent.devops.dispatch.service.DockerDevClusterService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserDockerDevClusterResourceImpl : UserDockerDevClusterResource {

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
}