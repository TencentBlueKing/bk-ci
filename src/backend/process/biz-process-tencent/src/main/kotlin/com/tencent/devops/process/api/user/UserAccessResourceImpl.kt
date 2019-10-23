package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.AccessRepository
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.service.AccessService
import com.tencent.devops.repository.pojo.RepositoryInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAccessResourceImpl @Autowired constructor(
    private val accessService: AccessService
) : UserAccessResource {
    override fun repositoryList(userId: String): Result<SQLPage<RepositoryInfo>> {
        return Result(SQLPage(0, emptyList()))
    }

    override fun create(userId: String, accessRepository: AccessRepository): Result<PipelineId> {
        return Result(PipelineId(accessService.create(userId, accessRepository)))
    }
}