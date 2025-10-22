/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.repository.service.code

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.constant.RepositoryConstants
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.constant.RepositoryMessageCode.ERROR_AUTH_TYPE_ENABLED_PAC
import com.tencent.devops.repository.constant.RepositoryMessageCode.ERROR_DEFAULT_BRANCH_IS_EMPTY
import com.tencent.devops.repository.constant.RepositoryMessageCode.ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION
import com.tencent.devops.repository.constant.RepositoryMessageCode.GIT_INVALID
import com.tencent.devops.repository.constant.RepositoryMessageCode.NOT_AUTHORIZED_BY_OAUTH
import com.tencent.devops.repository.constant.RepositoryMessageCode.REPO_TYPE_NO_NEED_CERTIFICATION
import com.tencent.devops.repository.constant.RepositoryMessageCode.USER_SECRET_EMPTY
import com.tencent.devops.repository.dao.RepositoryCodeGitDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryDetailInfo
import com.tencent.devops.repository.pojo.credential.RepoCredentialInfo
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.RepoCredentialType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.service.RepoCredentialService
import com.tencent.devops.repository.service.permission.RepositoryAuthorizationService
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.scm.IGitService
import com.tencent.devops.repository.service.scm.IScmOauthService
import com.tencent.devops.repository.service.scm.IScmService
import com.tencent.devops.scm.code.git.CodeGitWebhookEvent
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CodeGitRepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryCodeGitDao: RepositoryCodeGitDao,
    private val dslContext: DSLContext,
    private val credentialService: RepoCredentialService,
    private val scmService: IScmService,
    private val gitOauthService: IGitOauthService,
    private val scmOauthService: IScmOauthService,
    private val gitService: IGitService,
    private val repositoryAuthorizationService: RepositoryAuthorizationService
) : CodeRepositoryService<CodeGitRepository> {
    override fun repositoryType(): String {
        return CodeGitRepository::class.java.name
    }

    override fun create(projectId: String, userId: String, repository: CodeGitRepository): Long {
        val credentialInfo = checkCredentialInfo(projectId = projectId, repository = repository)
        var repositoryId = 0L
        // Git项目ID
        val gitProjectId = getGitProjectInfo(
            repo = repository,
            token = credentialInfo.token
        )?.id ?: throw ErrorCodeException(
            errorCode = ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
            params = arrayOf(repository.url, userId)
        )
        if (repository.enablePac == true) {
            pacCheckEnabled(projectId = projectId, userId = userId, repository = repository, retry = false)
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            repositoryId = repositoryDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                userId = userId,
                aliasName = repository.aliasName,
                url = repository.getFormatURL(),
                type = ScmType.CODE_GIT,
                atom = repository.atom,
                enablePac = repository.enablePac,
                scmCode = ScmType.CODE_GIT.name
            )
            repositoryCodeGitDao.create(
                dslContext = transactionContext,
                repositoryId = repositoryId,
                projectName = GitUtils.getProjectName(repository.url),
                userName = repository.userName,
                credentialId = repository.credentialId,
                authType = repository.authType,
                gitProjectId = gitProjectId,
                credentialType = credentialInfo.credentialType
            )
        }
        return repositoryId
    }

    override fun edit(
        userId: String,
        projectId: String,
        repositoryHashId: String,
        repository: CodeGitRepository,
        record: TRepositoryRecord
    ) {
        // 插件库仅允许修改OAUTH用户，不得修改其他内容
        if (record.atom == true && repository.authType != RepoAuthType.OAUTH) {
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    RepositoryMessageCode.ATOM_REPO_CAN_NOT_EDIT,
                    I18nUtil.getLanguage(userId)
                )
            )
        }
        // 提交的参数与数据库中类型不匹配
        if (record.type != ScmType.CODE_GIT.name) {
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = GIT_INVALID,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            )
        }
        // 不得切换代码库
        if (GitUtils.diffRepoUrl(record.url, repository.url)) {
            logger.warn("can not switch repo url|sourceUrl[${record.url}]|targetUrl[${repository.url}]")
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    RepositoryMessageCode.CAN_NOT_SWITCH_REPO_URL,
                    I18nUtil.getLanguage(userId)
                )
            )
        }
        // 凭证信息
        val credentialInfo = checkCredentialInfo(projectId = projectId, repository = repository)
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        // 原始代码库URL
        val sourceUrl = repositoryDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repositoryId = repositoryId
        ).url
        var gitProjectId: Long? = null
        // 需要更新gitProjectId
        if (sourceUrl != repository.url || repository.gitProjectId == null || repository.gitProjectId == 0L) {
            logger.info(
                "repository url unMatch,need change gitProjectId,sourceUrl=[$sourceUrl] targetUrl=[${repository.url}]"
            )
            // Git项目ID
            gitProjectId = getGitProjectInfo(
                repo = repository,
                token = credentialInfo.token
            )?.id ?: throw ErrorCodeException(
                errorCode = ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
                params = arrayOf(repository.url, repository.userName)
            )
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            repositoryDao.edit(
                dslContext = transactionContext,
                repositoryId = repositoryId,
                aliasName = repository.aliasName,
                url = repository.getFormatURL(),
                updateUser = userId
            )
            repositoryCodeGitDao.edit(
                dslContext = transactionContext,
                repositoryId = repositoryId,
                projectName = GitUtils.getProjectName(repository.url),
                userName = repository.userName,
                credentialId = repository.credentialId,
                authType = repository.authType,
                gitProjectId = gitProjectId,
                credentialType = credentialInfo.credentialType
            )
            // 重置授权管理
            repositoryAuthorizationService.batchModifyHandoverFrom(
                projectId = projectId,
                resourceAuthorizationHandoverList = listOf(
                    ResourceAuthorizationHandoverDTO(
                        projectCode = projectId,
                        resourceType = AuthResourceType.CODE_REPERTORY.value,
                        resourceName = record.aliasName,
                        resourceCode = repositoryHashId,
                        handoverTo = repository.userName
                    )
                )
            )
        }
    }

    override fun compose(repository: TRepositoryRecord): CodeGitRepository {
        val record = repositoryCodeGitDao.get(dslContext, repository.repositoryId)
        return CodeGitRepository(
            aliasName = repository.aliasName,
            url = repository.url,
            credentialId = record.credentialId,
            projectName = record.projectName,
            userName = record.userName,
            authType = RepoAuthType.parse(record.authType),
            projectId = repository.projectId,
            repoHashId = HashUtil.encodeOtherLongId(repository.repositoryId),
            gitProjectId = record.gitProjectId,
            atom = repository.atom,
            enablePac = repository.enablePac,
            yamlSyncStatus = repository.yamlSyncStatus,
            scmCode = repository.scmCode ?: ScmType.CODE_GIT.name,
            credentialType = record.credentialType ?: RepoCredentialType.OAUTH.name
        )
    }

    fun checkToken(
        repoCredentialInfo: RepoCredentialInfo,
        repository: CodeGitRepository
    ): TokenCheckResult {
        val checkResult = when (repository.authType) {
            RepoAuthType.SSH -> {
                if (repoCredentialInfo.token.isEmpty()) {
                    throw OperationException(
                        message = I18nUtil.getCodeLanMessage(CommonMessageCode.GIT_TOKEN_EMPTY)
                    )
                }
                if (repoCredentialInfo.privateKey.isEmpty()) {
                    throw OperationException(
                        message = I18nUtil.getCodeLanMessage(USER_SECRET_EMPTY)
                    )
                }
                scmService.checkPrivateKeyAndToken(
                    projectName = repository.projectName,
                    url = repository.getFormatURL(),
                    type = ScmType.CODE_GIT,
                    privateKey = repoCredentialInfo.privateKey,
                    passPhrase = repoCredentialInfo.passPhrase,
                    token = repoCredentialInfo.token,
                    region = null,
                    userName = repository.userName
                )
            }

            RepoAuthType.HTTP -> {
                if (repoCredentialInfo.username.isEmpty()) {
                    throw OperationException(
                        message = I18nUtil.getCodeLanMessage(CommonMessageCode.USER_NAME_EMPTY)
                    )
                }
                if (repoCredentialInfo.password.isEmpty()) {
                    throw OperationException(
                        message = I18nUtil.getCodeLanMessage(CommonMessageCode.PWD_EMPTY)
                    )
                }
                scmService.checkUsernameAndPassword(
                    projectName = repository.projectName,
                    url = repository.getFormatURL(),
                    type = ScmType.CODE_GIT,
                    username = repoCredentialInfo.username,
                    password = repoCredentialInfo.password,
                    token = repoCredentialInfo.token,
                    region = null,
                    repoUsername = repository.userName
                )
            }

            else -> {
                throw ErrorCodeException(
                    errorCode = REPO_TYPE_NO_NEED_CERTIFICATION,
                    params = arrayOf(repository.authType!!.name)
                )
            }
        }
        return checkResult
    }

    override fun getRepoDetailMap(repositoryIds: List<Long>): Map<Long, RepositoryDetailInfo> {
        return repositoryCodeGitDao.list(
            dslContext = dslContext,
            repositoryIds = repositoryIds.toSet()
        )?.associateBy({ it.repositoryId }, {
            val gitAuthType = it.authType
                ?: RepoAuthType.SSH.name
            val gitAuthIdentity = if (gitAuthType == RepoAuthType.OAUTH.name) {
                it.userName
            } else {
                it.credentialId
            }
            RepositoryDetailInfo(
                authType = gitAuthType,
                credentialId = gitAuthIdentity
            )
        }) ?: mapOf()
    }

    override fun getPacProjectId(userId: String, repoUrl: String): String? {
        val token = gitOauthService.getAccessToken(userId = userId)?.accessToken ?: throw ErrorCodeException(
            errorCode = NOT_AUTHORIZED_BY_OAUTH,
            params = arrayOf(userId)
        )
        val gitProjectId = getGitProjectInfo(repoUrl = repoUrl, userId = userId, token = token).id
        return getPacRepository(externalId = gitProjectId.toString())?.projectId
    }

    override fun pacCheckEnabled(projectId: String, userId: String, record: TRepositoryRecord, retry: Boolean) {
        val repository = compose(record)
        if (repository.authType != RepoAuthType.OAUTH) {
            throw ErrorCodeException(errorCode = ERROR_AUTH_TYPE_ENABLED_PAC)
        }
        val gitProjectId =
            pacCheckEnabled(projectId = projectId, userId = userId, repository = repository, retry = retry)
        // 修复历史数据
        if (repository.gitProjectId == null || repository.gitProjectId == 0L) {
            val repositoryId = HashUtil.decodeOtherIdToLong(repository.repoHashId!!)
            repositoryCodeGitDao.updateGitProjectId(
                dslContext = dslContext,
                id = repositoryId,
                gitProjectId = gitProjectId
            )
        }
    }

    private fun pacCheckEnabled(
        projectId: String,
        userId: String,
        repository: CodeGitRepository,
        retry: Boolean
    ): Long {
        if (repository.authType != RepoAuthType.OAUTH) {
            throw ErrorCodeException(errorCode = ERROR_AUTH_TYPE_ENABLED_PAC)
        }
        val credentialInfo = getCredentialInfo(projectId = projectId, repository = repository)
        // 获取工蜂ID
        val gitProjectInfo = try {
            getGitProjectInfo(
                repo = repository, token = credentialInfo.token
            )
        } catch (ignore: Exception) {
            null
        } ?: throw ErrorCodeException(
            errorCode = ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
            params = arrayOf(repository.url, repository.userName)
        )
        if (gitProjectInfo.defaultBranch == null) {
            throw ErrorCodeException(
                errorCode = ERROR_DEFAULT_BRANCH_IS_EMPTY
            )
        }
        val gitProjectId = gitProjectInfo.id.toString()
        // 重试不需要校验开启的pac仓库
        if (!retry) {
            getPacRepository(externalId = gitProjectId)?.let {
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_REPO_URL_HAS_ENABLED_PAC,
                    params = arrayOf(it.projectId, it.aliasName)
                )
            }
        }
        val member = gitService.getProjectMembersAll(
            gitProjectId = gitProjectId,
            page = 1,
            pageSize = 1,
            search = repository.userName,
            tokenType = TokenTypeEnum.OAUTH,
            token = credentialInfo.token
        ).data?.firstOrNull() ?: throw ErrorCodeException(
            errorCode = RepositoryMessageCode.ERROR_MEMBER_NOT_FOUND,
            params = arrayOf(repository.userName)
        )
        if (member.accessLevel < GitAccessLevelEnum.MASTER.level) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_MEMBER_LEVEL_LOWER_MASTER,
                params = arrayOf(repository.userName)
            )
        }
        // 初始化应该新增push和mr事件
        scmOauthService.addWebHook(
            projectName = gitProjectInfo.id.toString(),
            url = repository.url,
            type = ScmType.CODE_GIT,
            privateKey = null,
            passPhrase = null,
            token = credentialInfo.token,
            region = null,
            userName = userId,
            event = CodeGitWebhookEvent.PUSH_EVENTS.value
        )
        scmOauthService.addWebHook(
            projectName = gitProjectInfo.id.toString(),
            url = repository.url,
            type = ScmType.CODE_GIT,
            privateKey = null,
            passPhrase = null,
            token = credentialInfo.token,
            region = null,
            userName = userId,
            event = CodeGitWebhookEvent.MERGE_REQUESTS_EVENTS.value
        )
        return gitProjectInfo.id
    }

    override fun getGitFileTree(projectId: String, userId: String, record: TRepositoryRecord): List<GitFileInfo> {
        val codeGitRepository = compose(record)
        val credentialInfo = getCredentialInfo(projectId = projectId, repository = codeGitRepository)
        val gitProjectInfo = getGitProjectInfo(
            repoUrl = record.url,
            userId = codeGitRepository.userName,
            token = credentialInfo.token
        )
        return gitService.getGitFileTree(
            gitProjectId = gitProjectInfo.id.toString(),
            ref = gitProjectInfo.defaultBranch,
            path = RepositoryConstants.CI_DIR_PATH,
            token = credentialInfo.token,
            recursive = false,
            tokenType = TokenTypeEnum.OAUTH
        ).data ?: emptyList()
    }

    /**
     * 检查凭证信息
     */
    private fun checkCredentialInfo(projectId: String, repository: CodeGitRepository): RepoCredentialInfo {
        // 凭证信息
        val repoCredentialInfo = getCredentialInfo(projectId = projectId, repository = repository)
        // 若授权类型不为OAUTH则需要检查Token
        if (repository.authType != RepoAuthType.OAUTH) {
            val checkResult = checkToken(
                repoCredentialInfo = repoCredentialInfo,
                repository = repository
            )
            if (!checkResult.result) {
                logger.warn("Fail to check the repo token & private key because of ${checkResult.message}")
                throw OperationException(checkResult.message)
            }
        }
        return repoCredentialInfo
    }

    /**
     * 获取凭证信息
     */
    fun getCredentialInfo(projectId: String, repository: CodeGitRepository): RepoCredentialInfo {
        // 凭证信息
        return if (repository.authType == RepoAuthType.OAUTH) {
            RepoCredentialInfo(
                token = gitOauthService.getAccessToken(repository.userName)?.accessToken ?: "",
                credentialType = RepoCredentialType.OAUTH.name
            )
        } else {
            credentialService.getCredentialInfo(
                projectId = projectId,
                repository = repository,
                tryGetSession = true
            )
        }
    }

    override fun getPacRepository(externalId: String): TRepositoryRecord? {
        // 判断是否已有仓库开启pac
        val repositoryIds = repositoryCodeGitDao.listByGitProjectId(
            dslContext = dslContext,
            gitProjectId = externalId.toLong()
        ).map { it.repositoryId }
        return repositoryDao.getPacRepositoryByIds(dslContext = dslContext, repositoryIds = repositoryIds)
    }

    /**
     * 获取Git项目ID
     */
    fun getGitProjectInfo(repo: CodeGitRepository, token: String): GitProjectInfo? {
        val isOauth = repo.authType == RepoAuthType.OAUTH
        logger.info("the repo is:$repo,token length:${StringUtils.length(token)},isOauth:$isOauth")
        val repositoryProjectInfo = if (isOauth) {
            scmOauthService.getProjectInfo(
                projectName = repo.projectName,
                url = repo.getFormatURL(),
                type = ScmType.CODE_GIT,
                token = token
            )
        } else {
            scmService.getProjectInfo(
                projectName = repo.projectName,
                url = repo.getFormatURL(),
                type = ScmType.CODE_GIT,
                token = token
            )
        }
        logger.info("the gitProjectInfo is:$repositoryProjectInfo")
        return repositoryProjectInfo
    }

    private fun getGitProjectInfo(repoUrl: String, userId: String, token: String): GitProjectInfo {
        val gitProjectName = GitUtils.getProjectName(repoUrl)
        return scmOauthService.getProjectInfo(
            projectName = gitProjectName,
            url = repoUrl,
            type = ScmType.CODE_GIT,
            token = token
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
            params = arrayOf(repoUrl, userId)
        )
    }

    override fun addResourceAuthorization(
        projectId: String,
        userId: String,
        repositoryId: Long,
        repository: CodeGitRepository
    ) {
        with(repository) {
            if (authType == RepoAuthType.OAUTH) {
                repositoryAuthorizationService.addResourceAuthorization(
                    projectId = projectId,
                    listOf(
                        ResourceAuthorizationDTO(
                            projectCode = projectId,
                            resourceType = AuthResourceType.CODE_REPERTORY.value,
                            resourceName = repository.aliasName,
                            resourceCode = HashUtil.encodeOtherLongId(repositoryId),
                            handoverFrom = userId,
                            handoverTime = LocalDateTime.now().timestampmilli()
                        )
                    )
                )
            }
        }
    }

    override fun listByCondition(
        repoCondition: RepoCondition,
        limit: Int,
        offset: Int
    ): List<Repository>? {
        return repositoryCodeGitDao.listByCondition(
            dslContext = dslContext,
            repoCondition = repoCondition,
            limit = limit,
            offset = offset
        )
    }

    override fun countByCondition(repoCondition: RepoCondition): Long {
        return repositoryCodeGitDao.countByCondition(
            dslContext = dslContext,
            repoCondition = repoCondition
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitRepositoryService::class.java)
    }
}
