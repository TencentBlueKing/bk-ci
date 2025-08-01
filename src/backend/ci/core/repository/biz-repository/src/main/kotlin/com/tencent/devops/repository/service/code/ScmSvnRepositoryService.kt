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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.constant.RepositoryMessageCode.SVN_INVALID
import com.tencent.devops.repository.dao.RepositoryCodeSvnDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryDetailInfo
import com.tencent.devops.repository.pojo.ScmSvnRepository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.RepoCredentialType
import com.tencent.devops.repository.service.RepositoryCheckService
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.utils.code.svn.SvnUtils
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ScmSvnRepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryCodeSvnDao: RepositoryCodeSvnDao,
    private val dslContext: DSLContext,
    private val repositoryCheckService: RepositoryCheckService
) : CodeRepositoryService<ScmSvnRepository> {
    override fun repositoryType(): String {
        return ScmSvnRepository::class.java.name
    }

    override fun create(projectId: String, userId: String, repository: ScmSvnRepository): Long {
        repositoryCheckService.checkSvnCredential(
            projectId = projectId,
            authRepository = AuthRepository(repository.copy(projectId = projectId))
        )
        var repositoryId = 0L
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            repositoryId = repositoryDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                userId = userId,
                aliasName = repository.aliasName,
                url = repository.getFormatURL(),
                type = ScmType.SCM_SVN,
                enablePac = repository.enablePac,
                scmCode = repository.scmCode
            )
            // 如果repository为null，则默认为TC
            repositoryCodeSvnDao.create(
                dslContext = transactionContext,
                repositoryId = repositoryId,
                region = repository.region ?: CodeSvnRegion.TC,
                projectName = SvnUtils.getSvnProjectName(repository.getFormatURL()),
                userName = repository.userName,
                credentialId = repository.credentialId,
                svnType = repository.svnType,
                credentialType = repository.credentialType ?: RepoCredentialType.USERNAME_PASSWORD.name
            )
        }
        return repositoryId
    }

    override fun edit(
        userId: String,
        projectId: String,
        repositoryHashId: String,
        repository: ScmSvnRepository,
        record: TRepositoryRecord
    ) {
        // 提交的参数与数据库中类型不匹配
        if (!StringUtils.equals(record.type, ScmType.CODE_SVN.name)) {
            throw OperationException(I18nUtil.getCodeLanMessage(SVN_INVALID))
        }
        // 不得切换代码库
        if (diffRepoUrl(record, repository)) {
            logger.warn("can not switch repo url|sourceUrl[${record.url}]|targetUrl[${repository.url}]")
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    RepositoryMessageCode.CAN_NOT_SWITCH_REPO_URL,
                    I18nUtil.getLanguage(userId)
                )
            )
        }
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        repositoryCheckService.checkSvnCredential(
            projectId = projectId,
            authRepository = AuthRepository(repository)
        )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            repositoryDao.edit(
                dslContext = transactionContext,
                repositoryId = repositoryId,
                aliasName = repository.aliasName,
                url = repository.getFormatURL(),
                updateUser = userId
            )
            repositoryCodeSvnDao.edit(
                dslContext = transactionContext,
                repositoryId = repositoryId,
                region = repository.region ?: CodeSvnRegion.TC,
                projectName = SvnUtils.getSvnProjectName(repository.getFormatURL()),
                userName = repository.userName,
                credentialId = repository.credentialId,
                svnType = repository.svnType,
                credentialType = repository.credentialType ?: RepoCredentialType.USERNAME_PASSWORD.name
            )
        }
    }

    override fun compose(repository: TRepositoryRecord): Repository {
        val record = repositoryCodeSvnDao.get(dslContext, repository.repositoryId)
        return ScmSvnRepository(
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
            repoHashId = HashUtil.encodeOtherLongId(repository.repositoryId),
            svnType = record.svnType,
            enablePac = repository.enablePac,
            yamlSyncStatus = repository.yamlSyncStatus,
            scmCode = repository.scmCode,
            credentialType = record.credentialType
        )
    }

    override fun getRepoDetailMap(repositoryIds: List<Long>): Map<Long, RepositoryDetailInfo> {
        return repositoryCodeSvnDao.list(
            dslContext = dslContext,
            repositoryIds = repositoryIds.toSet()
        ).associateBy({ it.repositoryId }, {
            RepositoryDetailInfo(
                authType = it.svnType?.toUpperCase() ?: RepoAuthType.SSH.name,
                credentialId = it.credentialId,
                svnType = it.svnType
            )
        })
    }

    fun diffRepoUrl(
        sourceRepo: TRepositoryRecord,
        targetRepo: ScmSvnRepository
    ): Boolean {
        val sourceRepoUrl = sourceRepo.url
        val targetRepoUrl = targetRepo.url
        val sourceProjectName = SvnUtils.getSvnProjectName(sourceRepoUrl)
        val targetProjectName = SvnUtils.getSvnProjectName(targetRepoUrl)
        val targetSubPath = targetRepoUrl.substring(
            targetRepoUrl.indexOf(targetRepoUrl) +
                    targetRepoUrl.length
        )
        val sourceSubPath = targetRepoUrl.substring(
            targetRepoUrl.indexOf(targetRepoUrl) +
                    targetRepoUrl.length
        )
        return sourceProjectName != targetProjectName || targetSubPath != sourceSubPath
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
        repository: ScmSvnRepository
    ) = Unit

    override fun listByCondition(
        repoCondition: RepoCondition,
        limit: Int,
        offset: Int
    ): List<Repository>? {
        return repositoryCodeSvnDao.listByCondition(
            dslContext = dslContext,
            repoCondition = repoCondition,
            limit = limit,
            offset = offset
        ).map {
            ScmSvnRepository(
                aliasName = it.aliasName,
                url = it.url,
                credentialId = it.credentialId,
                region = it.region,
                projectName = it.projectName,
                userName = it.userName,
                projectId = it.projectId,
                repoHashId = it.repoHashId,
                svnType = it.svnType,
                enablePac = it.enablePac,
                yamlSyncStatus = it.yamlSyncStatus,
                scmCode = it.scmCode,
                credentialType = it.credentialType
            )
        }
    }

    override fun countByCondition(repoCondition: RepoCondition): Long {
        return repositoryCodeSvnDao.countByCondition(
            dslContext = dslContext,
            repoCondition = repoCondition
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmSvnRepositoryService::class.java)
    }
}
