package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.RepositoryConfResource
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.gitci.service.RepositoryConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class RepositoryConfResourceImpl @Autowired constructor(private val repositoryService: RepositoryConfService) : RepositoryConfResource {

    override fun disableGitCI(userId: String, gitProjectId: Long): Result<Boolean> {
        checkParam(userId)
        return Result(repositoryService.disableGitCI(gitProjectId))
    }

    override fun getGitCIConf(userId: String, gitProjectId: Long): Result<GitRepositoryConf?> {
        checkParam(userId)
        return Result(repositoryService.getGitCIConf(gitProjectId))
    }

    override fun saveGitCIConf(userId: String, repositoryConf: GitRepositoryConf): Result<Boolean> {
        checkParam(userId)
        return Result(repositoryService.saveGitCIConf(userId, repositoryConf))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
