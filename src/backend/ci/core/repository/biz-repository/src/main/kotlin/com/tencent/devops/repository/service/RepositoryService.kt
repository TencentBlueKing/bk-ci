/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.RepositoryMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.dao.RepositoryCodeGitDao
import com.tencent.devops.repository.dao.RepositoryCodeGitLabDao
import com.tencent.devops.repository.dao.RepositoryCodeP4Dao
import com.tencent.devops.repository.dao.RepositoryCodeSvnDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.dao.RepositoryGithubDao
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitProjectInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.scm.IGitService
import com.tencent.devops.repository.service.scm.IScmService
import com.tencent.devops.repository.utils.CredentialUtils
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Base64
import javax.ws.rs.NotFoundException

@Service
@Suppress("ALL")
class RepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryCodeSvnDao: RepositoryCodeSvnDao,
    private val repositoryCodeGitDao: RepositoryCodeGitDao,
    private val repositoryCodeGitLabDao: RepositoryCodeGitLabDao,
    private val repositoryGithubDao: RepositoryGithubDao,
    private val repositoryCodeP4Dao: RepositoryCodeP4Dao,
    private val gitOauthService: IGitOauthService,
    private val gitService: IGitService,
    private val scmService: IScmService,
    private val dslContext: DSLContext,
    private val client: Client,
    private val repositoryPermissionService: RepositoryPermissionService
) {

    @Value("\${repository.git.devopsPrivateToken}")
    private lateinit var devopsPrivateToken: String

    @Value("\${repository.git.devopsGroupName}")
    private lateinit var devopsGroupName: String

    fun hasAliasName(projectId: String, repositoryHashId: String?, aliasName: String): Boolean {
        val repositoryId = if (repositoryHashId != null) HashUtil.decodeOtherIdToLong(repositoryHashId) else 0L
        if (repositoryId != 0L) {
            val record = repositoryDao.get(dslContext = dslContext, repositoryId = repositoryId, projectId = projectId)
            if (record.aliasName == aliasName) return false
        }
        return repositoryDao.countByProjectAndAliasName(
            dslContext = dslContext,
            projectId = projectId,
            excludeRepositoryId = repositoryId,
            aliasName = aliasName
        ) != 0L
    }

    fun createGitCodeRepository(
        userId: String,
        projectCode: String?,
        repositoryName: String,
        sampleProjectPath: String?,
        namespaceId: Int?,
        visibilityLevel: VisibilityLevelEnum?,
        tokenType: TokenTypeEnum,
        frontendType: FrontendTypeEnum?
    ): Result<RepositoryInfo?> {
        val getGitTokenResult = getGitToken(tokenType, userId)
        if (getGitTokenResult.isNotOk()) {
            return Result(status = getGitTokenResult.status, message = getGitTokenResult.message ?: "")
        }
        val token = getGitTokenResult.data!!
        val gitRepositoryRespResult: Result<GitRepositoryResp?>
        val gitRepositoryResp: GitRepositoryResp?
        try {
            gitRepositoryRespResult = gitService.createGitCodeRepository(
                userId = userId,
                token = token,
                repositoryName = repositoryName,
                sampleProjectPath = sampleProjectPath,
                namespaceId = namespaceId,
                visibilityLevel = visibilityLevel,
                tokenType = tokenType,
                frontendType = frontendType
            )
            logger.info("createGitCodeRepository gitRepositoryRespResult is :$gitRepositoryRespResult")
            if (gitRepositoryRespResult.isOk()) {
                gitRepositoryResp = gitRepositoryRespResult.data
            } else {
                return Result(gitRepositoryRespResult.status, gitRepositoryRespResult.message ?: "")
            }
        } catch (e: Exception) {
            logger.error("createGitCodeRepository error is :$e", e)
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
        logger.info("gitRepositoryResp>> $gitRepositoryResp")
        return if (null != gitRepositoryResp) {
            val codeGitRepository = CodeGitRepository(
                aliasName = gitRepositoryResp.name,
                url = gitRepositoryResp.repositoryUrl,
                credentialId = "",
                projectName = gitRepositoryResp.name,
                userName = userId,
                authType = RepoAuthType.OAUTH,
                projectId = projectCode,
                repoHashId = null
            )

            // 关联代码库
            val repositoryHashId =
                if (null != projectCode) {
                    serviceCreate(userId = userId, projectId = projectCode, repository = codeGitRepository)
                } else null
            logger.info("serviceCreate result>> $repositoryHashId")
            Result(
                RepositoryInfo(
                    repositoryId = null,
                    repositoryHashId = repositoryHashId,
                    aliasName = gitRepositoryResp.name,
                    url = gitRepositoryResp.repositoryUrl,
                    type = ScmType.CODE_GIT,
                    updatedTime = LocalDateTime.now().timestampmilli()
                )
            )
        } else {
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
    }

    private fun getGitToken(tokenType: TokenTypeEnum, userId: String): Result<String?> {
        val token = if (TokenTypeEnum.OAUTH == tokenType) {
            val gitToken = gitOauthService.getAccessToken(userId)
            logger.info("gitToken>> $gitToken")
            if (null == gitToken) {
                // 抛出无效的token提示
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.OAUTH_TOKEN_IS_INVALID)
            }
            gitToken.accessToken
        } else {
            devopsPrivateToken
        }
        return Result(token)
    }

    fun updateGitCodeRepository(
        userId: String,
        repositoryConfig: RepositoryConfig,
        updateGitProjectInfo: UpdateGitProjectInfo,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        val repo = serviceGet("", repositoryConfig)
        logger.info("the repo is:$repo")
        val projectName = repo.projectName
        val finalTokenType = generateFinalTokenType(tokenType, projectName)
        return updateGitRepositoryInfo(finalTokenType, projectName, userId, updateGitProjectInfo)
    }

    fun updateGitCodeRepository(
        userId: String,
        projectName: String,
        updateGitProjectInfo: UpdateGitProjectInfo,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        return updateGitRepositoryInfo(tokenType, projectName, userId, updateGitProjectInfo)
    }

    private fun updateGitRepositoryInfo(
        tokenType: TokenTypeEnum,
        projectName: String,
        userId: String,
        updateGitProjectInfo: UpdateGitProjectInfo
    ): Result<Boolean> {
        val getGitTokenResult = getGitToken(tokenType, userId)
        if (getGitTokenResult.isNotOk()) {
            return Result(status = getGitTokenResult.status, message = getGitTokenResult.message ?: "")
        }
        val token = getGitTokenResult.data!!
        return try {
            val gitRepositoryRespResult = gitService.updateGitProjectInfo(
                projectName = projectName,
                updateGitProjectInfo = updateGitProjectInfo,
                token = token,
                tokenType = tokenType
            )
            logger.info("updateGitCodeRepository gitRepositoryRespResult is :$gitRepositoryRespResult")
            if (gitRepositoryRespResult.isOk()) {
                Result(true)
            } else {
                Result(gitRepositoryRespResult.status, gitRepositoryRespResult.message ?: "", false)
            }
        } catch (e: Exception) {
            logger.error("updateGitCodeRepository error is :$e", e)
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
    }

    fun getGitRepositoryTreeInfo(
        userId: String,
        repositoryConfig: RepositoryConfig,
        refName: String?,
        path: String?,
        tokenType: TokenTypeEnum
    ): Result<List<GitRepositoryDirItem>?> {
        val repo: CodeGitRepository = serviceGet("", repositoryConfig) as CodeGitRepository
        val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
        val getGitTokenResult = getGitToken(finalTokenType, userId)
        if (getGitTokenResult.isNotOk()) {
            return Result(status = getGitTokenResult.status, message = getGitTokenResult.message, data = null)
        }
        val token = getGitTokenResult.data!!
        return try {
            val getGitRepositoryTreeInfoResult = gitService.getGitRepositoryTreeInfo(
                userId = userId,
                repoName = repo.projectName,
                refName = null,
                path = null,
                token = token,
                tokenType = tokenType
            )
            getGitRepositoryTreeInfoResult
        } catch (e: Exception) {
            logger.error("getGitRepositoryTreeInfo error is :$e", e)
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
    }

    fun addGitProjectMember(
        userId: String,
        userIdList: List<String>,
        repositoryConfig: RepositoryConfig,
        gitAccessLevel: GitAccessLevelEnum,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        val repo: CodeGitRepository =
            serviceGet(projectId = "", repositoryConfig = repositoryConfig) as CodeGitRepository
        logger.info("the repo is:$repo")
        val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
        val getGitTokenResult = getGitToken(finalTokenType, userId)
        if (getGitTokenResult.isNotOk()) {
            return Result(status = getGitTokenResult.status, message = getGitTokenResult.message, data = false)
        }
        val token = getGitTokenResult.data!!
        val addGitProjectMemberResult = gitService.addGitProjectMember(
            userIdList = userIdList,
            repoName = repo.projectName,
            gitAccessLevel = gitAccessLevel,
            token = token,
            tokenType = finalTokenType
        )
        logger.info("addGitProjectMemberResult is :$addGitProjectMemberResult")
        if (addGitProjectMemberResult.isNotOk()) {
            return Result(
                status = addGitProjectMemberResult.status,
                message = addGitProjectMemberResult.message,
                data = false
            )
        }
        return Result(true)
    }

    fun deleteGitProjectMember(
        userId: String,
        userIdList: List<String>,
        repositoryConfig: RepositoryConfig,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        logger.info("deleteGitProjectMember userId is:$userId,userIdList is:$userIdList")
        logger.info("deleteGitProjectMember repositoryConfig is:$repositoryConfig,tokenType is:$tokenType")
        val repo: CodeGitRepository = serviceGet("", repositoryConfig) as CodeGitRepository
        logger.info("the repo is:$repo")
        val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
        val getGitTokenResult = getGitToken(finalTokenType, userId)
        if (getGitTokenResult.isNotOk()) {
            return Result(status = getGitTokenResult.status, message = getGitTokenResult.message, data = false)
        }
        val token = getGitTokenResult.data!!
        val deleteGitProjectMemberResult = gitService.deleteGitProjectMember(
            userIdList = userIdList,
            repoName = repo.projectName,
            token = token,
            tokenType = finalTokenType
        )
        logger.info("deleteGitProjectMemberResult is :$deleteGitProjectMemberResult")
        if (deleteGitProjectMemberResult.isNotOk()) {
            return Result(
                status = deleteGitProjectMemberResult.status,
                message = deleteGitProjectMemberResult.message,
                data = false
            )
        }
        return Result(true)
    }

    fun deleteGitProject(
        userId: String,
        repositoryConfig: RepositoryConfig,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        val repo: CodeGitRepository = serviceGet("", repositoryConfig) as CodeGitRepository
        val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
        val getGitTokenResult = getGitToken(finalTokenType, userId)
        if (getGitTokenResult.isNotOk()) {
            return Result(status = getGitTokenResult.status, message = getGitTokenResult.message, data = false)
        }
        val token = getGitTokenResult.data!!
        val deleteGitProjectResult = gitService.deleteGitProject(
            repoName = repo.projectName,
            token = token,
            tokenType = finalTokenType
        )
        logger.info("deleteGitProjectResult is :$deleteGitProjectResult")
        if (deleteGitProjectResult.isNotOk()) {
            return Result(
                status = deleteGitProjectResult.status,
                message = deleteGitProjectResult.message,
                data = false
            )
        }
        return Result(true)
    }

    fun moveGitProjectToGroup(
        userId: String,
        groupCode: String?,
        repositoryConfig: RepositoryConfig,
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?> {
        val repo: CodeGitRepository = serviceGet("", repositoryConfig) as CodeGitRepository
        val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
        val getGitTokenResult = getGitToken(finalTokenType, userId)
        if (getGitTokenResult.isNotOk()) {
            return Result(status = getGitTokenResult.status, message = getGitTokenResult.message ?: "")
        }
        val token = getGitTokenResult.data!!
        val moveProjectToGroupResult: Result<GitProjectInfo?>
        return try {
            moveProjectToGroupResult = gitService.moveProjectToGroup(
                groupCode = groupCode ?: devopsGroupName,
                repoName = repo.projectName,
                token = token,
                tokenType = finalTokenType
            )
            logger.info("moveProjectToGroupResult is :$moveProjectToGroupResult")
            if (moveProjectToGroupResult.isOk()) {
                val gitProjectInfo = moveProjectToGroupResult.data!!
                val repositoryId = HashUtil.decodeOtherIdToLong(repo.repoHashId!!)
                dslContext.transaction { t ->
                    val context = DSL.using(t)
                    repositoryDao.edit(
                        dslContext = context,
                        repositoryId = repositoryId,
                        aliasName = gitProjectInfo.namespaceName,
                        url = gitProjectInfo.repositoryUrl
                    )
                    repositoryCodeGitDao.edit(
                        dslContext = context,
                        repositoryId = repositoryId,
                        projectName = gitProjectInfo.namespaceName,
                        userName = repo.userName,
                        credentialId = repo.credentialId,
                        authType = repo.authType
                    )
                }
                Result(gitProjectInfo)
            } else {
                Result(moveProjectToGroupResult.status, moveProjectToGroupResult.message ?: "")
            }
        } catch (e: Exception) {
            logger.error("moveProjectToGroupResult error is :$e", e)
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
    }

    private fun generateFinalTokenType(tokenType: TokenTypeEnum, repoProjectName: String): TokenTypeEnum {
        // 兼容历史插件的代码库不在公共group下的情况，历史插件的代码库信息更新要用用户的token更新
        var finalTokenType = tokenType
        if (!repoProjectName.startsWith(devopsGroupName) && !repoProjectName
            .contains("bkdevops-extension-service", true)
        ) {
            finalTokenType = TokenTypeEnum.OAUTH
        }
        return finalTokenType
    }

    fun userCreate(userId: String, projectId: String, repository: Repository): String {
        // 指定oauth的用户名字只能是登录用户。
        repository.userName = userId
        validatePermission(userId, projectId, AuthPermission.CREATE, "用户($userId)在工程($projectId)下没有代码库创建权限")
        val repositoryId = createRepository(repository, projectId, userId)
        return HashUtil.encodeOtherLongId(repositoryId)
    }

    fun serviceCreate(userId: String, projectId: String, repository: Repository): String {
        val repositoryId = createRepository(repository, projectId, userId)
        return HashUtil.encodeOtherLongId(repositoryId)
    }

    private fun createRepository(
        repository: Repository,
        projectId: String,
        userId: String
    ): Long {
        if (!repository.isLegal()) {
            logger.warn("The repository($repository) is illegal")
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.REPO_PATH_WRONG_PARM,
                params = arrayOf(repository.getStartPrefix())
            )
        }

        if (hasAliasName(projectId, null, repository.aliasName)) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.REPO_NAME_EXIST,
                params = arrayOf(repository.aliasName)
            )
        }
        val repositoryType = getRepositoryType(repository)
        // 仓库凭证
        var token: String = StringUtils.EMPTY
        if (repositoryType != null && repositoryType.first == RepoAuthType.OAUTH) {
            token = gitOauthService.getAccessToken(userId)?.accessToken ?: StringUtils.EMPTY
        }
        if (needToCheckToken(repository)) {
            /**
             * tGit 类型，去除凭据验证
             *
             * 2022/2/10 tgit类型验证凭证，并且验证失败时返回提示信息
             */
            if (repository !is GithubRepository) {
                token = checkRepositoryToken(projectId, repository)
            }
        }
        // Git项目ID
        val gitProjectId = getGitProjectId(repo = repository, token = token)
        val repositoryId = dslContext.transactionResult { configuration ->
            val transactionContext = DSL.using(configuration)
            val repositoryId = when (repository) {
                is CodeSvnRepository -> {
                    val repositoryId = repositoryDao.create(
                        dslContext = transactionContext,
                        projectId = projectId,
                        userId = userId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL(),
                        type = ScmType.CODE_SVN
                    )
                    // 如果repository为null，则默认为TC
                    repositoryCodeSvnDao.create(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        region = repository.region ?: CodeSvnRegion.TC,
                        projectName = repository.projectName,
                        userName = repository.userName,
                        privateToken = repository.credentialId,
                        svnType = repository.svnType
                    )
                    repositoryId
                }
                is CodeGitRepository -> {
                    val repositoryId = repositoryDao.create(
                        dslContext = transactionContext,
                        projectId = projectId,
                        userId = userId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL(),
                        type = ScmType.CODE_GIT
                    )
                    repositoryCodeGitDao.create(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        projectName = GitUtils.getProjectName(repository.url),
                        userName = repository.userName,
                        credentialId = repository.credentialId,
                        authType = repository.authType,
                        gitProjectId = gitProjectId.toString()
                    )
                    repositoryId
                }
                is CodeTGitRepository -> {
                    val repositoryId = repositoryDao.create(
                        dslContext = transactionContext,
                        projectId = projectId,
                        userId = userId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL(),
                        type = ScmType.CODE_TGIT
                    )
                    repositoryCodeGitDao.create(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        projectName = GitUtils.getProjectName(repository.url),
                        userName = repository.userName,
                        credentialId = repository.credentialId,
                        authType = repository.authType,
                        gitProjectId = gitProjectId.toString()
                    )
                    repositoryId
                }
                is CodeGitlabRepository -> {
                    val repositoryId = repositoryDao.create(
                        dslContext = transactionContext,
                        projectId = projectId,
                        userId = userId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL(),
                        type = ScmType.CODE_GITLAB
                    )
                    repositoryCodeGitLabDao.create(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        projectName = GitUtils.getProjectName(repository.url),
                        userName = repository.userName,
                        privateToken = repository.credentialId,
                        authType = repository.authType,
                        gitProjectId = gitProjectId.toString()
                    )
                    repositoryId
                }
                is GithubRepository -> {
                    val repositoryId = repositoryDao.create(
                        dslContext = transactionContext,
                        projectId = projectId,
                        userId = userId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL(),
                        type = ScmType.GITHUB
                    )
                    repositoryGithubDao.create(dslContext, repositoryId, repository.projectName, userId)
                    repositoryId
                }
                is CodeP4Repository -> {
                    val repositoryId = repositoryDao.create(
                        dslContext = transactionContext,
                        projectId = projectId,
                        userId = userId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL(),
                        type = ScmType.CODE_P4
                    )
                    repositoryCodeP4Dao.create(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        projectName = repository.url,
                        userName = repository.userName,
                        credentialId = repository.credentialId
                    )
                    repositoryId
                }
                else -> throw IllegalArgumentException("Unknown repository type")
            }
            repositoryId
        }

        createResource(userId, projectId, repositoryId, repository.aliasName)
        return repositoryId
    }

    fun userGet(userId: String, projectId: String, repositoryConfig: RepositoryConfig): Repository {
        val repository = getRepository(projectId, repositoryConfig)

        val repositoryId = repository.repositoryId
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.VIEW,
            message = MessageCodeUtil.generateResponseDataObject<String>(
                RepositoryMessageCode.USER_VIEW_PEM_ERROR,
                arrayOf(userId, projectId, repositoryConfig.getRepositoryId())
            ).message!!
        )
        return compose(repository)
    }

    fun serviceGet(projectId: String, repositoryConfig: RepositoryConfig): Repository {
        return compose(getRepository(projectId, repositoryConfig))
    }

    private fun getRepository(projectId: String, repositoryConfig: RepositoryConfig): TRepositoryRecord {
        logger.info("[$projectId]Start to get the repository - ($repositoryConfig)")
        return when (repositoryConfig.repositoryType) {
            RepositoryType.ID -> {
                val repositoryId = HashUtil.decodeOtherIdToLong(repositoryConfig.getRepositoryId())
                repositoryDao.get(dslContext, repositoryId, projectId)
            }
            RepositoryType.NAME -> {
                repositoryDao.getByName(dslContext, projectId, repositoryConfig.getRepositoryId())
            }
        }
    }

    private fun compose(repository: TRepositoryRecord): Repository {
        val repositoryId = repository.repositoryId
        val hashId = HashUtil.encodeOtherLongId(repository.repositoryId)
        return when (repository.type) {
            ScmType.CODE_SVN.name -> {
                val record = repositoryCodeSvnDao.get(dslContext, repositoryId)
                CodeSvnRepository(
                    aliasName = repository.aliasName,
                    url = repository.url,
                    credentialId = record.credentialId,
                    region = if (record.region.isNullOrBlank()) {
                        CodeSvnRegion.TC
                    } else {
                        CodeSvnRegion.valueOf(record.region)
                    },
                    projectName = record.projectName,
                    userName = record.userName,
                    projectId = repository.projectId,
                    repoHashId = hashId,
                    svnType = record.svnType
                )
            }
            ScmType.CODE_GIT.name -> {
                val record = repositoryCodeGitDao.get(dslContext, repositoryId)
                CodeGitRepository(
                    aliasName = repository.aliasName,
                    url = repository.url,
                    credentialId = record.credentialId,
                    projectName = record.projectName,
                    userName = record.userName,
                    authType = RepoAuthType.parse(record.authType),
                    projectId = repository.projectId,
                    repoHashId = HashUtil.encodeOtherLongId(repository.repositoryId)
                )
            }
            ScmType.CODE_TGIT.name -> {
                val record = repositoryCodeGitDao.get(dslContext, repositoryId)
                CodeTGitRepository(
                    aliasName = repository.aliasName,
                    url = repository.url,
                    credentialId = record.credentialId,
                    projectName = record.projectName,
                    userName = record.userName,
                    authType = RepoAuthType.parse(record.authType),
                    projectId = repository.projectId,
                    repoHashId = hashId

                )
            }
            ScmType.CODE_GITLAB.name -> {
                val record = repositoryCodeGitLabDao.get(dslContext, repositoryId)
                CodeGitlabRepository(
                    aliasName = repository.aliasName,
                    url = repository.url,
                    credentialId = record.credentialId,
                    projectName = record.projectName,
                    userName = record.userName,
                    projectId = repository.projectId,
                    repoHashId = hashId
                )
            }
            ScmType.GITHUB.name -> {
                val record = repositoryGithubDao.get(dslContext, repositoryId)
                GithubRepository(
                    aliasName = repository.aliasName,
                    url = repository.url,
                    userName = repository.userId,
                    projectName = record.projectName,
                    projectId = repository.projectId,
                    repoHashId = hashId
                )
            }
            ScmType.CODE_P4.name -> {
                val record = repositoryCodeP4Dao.get(dslContext, repositoryId)
                CodeP4Repository(
                    aliasName = repository.aliasName,
                    url = repository.url,
                    credentialId = record.credentialId,
                    projectName = record.projectName,
                    userName = record.userName,
                    projectId = repository.projectId,
                    repoHashId = hashId
                )
            }
            else -> throw IllegalArgumentException("Unknown repository type")
        }
    }

    fun userEdit(userId: String, projectId: String, repositoryHashId: String, repository: Repository) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryHashId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        if (repository.aliasName.isBlank()) {
            throw ParamBlankException("Invalid repository aliasName")
        }
        if (repository.url.isBlank()) {
            throw ParamBlankException("Invalid repository url")
        }
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageCodeUtil.generateResponseDataObject<String>(
                RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                arrayOf(userId, projectId, repositoryHashId)
            ).message!!
        )
        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }

        if (!repository.isLegal()) {
            logger.warn("The repository($repository) is illegal")
            throw OperationException(
                MessageCodeUtil.generateResponseDataObject<String>(
                    RepositoryMessageCode.REPO_PATH_WRONG_PARM,
                    arrayOf(repository.getStartPrefix())
                ).message!!
            )
        }

        if (hasAliasName(projectId, repositoryHashId, repository.aliasName)) {
            throw OperationException(
                MessageCodeUtil.generateResponseDataObject<String>(
                    RepositoryMessageCode.REPO_NAME_EXIST,
                    arrayOf(repository.aliasName)
                ).message!!
            )
        }

        if (needToCheckToken(repository)) {
            /**
             * 类型为tGit,去掉凭据验证
             */
            if (repository !is GithubRepository) {
                checkRepositoryToken(projectId, repository)
            }
        }
        // 判断仓库类型是否一致
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            when (record.type) {
                ScmType.CODE_GIT.name -> {
                    if (repository !is CodeGitRepository) {
                        throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.GIT_INVALID))
                    }
                    repositoryDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL()
                    )
                    repositoryCodeGitDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        projectName = GitUtils.getProjectName(repository.url),
                        userName = repository.userName,
                        credentialId = repository.credentialId,
                        authType = repository.authType
                    )
                }
                ScmType.CODE_TGIT.name -> {
                    if (repository !is CodeTGitRepository) {
                        throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.TGIT_INVALID))
                    }
                    repositoryDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL()
                    )
                    repositoryCodeGitDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        projectName = GitUtils.getProjectName(repository.url),
                        userName = repository.userName,
                        credentialId = repository.credentialId,
                        authType = repository.authType
                    )
                }
                ScmType.CODE_SVN.name -> {
                    if (repository !is CodeSvnRepository) {
                        throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.SVN_INVALID))
                    }
                    repositoryDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL()
                    )
                    repositoryCodeSvnDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        region = repository.region ?: CodeSvnRegion.TC,
                        projectName = repository.projectName,
                        userName = repository.userName,
                        credentialId = repository.credentialId,
                        svnType = repository.svnType
                    )
                }
                ScmType.CODE_GITLAB.name -> {
                    if (repository !is CodeGitlabRepository) {
                        throw OperationException(
                            message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.GITLAB_INVALID)
                        )
                    }
                    repositoryDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL()
                    )
                    repositoryCodeGitLabDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        projectName = GitUtils.getProjectName(repository.url),
                        userName = repository.userName,
                        credentialId = repository.credentialId
                    )
                }
                ScmType.GITHUB.name -> {
                    if (repository !is GithubRepository) {
                        throw OperationException(
                            message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.GITHUB_INVALID)
                        )
                    }
                    repositoryDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL()
                    )
                    repositoryGithubDao.edit(dslContext, repositoryId, repository.projectName, repository.userName)
                }
                ScmType.CODE_P4.name -> {
                    if (repository !is CodeP4Repository) {
                        throw OperationException(
                            message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.P4_INVALID)
                        )
                    }
                    repositoryDao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        aliasName = repository.aliasName,
                        url = repository.getFormatURL()
                    )
                    repositoryCodeP4Dao.edit(
                        dslContext = transactionContext,
                        repositoryId = repositoryId,
                        projectName = repository.url,
                        userName = repository.userName,
                        credentialId = repository.credentialId
                    )
                }
            }
        }
        editResource(projectId, repositoryId, repository.aliasName)
    }

    fun serviceList(
        projectId: String,
        scmType: ScmType?
    ): List<RepositoryInfoWithPermission> {
        val dbRecords = repositoryDao.listByProject(dslContext, projectId, scmType)
        val gitRepoIds = dbRecords.filter { it.type == "CODE_GIT" }.map { it.repositoryId }.toSet()
        val gitAuthMap = repositoryCodeGitDao.list(dslContext, gitRepoIds)?.map { it.repositoryId to it }?.toMap()

        return dbRecords.map { repository ->
            val authType = gitAuthMap?.get(repository.repositoryId)?.authType ?: "SSH"
            RepositoryInfoWithPermission(
                repositoryHashId = HashUtil.encodeOtherLongId(repository.repositoryId),
                aliasName = repository.aliasName,
                url = repository.url,
                type = ScmType.valueOf(repository.type),
                updatedTime = repository.updatedTime.timestamp(),
                canEdit = true,
                canDelete = true,
                authType = authType
            )
        }.toList()
    }

    fun serviceCount(
        projectId: Set<String>,
        repositoryHashId: String,
        repositoryType: ScmType?,
        aliasName: String
    ): Long {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        val repoIds = if (repositoryId == 0L) setOf() else setOf(repositoryId)
        return repositoryDao.count(dslContext, projectId, repositoryType, repoIds, aliasName)
    }

    fun userList(
        userId: String,
        projectId: String,
        repositoryType: ScmType?,
        aliasName: String?,
        offset: Int,
        limit: Int,
        sortBy: String? = null,
        sortType: String? = null
    ): Pair<SQLPage<RepositoryInfoWithPermission>, Boolean> {
        // 校验权限
        val hasCreatePermission = validatePermission(userId, projectId, AuthPermission.CREATE)
        val permissionToListMap = repositoryPermissionService.filterRepositories(
            userId = userId,
            projectId = projectId,
            authPermissions = setOf(AuthPermission.LIST, AuthPermission.EDIT, AuthPermission.DELETE)
        )
        val hasListPermissionRepoList = permissionToListMap[AuthPermission.LIST]!!
        val hasEditPermissionRepoList = permissionToListMap[AuthPermission.EDIT]!!
        val hasDeletePermissionRepoList = permissionToListMap[AuthPermission.DELETE]!!

        val count =
            repositoryDao.countByProject(
                dslContext = dslContext,
                projectIds = setOf(projectId),
                repositoryTypes = repositoryType?.let { listOf(it) },
                aliasName = aliasName,
                repositoryIds = hasListPermissionRepoList.toSet()
            )
        val repositoryRecordList = repositoryDao.listByProject(
            dslContext = dslContext,
            projectId = projectId,
            repositoryTypes = repositoryType?.let { listOf(it) },
            aliasName = aliasName,
            repositoryIds = hasListPermissionRepoList.toSet(),
            offset = offset,
            limit = limit,
            sortBy = sortBy,
            sortType = sortType
        )
        val gitRepoIds =
            repositoryRecordList.filter {
                it.type == ScmType.CODE_GIT.name ||
                    it.type == ScmType.CODE_TGIT.name
            }.map { it.repositoryId }.toSet()
        val gitAuthMap =
            repositoryCodeGitDao.list(dslContext, gitRepoIds)?.map { it.repositoryId to it }?.toMap()

        val gitlabRepoIds =
            repositoryRecordList.filter { it.type == ScmType.CODE_GITLAB.name }
                .map { it.repositoryId }.toSet()
        val gitlabAuthMap =
            repositoryCodeGitLabDao.list(dslContext, gitlabRepoIds)?.map { it.repositoryId to it }?.toMap()

        val svnRepoIds =
            repositoryRecordList.filter { it.type == ScmType.CODE_SVN.name }
                .map { it.repositoryId }.toSet()
        val svnRepoRecords =
            repositoryCodeSvnDao.list(dslContext, svnRepoIds)
                .map { it.repositoryId to it }.toMap()

        val p4RepoIds = repositoryRecordList.filter { it.type == ScmType.CODE_P4.name }
            .map { it.repositoryId }.toSet()
        val p4RepoAuthMap = repositoryCodeP4Dao.list(dslContext, p4RepoIds)?.map { it.repositoryId to it }?.toMap()

        val repositoryList = repositoryRecordList.map {
            val hasEditPermission = hasEditPermissionRepoList.contains(it.repositoryId)
            val hasDeletePermission = hasDeletePermissionRepoList.contains(it.repositoryId)
            val (authType, authIdentity: String?) = when (it.type) {
                ScmType.GITHUB.name ->
                    RepoAuthType.OAUTH.name to it.userId
                ScmType.CODE_SVN.name -> {
                    val svnRepo = svnRepoRecords[it.repositoryId]
                    (svnRepo?.svnType?.toUpperCase() ?: RepoAuthType.SSH.name) to svnRepo?.credentialId
                }
                ScmType.CODE_GITLAB.name -> {
                    val gitlabRepo = gitlabAuthMap?.get(it.repositoryId)
                    val gitlabAuthType = gitlabRepo?.authType ?: RepoAuthType.HTTP.name
                    gitlabAuthType to gitlabRepo?.credentialId
                }
                ScmType.CODE_P4.name -> {
                    RepoAuthType.HTTP.name to p4RepoAuthMap?.get(it.repositoryId)?.credentialId
                }
                else -> {
                    val gitRepo = gitAuthMap?.get(it.repositoryId)
                    val gitAuthType = gitRepo?.authType ?: RepoAuthType.SSH.name
                    val gitAuthIdentity = if (gitAuthType == RepoAuthType.OAUTH.name) {
                        gitRepo?.userName
                    } else {
                        gitRepo?.credentialId
                    }
                    gitAuthType to gitAuthIdentity
                }
            }
            val svnType = svnRepoRecords[it.repositoryId]?.svnType
            RepositoryInfoWithPermission(
                repositoryHashId = HashUtil.encodeOtherLongId(it.repositoryId),
                aliasName = it.aliasName,
                url = it.url,
                type = ScmType.valueOf(it.type),
                updatedTime = it.updatedTime.timestamp(),
                canEdit = hasEditPermission,
                canDelete = hasDeletePermission,
                authType = authType,
                svnType = svnType,
                authIdentity = authIdentity
            )
        }
        return Pair(SQLPage(count, repositoryList), hasCreatePermission)
    }

    fun hasPermissionList(
        userId: String,
        projectId: String,
        repositoryType: String?,
        authPermission: AuthPermission,
        offset: Int,
        limit: Int,
        aliasName: String? = null
    ): SQLPage<RepositoryInfo> {
        val hasPermissionList = repositoryPermissionService.filterRepository(userId, projectId, authPermission)
        val repositoryTypes = repositoryType?.split(",")?.map { ScmType.valueOf(it) }

        val count = repositoryDao.countByProject(
            dslContext = dslContext,
            projectIds = setOf(projectId),
            repositoryTypes = repositoryTypes,
            aliasName = aliasName,
            repositoryIds = hasPermissionList.toSet()
        )
        val repositoryRecordList =
            repositoryDao.listByProject(
                dslContext = dslContext,
                projectId = projectId,
                repositoryTypes = repositoryTypes,
                aliasName = aliasName,
                repositoryIds = hasPermissionList.toSet(),
                offset = offset,
                limit = limit
            )
        val repositoryList = repositoryRecordList.map {
            RepositoryInfo(
                repositoryId = null,
                repositoryHashId = HashUtil.encodeOtherLongId(it.repositoryId),
                aliasName = it.aliasName,
                url = it.url,
                type = ScmType.valueOf(it.type),
                updatedTime = it.updatedTime.timestamp()
            )
        }
        return SQLPage(count, repositoryList)
    }

    fun listByProject(
        projectIds: Collection<String>,
        repositoryType: ScmType?,
        offset: Int,
        limit: Int
    ): SQLPage<RepositoryInfo> {

        val count = repositoryDao.countByProject(
            dslContext = dslContext,
            projectIds = projectIds,
            repositoryTypes = repositoryType?.let { listOf(it) },
            aliasName = null,
            repositoryIds = null
        )
        val repositoryRecordList =
            repositoryDao.listByProject(
                dslContext = dslContext,
                projectIds = projectIds,
                repositoryType = repositoryType,
                repositoryIds = null,
                offset = offset,
                limit = limit
            )
        val repositoryList = repositoryRecordList.map {
            RepositoryInfo(
                repositoryId = it.repositoryId,
                repositoryHashId = HashUtil.encodeOtherLongId(it.repositoryId),
                aliasName = it.aliasName,
                url = it.url,
                type = ScmType.valueOf(it.type),
                updatedTime = it.updatedTime.timestamp()
            )
        }
        return SQLPage(count, repositoryList)
    }

    fun searchByAliasName(
        projectId: String,
        aliasName: String,
        offset: Int,
        limit: Int
    ): SQLPage<RepositoryInfo> {

        val count = repositoryDao.countByProject(
            dslContext = dslContext,
            projectIds = arrayListOf(projectId),
            repositoryTypes = null,
            aliasName = aliasName,
            repositoryIds = null
        )
        val repositoryRecordList =
            repositoryDao.listByProject(
                dslContext = dslContext,
                projectId = projectId,
                aliasName = aliasName,
                repositoryTypes = null,
                repositoryIds = null,
                offset = offset,
                limit = limit
            )
        val repositoryList = repositoryRecordList.map {
            RepositoryInfo(
                repositoryId = it.repositoryId,
                repositoryHashId = HashUtil.encodeOtherLongId(it.repositoryId),
                aliasName = it.aliasName,
                url = it.url,
                type = ScmType.valueOf(it.type),
                updatedTime = it.updatedTime.timestamp()
            )
        }
        return SQLPage(count, repositoryList)
    }

    fun userDelete(userId: String, projectId: String, repositoryHashId: String) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.DELETE,
            message = MessageCodeUtil.generateResponseDataObject<String>(
                RepositoryMessageCode.USER_DELETE_PEM_ERROR,
                arrayOf(userId, projectId, repositoryHashId)
            ).message!!
        )

        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }

        deleteResource(projectId, repositoryId)
        repositoryDao.delete(dslContext, repositoryId)
    }

    fun validatePermission(user: String, projectId: String, authPermission: AuthPermission, message: String) {
        if (!validatePermission(user, projectId, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    fun validatePermission(
        user: String,
        projectId: String,
        repositoryId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        repositoryPermissionService.validatePermission(
            userId = user,
            projectId = projectId,
            authPermission = authPermission,
            repositoryId = repositoryId,
            message = message
        )
    }

    fun userLock(userId: String, projectId: String, repositoryHashId: String) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageCodeUtil.generateResponseDataObject<String>(
                RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                arrayOf(userId, projectId, repositoryHashId)
            ).message!!
        )
        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }
        if (record.type != ScmType.CODE_SVN.name) {
            throw PermissionForbiddenException(
                MessageCodeUtil.generateResponseDataObject<String>(
                    RepositoryMessageCode.REPO_LOCK_UN_SUPPORT,
                    arrayOf(repositoryHashId)
                ).message!!
            )
        }

        scmService.lock(
            projectName = record.projectId,
            url = record.url,
            type = ScmType.CODE_SVN,
            region = CodeSvnRegion.getRegion(record.url),
            userName = record.userId
        )
    }

    fun userUnLock(userId: String, projectId: String, repositoryHashId: String) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageCodeUtil.generateResponseDataObject<String>(
                RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                arrayOf(userId, projectId, repositoryHashId)
            ).message!!
        )
        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }
        if (record.type != ScmType.CODE_SVN.name) {
            throw PermissionForbiddenException(
                MessageCodeUtil.generateResponseDataObject<String>(
                    RepositoryMessageCode.REPO_LOCK_UN_SUPPORT,
                    arrayOf(repositoryHashId)
                ).message!!
            )
        }
        scmService.unlock(
            projectName = record.projectId,
            url = record.url,
            type = ScmType.CODE_SVN,
            region = CodeSvnRegion.getRegion(record.url),
            userName = record.userId
        )
    }

    fun getInfoByHashIds(hashIds: List<String>): List<RepositoryInfo> {
        val repositoryIds = hashIds.map { HashUtil.decodeOtherIdToLong(it) }
        val repositoryInfos = repositoryDao.getRepoByIds(
            dslContext = dslContext,
            repositoryIds = repositoryIds,
            checkDelete = true
        )
        val result = mutableListOf<RepositoryInfo>()
        repositoryInfos?.map {
            result.add(
                RepositoryInfo(
                    repositoryId = it.repositoryId,
                    repositoryHashId = HashUtil.encodeOtherLongId(it.repositoryId),
                    aliasName = it.aliasName,
                    url = it.url,
                    type = ScmType.valueOf(it.type),
                    updatedTime = it.updatedTime.timestampmilli(),
                    createUser = it.userId
                )
            )
        }
        return result
    }

    fun getRepositoryByHashIds(hashIds: List<String>): List<Repository> {
        val repositoryIds = hashIds.map { HashUtil.decodeOtherIdToLong(it) }
        val repositoryInfos = repositoryDao.getRepoByIds(
            dslContext = dslContext,
            repositoryIds = repositoryIds,
            checkDelete = true
        )
        val result = mutableListOf<Repository>()
        repositoryInfos?.map {
            val repository = compose(it)
            result.add(repository)
        }
        return result
    }

    fun getInfoByIds(ids: List<Long>): List<RepositoryInfo> {
        val repositoryInfos = repositoryDao.getRepoByIds(
            dslContext = dslContext,
            repositoryIds = ids,
            checkDelete = true
        )
        val result = mutableListOf<RepositoryInfo>()
        repositoryInfos?.map {
            result.add(
                RepositoryInfo(
                    repositoryId = it.repositoryId,
                    repositoryHashId = HashUtil.encodeOtherLongId(it.repositoryId),
                    aliasName = it.aliasName,
                    url = it.url,
                    type = ScmType.valueOf(it.type),
                    updatedTime = it.updatedTime.timestampmilli(),
                    createUser = it.userId
                )
            )
        }
        return result
    }

    private fun validatePermission(user: String, projectId: String, authPermission: AuthPermission): Boolean {
        return repositoryPermissionService.hasPermission(
            userId = user,
            projectId = projectId,
            authPermission = authPermission
        )
    }

    private fun createResource(user: String, projectId: String, repositoryId: Long, repositoryName: String) {
        repositoryPermissionService.createResource(
            userId = user,
            projectId = projectId,
            repositoryId = repositoryId,
            repositoryName = repositoryName
        )
    }

    private fun editResource(projectId: String, repositoryId: Long, repositoryName: String) {
        repositoryPermissionService.editResource(
            projectId = projectId,
            repositoryId = repositoryId,
            repositoryName = repositoryName
        )
    }

    private fun deleteResource(projectId: String, repositoryId: Long) {
        repositoryPermissionService.deleteResource(projectId = projectId, repositoryId = repositoryId)
    }

    private fun checkRepositoryToken(projectId: String, repo: Repository): String {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val result = client.get(ServiceCredentialResource::class)
                .get(projectId, repo.credentialId, encoder.encodeToString(pair.publicKey))
        if (result.isNotOk() || result.data == null) {
            throw ErrorCodeException(errorCode = RepositoryMessageCode.GET_TICKET_FAIL)
        }
        val credential = result.data!!
        logger.info("Get the credential($credential)")
        val list = ArrayList<String>()

        list.add(decode(credential.v1, credential.publicKey, pair.privateKey))
        if (!credential.v2.isNullOrEmpty()) {
            list.add(decode(credential.v2!!, credential.publicKey, pair.privateKey))
            if (!credential.v3.isNullOrEmpty()) {
                list.add(decode(credential.v3!!, credential.publicKey, pair.privateKey))
                if (!credential.v4.isNullOrEmpty()) {
                    list.add(decode(credential.v4!!, credential.publicKey, pair.privateKey))
                }
            }
        }
        var token = list[0]
        val checkResult = when (repo) {
            is CodeSvnRepository -> {
                token = StringUtils.EMPTY
                val svnCredential = CredentialUtils.getCredential(repo, list, result.data!!.credentialType)
                scmService.checkPrivateKeyAndToken(
                    projectName = repo.projectName,
                    url = repo.getFormatURL(),
                    type = ScmType.CODE_SVN,
                    privateKey = svnCredential.privateKey,
                    passPhrase = svnCredential.passPhrase,
                    token = null,
                    region = repo.region,
                    userName = svnCredential.username
                )
            }
            is CodeGitRepository -> {
                when (repo.authType) {
                    RepoAuthType.SSH -> {
                        if (list.size < 2) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY)
                            )
                        }
                        val privateKey = list[1]
                        if (privateKey.isEmpty()) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY)
                            )
                        }
                        val passPhrase = if (list.size > 2) {
                            val p = list[2]
                            p.ifEmpty { null }
                        } else {
                            null
                        }
                        scmService.checkPrivateKeyAndToken(
                            projectName = repo.projectName,
                            url = repo.getFormatURL(),
                            type = ScmType.CODE_GIT,
                            privateKey = privateKey,
                            passPhrase = passPhrase,
                            token = token,
                            region = null,
                            userName = repo.userName
                        )
                    }
                    RepoAuthType.HTTP -> {
                        if (list.size < 2) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                            )
                        }
                        val username = list[1]
                        if (username.isEmpty()) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                            )
                        }
                        if (list.size < 3) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
                            )
                        }
                        val password = list[2]
                        if (password.isEmpty()) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
                            )
                        }
                        scmService.checkUsernameAndPassword(
                            projectName = repo.projectName,
                            url = repo.getFormatURL(),
                            type = ScmType.CODE_GIT,
                            username = username,
                            password = password,
                            token = token,
                            region = null,
                            repoUsername = repo.userName
                        )
                    }
                    else -> {
                        throw ErrorCodeException(
                            errorCode = RepositoryMessageCode.REPO_TYPE_NO_NEED_CERTIFICATION,
                            params = arrayOf(repo.authType!!.name)
                        )
                    }
                }
            }
            is CodeTGitRepository -> {
                when (repo.authType) {
                    RepoAuthType.SSH -> {
                        if (list.size < 2) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY)
                            )
                        }
                        val privateKey = list[1]
                        if (privateKey.isEmpty()) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY)
                            )
                        }
                        val passPhrase = if (list.size > 2) {
                            val p = list[2]
                            p.ifEmpty {
                                null
                            }
                        } else {
                            null
                        }
                        scmService.checkPrivateKeyAndToken(
                            projectName = repo.projectName,
                            url = repo.getFormatURL(),
                            type = ScmType.CODE_GIT,
                            privateKey = privateKey,
                            passPhrase = passPhrase,
                            token = token,
                            region = null,
                            userName = repo.userName
                        )
                    }
                    RepoAuthType.HTTP -> {
                        if (list.size < 2) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                            )
                        }
                        val username = list[1]
                        if (username.isEmpty()) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                            )
                        }
                        if (list.size < 3) {
                            logger.info("TGit check type is username+password,don't check, return")
                            return StringUtils.EMPTY
                        }
                        val password = list[2]
                        if (password.isEmpty()) {
                            throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY))
                        }
                        scmService.checkUsernameAndPassword(
                            projectName = repo.projectName,
                            url = repo.getFormatURL(),
                            type = ScmType.CODE_GIT,
                            username = username,
                            password = password,
                            token = token,
                            region = null,
                            repoUsername = repo.userName
                        )
                    }
                    RepoAuthType.HTTPS -> {
                        if (list.size < 2) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                            )
                        }
                        val username = list[1]
                        if (username.isEmpty()) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                            )
                        }
                        if (list.size < 3) {
                            logger.info("TGit check type is username+password,don't check, return")
                            return StringUtils.EMPTY
                        }
                        val password = list[2]
                        if (password.isEmpty()) {
                            throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY))
                        }
                        scmService.checkUsernameAndPassword(
                            projectName = repo.projectName,
                            url = repo.getFormatURL(),
                            type = ScmType.CODE_TGIT,
                            username = username,
                            password = password,
                            token = token,
                            region = null,
                            repoUsername = repo.userName
                        )
                    }
                    else -> {
                        throw ErrorCodeException(
                            errorCode = RepositoryMessageCode.REPO_TYPE_NO_NEED_CERTIFICATION,
                            params = arrayOf(repo.authType!!.name)
                        )
                    }
                }
            }
            is CodeGitlabRepository -> {
                when (repo.authType) {
                    RepoAuthType.SSH -> {
                        if (list.size < 2) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY)
                            )
                        }
                        val privateKey = list[1]
                        if (privateKey.isEmpty()) {
                            throw OperationException(
                                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY)
                            )
                        }
                        val passPhrase = if (list.size > 2) {
                            val p = list[2]
                            p.ifEmpty {
                                null
                            }
                        } else {
                            null
                        }
                        scmService.checkPrivateKeyAndToken(
                            projectName = repo.projectName,
                            url = repo.getFormatURL(),
                            type = ScmType.CODE_GITLAB,
                            privateKey = privateKey,
                            passPhrase = passPhrase,
                            token = token,
                            region = null,
                            userName = repo.userName
                        )
                    }
                    else -> {
                        scmService.checkPrivateKeyAndToken(
                            projectName = repo.projectName,
                            url = repo.getFormatURL(),
                            type = ScmType.CODE_GITLAB,
                            privateKey = null,
                            passPhrase = null,
                            token = list[0],
                            region = null,
                            userName = repo.userName
                        )
                    }
                }
            }
            is CodeP4Repository -> {
                token = StringUtils.EMPTY
                val username = list[0]
                if (username.isEmpty()) {
                    throw OperationException(
                        message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
                    )
                }
                if (list.size < 2) {
                    throw OperationException(
                        message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
                    )
                }
                val password = list[1]
                if (password.isEmpty()) {
                    throw OperationException(
                        message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
                    )
                }
                scmService.checkUsernameAndPassword(
                    projectName = repo.projectName,
                    url = repo.getFormatURL(),
                    type = ScmType.CODE_P4,
                    username = username,
                    password = password,
                    token = "",
                    region = null,
                    repoUsername = username
                )
            }
            else -> {
                throw IllegalArgumentException("Unknown repo($repo)")
            }
        }

        if (!checkResult.result) {
            logger.warn("Fail to check the repo token & private key because of ${checkResult.message}")
            throw OperationException(checkResult.message)
        }

        // 返回token信息
        return token
    }

    private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
    }

    private fun needToCheckToken(repository: Repository): Boolean {
        if (repository is GithubRepository) {
            return false
        }
        val isGitOauth = repository is CodeGitRepository && repository.authType == RepoAuthType.OAUTH
        if (isGitOauth) {
            return false
        }
        return true
    }

    fun getRepoRecentCommitInfo(
        userId: String,
        sha: String,
        repositoryConfig: RepositoryConfig,
        tokenType: TokenTypeEnum
    ): Result<GitCommit?> {
        val repo: CodeGitRepository = serviceGet("", repositoryConfig) as CodeGitRepository
        logger.info("the repo is:$repo")
        val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
        val getGitTokenResult = getGitToken(finalTokenType, userId)
        if (getGitTokenResult.isNotOk()) {
            return Result(status = getGitTokenResult.status, message = getGitTokenResult.message ?: "")
        }
        val token = getGitTokenResult.data!!
        return gitService.getRepoRecentCommitInfo(
            repoName = repo.projectName,
            sha = sha,
            token = token,
            tokenType = finalTokenType
        )
    }

    fun createGitTag(
        userId: String,
        tagName: String,
        ref: String,
        repositoryConfig: RepositoryConfig,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        val repo: CodeGitRepository = serviceGet("", repositoryConfig) as CodeGitRepository
        logger.info("the repo is:$repo")
        val finalTokenType = generateFinalTokenType(tokenType, repo.projectName)
        val getGitTokenResult = getGitToken(finalTokenType, userId)
        if (getGitTokenResult.isNotOk()) {
            return Result(status = getGitTokenResult.status, message = getGitTokenResult.message ?: "")
        }
        val token = getGitTokenResult.data!!
        return gitService.createGitTag(
            repoName = repo.projectName,
            tagName = tagName,
            ref = ref,
            token = token,
            tokenType = finalTokenType
        )
    }

    private fun getRepositoryType(repo: Repository): Pair<RepoAuthType?, ScmType>? {
        // 获取仓库对应的类型信息
        return when (repo) {
            is CodeGitRepository ->
                Pair(repo.authType, ScmType.CODE_GIT)
            is CodeTGitRepository ->
                Pair(repo.authType, ScmType.CODE_TGIT)
            is CodeGitlabRepository ->
                Pair(RepoAuthType.HTTP, ScmType.CODE_GITLAB)
            else ->
                return null
        }
    }

    fun getGitProjectId(repo: Repository, token: String): Int {
        logger.info("the repo is:$repo")
        val type = getRepositoryType(repo) ?: return -1
        // 根据仓库授权类型匹配Token类型
        val tokenType = if (type.first == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
        val gitProjectInfo = gitService.getGitProjectInfo(id = repo.projectName, token = token, tokenType = tokenType)
        logger.info("the gitProjectInfo is:$gitProjectInfo")
        return gitProjectInfo.data?.id ?: -1
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryService::class.java)
    }
}
