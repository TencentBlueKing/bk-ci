package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ExternalCodeccRepoResource
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.service.CommonRepoFileService
import com.tencent.devops.repository.service.RepoFileService
import com.tencent.devops.scm.pojo.GitMember
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalCodeccRepoResourceImpl @Autowired constructor(
    private val repoFileService: RepoFileService,
    private val commonRepoFileService: CommonRepoFileService
) : ExternalCodeccRepoResource {
    override fun getFileContent(
        repoId: String,
        filePath: String,
        reversion: String?,
        branch: String?,
        subModule: String?,
        repositoryType:
            RepositoryType?
    ): Result<String> {
        return Result(
            repoFileService.getFileContent(
                repositoryConfig = buildConfig(repoId, repositoryType),
                filePath = filePath,
                reversion = reversion,
                branch = branch,
                subModule = subModule
            )
        )
    }

    override fun getFileContentV2(
        repoId: String,
        filePath: String,
        reversion: String?,
        branch: String?,
        subModule: String?,
        repositoryType: RepositoryType?
    ): Result<String> {
        return Result(
            repoFileService.getFileContent(
                repositoryConfig = buildConfig(repoId, repositoryType),
                filePath = filePath,
                reversion = reversion,
                branch = branch,
                subModule = subModule,
                svnFullPath = true
            )
        )
    }

    override fun getGitFileContentCommon(
        repoUrl: String,
        filePath: String,
        ref: String?,
        token: String,
        authType: RepoAuthType?,
        subModule: String?
    ): Result<String> {
        return Result(
            commonRepoFileService.getGitFileContent(
                repoUrl = repoUrl,
                filePath = filePath,
                ref = ref,
                token = token,
                authType = authType,
                subModule = subModule
            )
        )
    }

    override fun getGitFileContentOAuth(
        userId: String,
        repoName: String,
        filePath: String,
        ref: String?
    ): Result<String> {
        return commonRepoFileService.getGitFileContentOauth(
            userId = userId,
            repoName = repoName,
            filePath = filePath,
            ref = ref
        )
    }

    override fun getRepoMembers(repoUrl: String, userId: String): Result<List<GitMember>> {
        return commonRepoFileService.getGitProjectMembers(repoUrl, userId)
    }

    override fun getRepoAllMembers(repoUrl: String, userId: String): Result<List<GitMember>> {
        return commonRepoFileService.getGitProjectAllMembers(repoUrl, userId)
    }

    override fun isProjectMember(repoUrl: String, userId: String): Result<Boolean> {
        return commonRepoFileService.isProjectMember(repoUrl = repoUrl, userId = userId)
    }

    override fun getFileContentByUrl(
        projectId: String,
        repoUrl: String,
        scmType: ScmType,
        filePath: String,
        reversion: String?,
        branch: String?,
        subModule: String?,
        credentialId: String,
        scmCode: String?
    ): Result<String> {
        return Result(
            repoFileService.getFileContentByUrl(
                projectId = projectId,
                repoUrl = repoUrl,
                scmType = scmType,
                filePath = filePath,
                reversion = reversion,
                branch = branch,
                subModule = subModule,
                credentialId = credentialId,
                scmCode = scmCode
            )
        )
    }
}
