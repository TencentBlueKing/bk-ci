package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.RepositoryConfResource
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.gitci.service.GitProjectConfService
import com.tencent.devops.gitci.service.RepositoryConfService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class RepositoryConfResourceImpl @Autowired constructor(
    private val repositoryService: RepositoryConfService,
    private val gitProjectConfService: GitProjectConfService
) : RepositoryConfResource {

    override fun disableGitCI(userId: String, gitProjectId: Long): Result<Boolean> {
        checkParam(userId, gitProjectId)
        return Result(repositoryService.disableGitCI(gitProjectId))
    }

    override fun getGitCIConf(userId: String, gitProjectId: Long): Result<GitRepositoryConf?> {
        checkParam(userId, gitProjectId)
        return Result(repositoryService.getGitCIConf(gitProjectId))
    }

    override fun saveGitCIConf(userId: String, repositoryConf: GitRepositoryConf): Result<Boolean> {
        checkParam(userId, repositoryConf.gitProjectId)
        return Result(repositoryService.saveGitCIConf(userId, repositoryConf))
    }

    private fun checkParam(userId: String, gitProjectId: Long) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (!gitProjectConfService.isEnable(gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，请联系蓝盾助手")
        }
    }
}
