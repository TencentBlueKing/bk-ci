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

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.coerceAtMaxLength
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
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.process.api.service.ServicePipelineYamlResource
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.constant.RepositoryMessageCode.PAC_REPO_CAN_NOT_DELETE
import com.tencent.devops.repository.constant.RepositoryMessageCode.PAC_REPO_CAN_NOT_RENAME
import com.tencent.devops.repository.constant.RepositoryMessageCode.USER_CREATE_PEM_ERROR
import com.tencent.devops.repository.dao.RepositoryCodeGitDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.AtomRefRepositoryInfo
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.RepoRename
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryDetailInfo
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.service.loader.CodeRepositoryServiceRegistrar
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.scm.IGitService
import com.tencent.devops.repository.service.scm.IScmService
import com.tencent.devops.repository.service.tgit.TGitOAuthService
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import com.tencent.devops.scm.pojo.GitRepositoryResp
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
    private val repositoryCodeGitDao: RepositoryCodeGitDao,
    private val gitOauthService: IGitOauthService,
    private val gitService: IGitService,
    private val scmService: IScmService,
    private val tGitOAuthService: TGitOAuthService,
    private val dslContext: DSLContext,
    private val repositoryPermissionService: RepositoryPermissionService,
    private val client: Client
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
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
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
                repoHashId = null,
                gitProjectId = 0L,
                atom = true
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
            I18nUtil.generateResponseDataObject(
                CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
        }
    }

    private fun getGitToken(tokenType: TokenTypeEnum, userId: String): Result<String?> {
        val token = if (TokenTypeEnum.OAUTH == tokenType) {
            val gitToken = gitOauthService.getAccessToken(userId)
            logger.info("gitToken>> $gitToken")
            if (null == gitToken) {
                // 抛出无效的token提示
                return I18nUtil.generateResponseDataObject(
                    CommonMessageCode.OAUTH_TOKEN_IS_INVALID,
                    language = I18nUtil.getLanguage(userId)
                )
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
            I18nUtil.generateResponseDataObject(
                CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
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
                refName = refName,
                path = path,
                token = token,
                tokenType = tokenType
            )
            getGitRepositoryTreeInfoResult
        } catch (e: Exception) {
            logger.error("getGitRepositoryTreeInfo error is :$e", e)
            I18nUtil.generateResponseDataObject(
                CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
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
                        url = gitProjectInfo.repositoryUrl,
                        updateUser = userId
                    )
                    repositoryCodeGitDao.edit(
                        dslContext = context,
                        repositoryId = repositoryId,
                        projectName = gitProjectInfo.namespaceName,
                        userName = repo.userName,
                        credentialId = repo.credentialId,
                        authType = repo.authType,
                        gitProjectId = -1L
                    )
                }
                Result(gitProjectInfo)
            } else {
                Result(moveProjectToGroupResult.status, moveProjectToGroupResult.message ?: "")
            }
        } catch (e: Exception) {
            logger.error("moveProjectToGroupResult error is :$e", e)
            I18nUtil.generateResponseDataObject(
                CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
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

    @ActionAuditRecord(
        actionId = ActionId.REPERTORY_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.REPERTORY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.REPERTORY_CREATE_CONTENT
    )
    fun userCreate(userId: String, projectId: String, repository: Repository): String {
        // 指定oauth的用户名字只能是登录用户。
        repository.userName = userId
        validatePermission(
            userId,
            projectId,
            AuthPermission.CREATE,
            MessageUtil.getMessageByLocale(
                USER_CREATE_PEM_ERROR,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, projectId)
            )
        )
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
        val repositoryService = CodeRepositoryServiceRegistrar.getService(repository = repository)
        val repositoryId =
            repositoryService.create(projectId = projectId, userId = userId, repository = repository)
        ActionAuditContext.current()
            .setInstanceId(repositoryId.toString())
            .setInstanceName(repository.aliasName)
            .setInstance(repository)
        createResource(userId, projectId, repositoryId, repository.aliasName)
        try {
            if (repository.enablePac == true) {
                client.get(ServicePipelineYamlResource::class).enable(
                    userId = userId,
                    projectId = projectId,
                    repoHashId = HashUtil.encodeOtherLongId(repositoryId),
                    scmType = repository.getScmType()
                )
            }
        } catch (exception: Exception) {
            logger.error("failed to enable pac when create repository,rollback|$projectId|$repositoryId")
            userDelete(
                userId = userId,
                projectId = projectId,
                repositoryHashId = HashUtil.encodeOtherLongId(repositoryId),
                checkPac = false
            )
            throw exception
        }
        return repositoryId
    }

    @ActionAuditRecord(
        actionId = ActionId.REPERTORY_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.REPERTORY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.REPERTORY_VIEW_CONTENT
    )
    fun userGet(userId: String, projectId: String, repositoryConfig: RepositoryConfig): Repository {
        val repository = getRepository(projectId, repositoryConfig)
        val repositoryId = repository.repositoryId
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.VIEW,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_VIEW_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryConfig.getRepositoryId()),
                language = I18nUtil.getLanguage(userId)
            )
        )
        ActionAuditContext.current()
            .setInstanceId(repository.repositoryId.toString())
            .setInstanceName(repository.aliasName)
        return compose(repository)
    }

    @ActionAuditRecord(
        actionId = ActionId.REPERTORY_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.REPERTORY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.REPERTORY_VIEW_CONTENT
    )
    fun serviceGet(projectId: String, repositoryConfig: RepositoryConfig): Repository {
        val repository = getRepository(projectId, repositoryConfig)
        ActionAuditContext.current()
            .setInstanceId(repository.repositoryId.toString())
            .setInstanceName(repository.aliasName)
        return compose(repository)
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
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getServiceByScmType(repository.type)
        return codeRepositoryService.compose(repository = repository)
    }

    @ActionAuditRecord(
        actionId = ActionId.REPERTORY_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.REPERTORY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.REPERTORY_EDIT_CONTENT
    )
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
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryHashId),
                language = I18nUtil.getLanguage(userId)
            )
        )
        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }

        if (!repository.isLegal()) {
            logger.warn("The repository($repository) is illegal")
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    messageCode = RepositoryMessageCode.REPO_PATH_WRONG_PARM,
                    params = arrayOf(repository.getStartPrefix()),
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        if (hasAliasName(projectId, repositoryHashId, repository.aliasName)) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    RepositoryMessageCode.REPO_NAME_EXIST,
                    I18nUtil.getLanguage(userId),
                    arrayOf(repository.aliasName)
                )
            )
        }
        val repositoryInfo = serviceGet(
            projectId = projectId,
            RepositoryConfig(
                repositoryHashId = repositoryHashId,
                repositoryName = null,
                repositoryType = RepositoryType.ID
            )
        )
        ActionAuditContext.current()
            .setInstanceId(repositoryId.toString())
            .setInstanceName(repository.aliasName)
            .setOriginInstance(repositoryInfo)
            .setInstance(repository)
        val codeRepositoryService = CodeRepositoryServiceRegistrar.getService(repository)
        codeRepositoryService.edit(
            userId = userId,
            projectId = projectId,
            repositoryHashId = repositoryHashId,
            repository = repository,
            record = record
        )
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
                authType = authType,
                createUser = repository.userId,
                createTime = repository.createdTime.timestamp(),
                updatedUser = repository.updatedUser
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
            authPermissions = setOf(
                AuthPermission.LIST,
                AuthPermission.EDIT,
                AuthPermission.DELETE,
                AuthPermission.USE,
                AuthPermission.VIEW
            )
        )
        val hasListPermissionRepoList = permissionToListMap[AuthPermission.LIST]!!
        val hasEditPermissionRepoList = permissionToListMap[AuthPermission.EDIT]!!
        val hasDeletePermissionRepoList = permissionToListMap[AuthPermission.DELETE]!!
        val hasUsePermissionRepoList = permissionToListMap[AuthPermission.USE]!!
        val hasViewPermissionRepoList = permissionToListMap[AuthPermission.VIEW]!!
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
        val repoGroup = repositoryRecordList.groupBy { it.type }.mapValues { it.value.map { a -> a.repositoryId } }
        val repoDetailInfoMap = mutableMapOf<Long, RepositoryDetailInfo>()
        repoGroup.forEach { (type, repositoryIds) ->
            run {
                // 1. 获取处理类
                val codeGitRepositoryService = CodeRepositoryServiceRegistrar.getServiceByScmType(scmType = type)
                // 2. 得到授权身份<repoId, authInfo>
                repoDetailInfoMap.putAll(codeGitRepositoryService.getRepoDetailMap(repositoryIds))
            }
        }
        val repositoryList = repositoryRecordList.map {
            val hasEditPermission = hasEditPermissionRepoList.contains(it.repositoryId)
            val hasDeletePermission = hasDeletePermissionRepoList.contains(it.repositoryId)
            val hasUsePermission = hasUsePermissionRepoList.contains(it.repositoryId)
            val hasViewPermission = hasViewPermissionRepoList.contains(it.repositoryId)
            val repoDetailInfo = repoDetailInfoMap[it.repositoryId]
            RepositoryInfoWithPermission(
                repositoryHashId = HashUtil.encodeOtherLongId(it.repositoryId),
                aliasName = it.aliasName,
                url = it.url,
                type = ScmType.valueOf(it.type),
                updatedTime = it.updatedTime.timestamp(),
                canEdit = hasEditPermission,
                canDelete = hasDeletePermission,
                canUse = hasUsePermission,
                canView = hasViewPermission,
                authType = repoDetailInfo?.authType ?: RepoAuthType.HTTP.name,
                svnType = repoDetailInfo?.svnType,
                authIdentity = repoDetailInfo?.credentialId?.ifBlank { it.userId },
                createTime = it.createdTime.timestamp(),
                createUser = it.userId,
                updatedUser = it.updatedUser ?: it.userId,
                atom = it.atom ?: false,
                enablePac = it.enablePac
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
        aliasName: String? = null,
        enablePac: Boolean? = null
    ): SQLPage<RepositoryInfo> {
        val hasPermissionList = repositoryPermissionService.filterRepository(userId, projectId, authPermission)
        val repositoryTypes = repositoryType?.split(",")?.map { ScmType.valueOf(it) }

        val count = repositoryDao.countByProject(
            dslContext = dslContext,
            projectIds = setOf(projectId),
            repositoryTypes = repositoryTypes,
            aliasName = aliasName,
            repositoryIds = hasPermissionList.toSet(),
            enablePac = enablePac
        )
        val repositoryRecordList =
            repositoryDao.listByProject(
                dslContext = dslContext,
                projectId = projectId,
                repositoryTypes = repositoryTypes,
                aliasName = aliasName,
                repositoryIds = hasPermissionList.toSet(),
                enablePac = enablePac,
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

    @ActionAuditRecord(
        actionId = ActionId.REPERTORY_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.REPERTORY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.REPERTORY_DELETE_CONTENT
    )
    fun userDelete(
        userId: String,
        projectId: String,
        repositoryHashId: String,
        checkAtom: Boolean = true,
        checkPac: Boolean = true
    ) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.DELETE,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_DELETE_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryHashId),
                language = I18nUtil.getLanguage(userId)
            )
        )

        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }
        if (checkAtom && record.atom == true) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    RepositoryMessageCode.ATOM_REPO_CAN_NOT_DELETE,
                    I18nUtil.getLanguage(userId)
                )
            )
        }
        if (checkPac && record.enablePac == true) {
            throw ErrorCodeException(errorCode = PAC_REPO_CAN_NOT_DELETE)
        }
        ActionAuditContext.current()
            .setInstanceId(repositoryId.toString())
            .setInstanceName(record.aliasName)
        deleteResource(projectId, repositoryId)
        val deleteTime = DateTimeUtil.toDateTime(LocalDateTime.now(), "yyMMddHHmmSS")
        val deleteAliasName = "${record.aliasName}[$deleteTime]"
        repositoryDao.delete(
            dslContext = dslContext,
            repositoryId = repositoryId,
            deleteAliasName = deleteAliasName.coerceAtMaxLength(MAX_ALIAS_LENGTH),
            updateUser = userId
        )
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

    @ActionAuditRecord(
        actionId = ActionId.REPERTORY_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.REPERTORY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.REPERTORY_EDIT_LOCK_CONTENT
    )
    fun userLock(userId: String, projectId: String, repositoryHashId: String) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryHashId),
                language = I18nUtil.getLanguage(userId)
            )
        )
        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }
        if (record.type != ScmType.CODE_SVN.name) {
            throw PermissionForbiddenException(
                MessageUtil.getMessageByLocale(
                    messageCode = RepositoryMessageCode.REPO_LOCK_UN_SUPPORT,
                    params = arrayOf(repositoryHashId),
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        ActionAuditContext.current()
            .setInstanceId(record.repositoryId.toString())
            .setInstanceName(record.aliasName)
        scmService.lock(
            projectName = record.projectId,
            url = record.url,
            type = ScmType.CODE_SVN,
            region = CodeSvnRegion.getRegion(record.url),
            userName = record.userId
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.REPERTORY_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.REPERTORY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.REPERTORY_EDIT_LOCK_CONTENT
    )
    fun userUnLock(userId: String, projectId: String, repositoryHashId: String) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryHashId),
                language = I18nUtil.getLanguage(userId)
            )
        )
        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }
        if (record.type != ScmType.CODE_SVN.name) {
            throw PermissionForbiddenException(
                MessageUtil.getMessageByLocale(
                    messageCode = RepositoryMessageCode.REPO_LOCK_UN_SUPPORT,
                    params = arrayOf(repositoryHashId),
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        ActionAuditContext.current()
            .setInstanceId(record.repositoryId.toString())
            .setInstanceName(record.aliasName)
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

    private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
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

    @ActionAuditRecord(
        actionId = ActionId.REPERTORY_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.REPERTORY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.REPERTORY_EDIT_RENAME_CONTENT
    )
    fun rename(userId: String, projectId: String, repositoryHashId: String, repoRename: RepoRename) {
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        // 权限校验
        validatePermission(
            user = userId,
            projectId = projectId,
            repositoryId = repositoryId,
            authPermission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                messageCode = RepositoryMessageCode.USER_EDIT_PEM_ERROR,
                params = arrayOf(userId, projectId, repositoryHashId),
                language = I18nUtil.getLanguage(userId)
            )
        )
        if (hasAliasName(projectId, repositoryHashId, repoRename.name)) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.REPO_NAME_EXIST,
                params = arrayOf(repoRename.name)
            )
        }
        val record = repositoryDao.get(dslContext, repositoryId, projectId)
        if (record.projectId != projectId) {
            throw NotFoundException("Repository is not part of the project")
        }
        if (record.enablePac == true) {
            throw ErrorCodeException(errorCode = PAC_REPO_CAN_NOT_RENAME)
        }
        ActionAuditContext.current()
            .setInstanceId(repositoryId.toString())
            .setInstanceName(repoRename.name)
            .setOriginInstance(record.aliasName)
            .setInstance(repoRename.name)
        repositoryDao.rename(
            dslContext = dslContext,
            projectId = projectId,
            updateUser = userId,
            hashId = repositoryHashId,
            newName = repoRename.name
        )
        // 同步权限中心
        editResource(projectId, repositoryId, repoRename.name)
    }

    fun updateAtomRepoFlag(
        userId: String,
        atomRefRepositoryInfo: List<AtomRefRepositoryInfo>
    ) {
        logger.info("start update atom repo flag, userId: $userId, atomRefRepositoryInfo: $atomRefRepositoryInfo")
        if (atomRefRepositoryInfo.isEmpty()) {
            return
        }
        val repoInfos = mutableListOf <TRepositoryRecord>()
        // 过滤无效数据
        atomRefRepositoryInfo.forEach {
            val repositoryRecord = repositoryDao.getById(
                dslContext = dslContext,
                repositoryId = HashUtil.decodeOtherIdToLong(it.repositoryHashId)
            ) ?: return@forEach
            repoInfos.add(repositoryRecord)
        }
        repoInfos.forEach {
            logger.info("update atom repo flag|${it.projectId}|${it.repositoryHashId}")
            repositoryDao.updateAtomRepoFlag(
                dslContext = dslContext,
                projectId = it.projectId,
                repositoryId = it.repositoryId,
                atom = true
            )
        }
    }

    fun getGitProjectIdByRepositoryHashId(userId: String, repositoryHashIdList: List<String>): List<String> {
        return repositoryDao.getGitProjectIdByRepositoryHashId(dslContext, repositoryHashIdList)
    }

    fun isOAuth(
        userId: String,
        projectId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        repositoryType: ScmType?
    ): AuthorizeResult {
        return when (repositoryType) {
            ScmType.CODE_GIT -> gitOauthService.isOAuth(
                userId = userId,
                redirectUrlType = redirectUrlType,
                redirectUrl = redirectUrl
            )

            ScmType.CODE_TGIT -> tGitOAuthService.isOAuth(
                userId = userId,
                redirectUrlType = redirectUrlType,
                redirectUrl = redirectUrl
            )

            else ->
                AuthorizeResult(200, "")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryService::class.java)
        const val MAX_ALIAS_LENGTH = 255
    }
}
