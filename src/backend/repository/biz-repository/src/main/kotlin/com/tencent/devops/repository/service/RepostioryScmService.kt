package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.repository.pojo.Project
import com.tencent.devops.repository.pojo.enums.*
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.TokenCheckResult
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*

interface RepostioryScmService {

    fun getProject(
            accessToken: String,
            userId: String
    ): List<Project>

    fun getAuthUrl(
            authParamJsonStr: String
    ): String

    fun getToken(
            userId: String,
            code: String
    ): GitToken

    @ApiOperation("获取转发地址")
    fun getRedirectUrl(
            redirectUrlType: String
    ): String

    @ApiOperation("刷新用户的token")
    fun refreshToken(
            userId: String,
            accessToken: GitToken
    ): GitToken

    @ApiOperation("获取文件内容")
    fun getSvnFileContent(
            url: String,
            userId: String,
            svnType: String,
            filePath: String,
            reversion: Long,
            credential1: String,
            credential2: String? = null
    ): String

    @ApiOperation("获取git文件内容")
    fun getGitFileContent(
            repoName: String,
            filePath: String,
            authType: RepoAuthType?,
            token: String,
            ref: String
    ): String

    @ApiOperation("获取gitlab文件内容")
    fun getGitlabFileContent(
            repoUrl: String,
            repoName: String,
            filePath: String,
            ref: String,
            accessToken: String
    ): String

    @ApiOperation("lock svn")
    fun unlock(
            projectName: String,
            url: String,
            type: ScmType,
            region: CodeSvnRegion?,
            userName: String
    ): Boolean

    @ApiOperation("lock svn")
    fun lock(
            projectName: String,
            url: String,
            type: ScmType,
            region: CodeSvnRegion?,
            userName: String
    ): Boolean

    @ApiOperation("把项目迁移到指定项目组下")
    fun moveProjectToGroup(
            token: String,
            groupCode: String,
            repositoryName: String,
            tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?>

    @ApiOperation("更新git代码库信息")
    fun updateGitCodeRepository(
            token: String,
            projectName: String,
            updateGitProjectInfo: UpdateGitProjectInfo,
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("创建git代码库")
    fun createGitCodeRepository(
            userId: String,
            token: String,
            repositoryName: String,
            sampleProjectPath: String?,
            namespaceId: Int?,
            visibilityLevel: VisibilityLevelEnum?,
            tokenType: TokenTypeEnum
    ): Result<GitRepositoryResp?>

    @ApiOperation("为项目成员赋予代码库权限")
    fun addGitProjectMember(
            userIdList: List<String>,
            repositorySpaceName: String,
            gitAccessLevel: GitAccessLevelEnum,
            token: String,
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("删除项目成员的代码库权限")
    fun deleteGitProjectMember(
            userIdList: List<String>,
            repositorySpaceName: String,
            token: String,
            tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("Check if the svn private key and passphrase legal")
    fun checkPrivateKeyAndToken(
            projectName: String,
            url: String,
            type: ScmType,
            privateKey: String?,
            passPhrase: String?,
            token: String?,
            region: CodeSvnRegion?,
            userName: String
    ): Result<TokenCheckResult>

    @ApiOperation("Check if the svn private key and passphrase legal")
    fun checkUsernameAndPassword(
            projectName: String,
            @ApiParam("仓库地址", required = true)
            url: String,
            type: ScmType,
            username: String,
            password: String,
            token: String,
            region: CodeSvnRegion?,
            repoUsername: String
    ): Result<TokenCheckResult>
}