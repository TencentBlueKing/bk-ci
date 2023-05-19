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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.constant.RepositoryMessageCode.P4_INVALID
import com.tencent.devops.repository.dao.RepositoryCodeP4Dao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.auth.RepoAuthInfo
import com.tencent.devops.repository.pojo.credential.RepoCredentialInfo
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.service.CredentialService
import com.tencent.devops.repository.service.scm.IScmService
import com.tencent.devops.scm.pojo.TokenCheckResult
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CodeP4RepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryCodeP4Dao: RepositoryCodeP4Dao,
    private val dslContext: DSLContext,
    private val scmService: IScmService,
    private val credentialService: CredentialService
) : CodeRepositoryService<CodeP4Repository> {
    override fun repositoryType(): String {
        return CodeP4Repository::class.java.name
    }

    override fun create(projectId: String, userId: String, repository: CodeP4Repository): Long {
        // 触发器可以由用户自己配置,可能存在ci不能访问p4服务器,但是需要p4服务器能访问ci,也能支持事件触发,所以p4不再校验用户名密码
        // checkCredentialInfo(projectId = projectId, repository = repository)
        var repositoryId = 0L
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            repositoryId = repositoryDao.create(
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
        }
        return repositoryId
    }

    override fun edit(
        userId: String,
        projectId: String,
        repositoryHashId: String,
        repository: CodeP4Repository,
        record: TRepositoryRecord
    ) {
        // 提交的参数与数据库中类型不匹配
        if (record.type != ScmType.CODE_P4.name) {
            throw OperationException(I18nUtil.getCodeLanMessage(P4_INVALID))
        }
        // checkCredentialInfo(projectId = projectId, repository = repository)
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
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

    override fun compose(repository: TRepositoryRecord): Repository {
        val record = repositoryCodeP4Dao.get(dslContext, repository.repositoryId)
        return CodeP4Repository(
            aliasName = repository.aliasName,
            url = repository.url,
            credentialId = record.credentialId,
            projectName = record.projectName,
            userName = record.userName,
            projectId = repository.projectId,
            repoHashId = HashUtil.encodeOtherLongId(repository.repositoryId)
        )
    }

    /**
     * 检查凭证信息
     */
    private fun checkCredentialInfo(projectId: String, repository: CodeP4Repository): RepoCredentialInfo {
        val repoCredentialInfo = getCredentialInfo(
            projectId = projectId,
            repository = repository
        )
        val checkResult = checkToken(
            repoCredentialInfo = repoCredentialInfo,
            repository = repository
        )
        if (!checkResult.result) {
            logger.warn("Fail to check the repo token & private key because of ${checkResult.message}")
            throw OperationException(checkResult.message)
        }
        return repoCredentialInfo
    }

    fun checkToken(
        repoCredentialInfo: RepoCredentialInfo,
        repository: CodeP4Repository
    ): TokenCheckResult {
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
        return scmService.checkUsernameAndPassword(
            projectName = repository.projectName,
            url = repository.getFormatURL(),
            type = ScmType.CODE_P4,
            username = repoCredentialInfo.username,
            password = repoCredentialInfo.password,
            token = "",
            region = null,
            repoUsername = repoCredentialInfo.username
        )
    }

    override fun getAuthInfo(repositoryIds: List<Long>): Map<Long, RepoAuthInfo> {
        return repositoryCodeP4Dao.list(
            dslContext = dslContext,
            repositoryIds = repositoryIds.toSet()
        )?.associateBy({ it -> it.repositoryId }, {
            RepoAuthInfo(RepoAuthType.HTTP.name, it.credentialId)
        }) ?: mapOf()
    }

    /**
     * 获取凭证信息
     */
    fun getCredentialInfo(projectId: String, repository: CodeP4Repository): RepoCredentialInfo {
        // 凭证信息
        return credentialService.getCredentialInfo(
            projectId = projectId,
            repository = repository
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeP4RepositoryService::class.java)
    }
}
