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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.constant.RepositoryMessageCode.GITHUB_INVALID
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.dao.RepositoryGithubDao
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.auth.RepoAuthInfo
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.repository.sdk.github.service.GithubRepositoryService
import com.tencent.devops.repository.service.github.GithubTokenService
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CodeGithubRepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryGithubDao: RepositoryGithubDao,
    private val dslContext: DSLContext,
    private val githubRepositoryService: GithubRepositoryService,
    private val githubTokenService: GithubTokenService
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
                type = ScmType.GITHUB
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
        if (sourceUrl != repository.url) {
            logger.info("repository url unMatch,need change gitProjectId,sourceUrl=[$sourceUrl] " +
                            "targetUrl=[${repository.url}]")
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
                dslContext,
                repositoryId,
                repository.projectName,
                repository.userName,
                gitProjectId = gitProjectId
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
            gitProjectId = record.gitProjectId.toLong()
        )
    }

    override fun getAuthInfo(repositoryIds: List<Long>): Map<Long, RepoAuthInfo> {
        return repositoryIds.associateWith { RepoAuthInfo(authType = RepoAuthType.OAUTH.name, credentialId = "") }
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
}
