package com.tencent.devops.repository.resources.scm

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.pojo.Project
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.pojo.scm.GitRepositoryResp
import com.tencent.devops.repository.service.scm.GitService
import com.tencent.devops.scm.api.ServiceGitResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGitResourceImpl @Autowired constructor(
    private val gitService: GitService
) : ServiceGitResource {

    override fun moveProjectToGroup(
        token: String,
        groupCode: String,
        repositoryName: String,
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?> {
        return gitService.moveProjectToGroup(groupCode, repositoryName, token, tokenType)
    }

    override fun updateGitCodeRepository(
        token: String,
        projectName: String,
        updateGitProjectInfo: UpdateGitProjectInfo,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.updateGitProjectInfo(projectName, updateGitProjectInfo, token, tokenType)
    }

    override fun getProject(accessToken: String, userId: String): Result<List<Project>> {
        return Result(gitService.getProject(accessToken, userId))
    }

    override fun refreshToken(userId: String, accessToken: GitToken): Result<GitToken> {
        return Result(gitService.refreshToken(userId, accessToken))
    }

    override fun getAuthUrl(
        userId: String,
        projectId: String?,
        repoHashId: String?,
        redirectUrlType: String?
    ): Result<String> {
        return Result(gitService.getAuthUrl(userId, projectId, repoHashId, redirectUrlType))
    }

    override fun getToken(userId: String, code: String): Result<GitToken> {
        return Result(gitService.getToken(userId, code))
    }

    override fun getRedirectUrl(redirectUrlType: String?): Result<String> {
        return Result(gitService.getRedirectUrl(redirectUrlType))
    }

    override fun getGitFileContent(
        repoName: String,
        filePath: String,
        authType: RepoAuthType?,
        token: String,
        ref: String
    ): Result<String> {
        return Result(gitService.getGitFileContent(repoName, filePath, authType, token, ref))
    }

    override fun getGitlabFileContent(
        repoUrl: String,
        repoName: String,
        filePath: String,
        ref: String,
        accessToken: String
    ): Result<String> {
        return Result(
            gitService.getGitlabFileContent(
                repoUrl = repoUrl,
                repoName = repoName,
                filePath = filePath,
                ref = ref,
                accessToken = accessToken
            )
        )
    }

    override fun createGitCodeRepository(
            userId: String,
            token: String,
            repositoryName: String,
            sampleProjectPath: String,
            namespaceId: Int?,
            visibilityLevel: VisibilityLevelEnum?,
            tokenType: TokenTypeEnum
    ): Result<GitRepositoryResp?> {
        return gitService.createGitCodeRepository(userId, token, repositoryName, sampleProjectPath, namespaceId, visibilityLevel, tokenType)
    }

    override fun addGitProjectMember(
            userIdList: List<String>,
            repositorySpaceName: String,
            gitAccessLevel: GitAccessLevelEnum,
            token: String,
            tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.addGitProjectMember(userIdList, repositorySpaceName, gitAccessLevel, token, tokenType)
    }

    override fun deleteGitProjectMember(
            userIdList: List<String>,
            repositorySpaceName: String,
            token: String,
            tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return gitService.deleteGitProjectMember(userIdList, repositorySpaceName, token, tokenType)
    }
}