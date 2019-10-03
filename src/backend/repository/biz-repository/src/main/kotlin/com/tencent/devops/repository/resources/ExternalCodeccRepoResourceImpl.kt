package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ExternalCodeccRepoResource
import com.tencent.devops.repository.service.RepoFileService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalCodeccRepoResourceImpl @Autowired constructor(
    private val repoFileService: RepoFileService
) : ExternalCodeccRepoResource {
    override fun getFileContent(repoId: String, filePath: String, reversion: String?, branch: String?, subModule: String?, repositoryType: RepositoryType?): Result<String> {
        return Result(repoFileService.getFileContent(buildConfig(repoId, repositoryType), filePath, reversion, branch, subModule))
    }
}