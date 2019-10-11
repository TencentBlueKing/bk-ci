package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceCommitResource
import com.tencent.devops.repository.pojo.commit.CommitData
import com.tencent.devops.repository.pojo.commit.CommitResponse
import com.tencent.devops.repository.service.RepositoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCommitResourceImpl @Autowired constructor(
    private val repositoryService: RepositoryService
) : ServiceCommitResource {

    override fun getLatestCommit(
        pipelineId: String,
        elementId: String,
        repositoryId: String,
        repositoryType: RepositoryType?
    ): Result<CommitData?> {
        val data = repositoryService.getLatestCommit(pipelineId, elementId, repositoryId, repositoryType, 1, 1)
        return if (data.isNotEmpty()) {
            Result(data[0])
        } else {
            Result(0) // 第一次拉是没有记录
        }
    }

    override fun getCommitsByBuildId(buildId: String, agentId: String): Result<List<CommitResponse>> {
        return Result(repositoryService.getCommit(buildId))
    }
}