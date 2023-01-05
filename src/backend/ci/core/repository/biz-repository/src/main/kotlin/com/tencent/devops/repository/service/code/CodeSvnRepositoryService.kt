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

import com.tencent.devops.common.api.constant.RepositoryMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.repository.tables.TRepositoryCodeSvn
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.dao.RepositoryCodeSvnDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.service.CredentialService
import com.tencent.devops.repository.service.scm.IScmService
import com.tencent.devops.repository.utils.CredentialUtils
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CodeSvnRepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryCodeSvnDao: RepositoryCodeSvnDao,
    private val dslContext: DSLContext,
    private val scmService: IScmService,
    private val credentialService: CredentialService
) : CodeRepositoryService<CodeSvnRepository> {
    override fun repositoryType(): String {
        return CodeSvnRepository::class.java.name
    }

    override fun create(projectId: String, userId: String, token: String, repository: CodeSvnRepository): Long {
        var repositoryId: Long = 0L
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            repositoryId = repositoryDao.create(
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
        }
        return repositoryId
    }

    override fun edit(
        userId: String,
        projectId: String,
        repositoryHashId: String,
        repository: CodeSvnRepository,
        record: TRepositoryRecord
    ) {
        // 提交的参数与数据库中类型不匹配
        if (!StringUtils.equals(record.type, ScmType.CODE_SVN.name)) {
            throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.SVN_INVALID))
        }
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
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
    }

    override fun compose(repository: TRepositoryRecord): Repository {
        val record = repositoryCodeSvnDao.get(dslContext, repository.repositoryId)
        return CodeSvnRepository(
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
            svnType = record.svnType
        )
    }

    override fun checkToken(
        credentialList: List<String>,
        repository: CodeSvnRepository,
        credentialType: CredentialType
    ): TokenCheckResult {
        val svnCredential = CredentialUtils.getCredential(repository, credentialList, credentialType)
        return scmService.checkPrivateKeyAndToken(
            projectName = repository.projectName,
            url = repository.getFormatURL(),
            type = ScmType.CODE_SVN,
            privateKey = svnCredential.privateKey,
            passPhrase = svnCredential.passPhrase,
            token = null,
            region = repository.region,
            userName = svnCredential.username
        )
    }

    override fun getAuthMap(
        repositoryRecordList: Result<TRepositoryRecord>
    ): Map<Long, Record> {
        val svnAuthMap: MutableMap<Long, Record> = HashMap()
        val svnRepoIds =
            repositoryRecordList.filter { it.type == ScmType.CODE_SVN.name }
                    .map { it.repositoryId }.toSet()
        repositoryCodeSvnDao.list(dslContext, svnRepoIds)
                .forEach { svnAuthMap.put(it.repositoryId, it) }
        return svnAuthMap
    }

    override fun getAuth(
        authMap: Map<String, Map<Long, Record>>,
        repository: TRepositoryRecord
    ): Pair<String, String?> {
        val svnRepo = authMap[ScmType.CODE_SVN.name]?.get(repository.repositoryId)
        return Pair(
            (svnRepo?.get(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN.SVN_TYPE)?.toUpperCase() ?: RepoAuthType.SSH.name),
            svnRepo?.get(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN.CREDENTIAL_ID)
        )
    }

    override fun needCheckToken(repository: CodeSvnRepository): Boolean {
        return true
    }

    override fun getCredentialInfo(
        projectId: String,
        repository: CodeSvnRepository
    ): Pair<List<String>, CredentialType> {
        return credentialService.getCredentialInfo(projectId = projectId, repository = repository)
    }
}