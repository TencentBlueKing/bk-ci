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
package com.tencent.devops.repository.service.code

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.constant.RepositoryMessageCode.GITHUB_INVALID
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.dao.RepositoryGithubDao
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryDetailInfo
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.repository.sdk.github.service.GithubRepositoryService
import com.tencent.devops.repository.service.github.GithubTokenService
import com.tencent.devops.repository.service.permission.RepositoryAuthorizationService
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CodeGithubRepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryGithubDao: RepositoryGithubDao,
    private val dslContext: DSLContext,
    private val githubRepositoryService: GithubRepositoryService,
    private val githubTokenService: GithubTokenService,
    private val repositoryAuthorizationService: RepositoryAuthorizationService
) : CodeRepositoryService<GithubRepository> {
    override fun repositoryType(): String {
        return GithubRepository::class.java.name
    }

    override fun create(projectId: String, userId: String, repository: GithubRepository): Long {
        // Github无需检查凭证信息
        var repositoryId: Long = 0L
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            repositoryId = repositoryDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                userId = userId,
                aliasName = repository.aliasName,
                url = repository.getFormatURL(),
                type = ScmType.GITHUB,
                enablePac = repository.enablePac,
                scmCode = ScmType.GITHUB.name
            )
            repositoryGithubDao.create(
                dslContext = transactionContext,
                repositoryId = repositoryId,
                projectName = repository.projectName,
                userName = userId,
                gitProjectId = getProjectId(repository, userId)
            )
        }
        return repositoryId
    }

    override fun edit(
        userId: String,
        projectId: String,
        repositoryHashId: String,
        repository: GithubRepository,
        record: TRepositoryRecord
    ) {
        // 提交的参数与数据库中类型不匹配
        if (record.type != ScmType.GITHUB.name) {
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = GITHUB_INVALID,
                    language = I18nUtil.getLanguage(userId)
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
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        val sourceUrl = repositoryDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repositoryId = repositoryId
        ).url
        var gitProjectId: Long? = null
        if (sourceUrl != repository.url || repository.gitProjectId == null || repository.gitProjectId == 0L) {
            logger.info(
                "repository url unMatch,need change gitProjectId,sourceUrl=[$sourceUrl] " +
                    "targetUrl=[${repository.url}]"
            )
            // Git项目ID
            gitProjectId = getProjectId(repository, userId)
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
            repositoryGithubDao.edit(
                transactionContext,
                repositoryId,
                repository.projectName,
                repository.userName,
                gitProjectId = gitProjectId
            )
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

    override fun compose(repository: TRepositoryRecord): GithubRepository {
        val record = repositoryGithubDao.get(dslContext, repository.repositoryId)
        return GithubRepository(
            aliasName = repository.aliasName,
            url = repository.url,
            userName = record.userName,
            projectName = record.projectName,
            projectId = repository.projectId,
            repoHashId = HashUtil.encodeOtherLongId(repository.repositoryId),
            gitProjectId = record.gitProjectId.toLong(),
            enablePac = repository.enablePac,
            yamlSyncStatus = repository.yamlSyncStatus,
            scmCode = repository.scmCode ?: ScmType.GITHUB.name
        )
    }

    override fun getRepoDetailMap(repositoryIds: List<Long>): Map<Long, RepositoryDetailInfo> {
        return repositoryIds.associateWith {
            RepositoryDetailInfo(authType = RepoAuthType.OAUTH.name, credentialId = "")
        }
    }

    private fun getProjectId(repository: GithubRepository, userId: String): Long {
        val accessToken = githubTokenService.getAccessToken(userId)
        // github仓库基本信息
        val githubRepo = if (accessToken == null) {
            logger.warn("The user[$userId] token invalid")
            null
        } else {
            try {
                githubRepositoryService.getRepository(
                    request = GetRepositoryRequest(
                        repoName = repository.projectName
                    ),
                    token = accessToken.accessToken
                )
            } catch (ignored: Exception) {
                logger.warn(
                    "get github project info failed,projectName=[${repository.projectName}] | $ignored"
                )
                null
            }
        }
        return githubRepo?.id ?: 0L
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGithubRepositoryService::class.java)
    }

    override fun getPacProjectId(userId: String, repoUrl: String): String? = null

    override fun pacCheckEnabled(
        projectId: String,
        userId: String,
        record: TRepositoryRecord,
        retry: Boolean
    ) = Unit

    override fun getGitFileTree(
        projectId: String,
        userId: String,
        record: TRepositoryRecord
    ) = emptyList<GitFileInfo>()

    override fun getPacRepository(externalId: String): TRepositoryRecord? = null

    override fun addResourceAuthorization(
        projectId: String,
        userId: String,
        repositoryId: Long,
        repository: GithubRepository
    ) {
        with(repository) {
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

    override fun listByCondition(
        repoCondition: RepoCondition,
        limit: Int,
        offset: Int
    ): List<Repository>? {
        return repositoryGithubDao.listByCondition(
            dslContext = dslContext,
            repoCondition = repoCondition,
            limit = limit,
            offset = offset
        )
    }

    override fun countByCondition(repoCondition: RepoCondition): Long {
        return repositoryGithubDao.countByCondition(
            dslContext = dslContext,
            repoCondition = repoCondition
        )
    }
}
