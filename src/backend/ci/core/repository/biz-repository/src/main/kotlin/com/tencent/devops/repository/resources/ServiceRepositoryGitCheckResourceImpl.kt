package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceRepositoryGitCheckResource
import com.tencent.devops.repository.pojo.RepositoryGitCheck
import com.tencent.devops.repository.service.GitCheckService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceRepositoryGitCheckResourceImpl @Autowired constructor(
    private val gitCheckService: GitCheckService
) : ServiceRepositoryGitCheckResource {
    override fun getGitCheck(
        pipelineId: String,
        commitId: String,
        context: String,
        repositoryConfig: RepositoryConfig
    ): Result<RepositoryGitCheck?> {
        return Result(gitCheckService.getGitCheck(pipelineId, repositoryConfig, commitId, context))
    }

    override fun createGitCheck(gitCheck: RepositoryGitCheck) {
        gitCheckService.creatGitCheck(gitCheck)
    }

    override fun updateGitCheck(gitCheckId: Long, buildNumber: Int) {
        gitCheckService.updateGitCheck(gitCheckId = gitCheckId, buildNumber = buildNumber)
    }
}
