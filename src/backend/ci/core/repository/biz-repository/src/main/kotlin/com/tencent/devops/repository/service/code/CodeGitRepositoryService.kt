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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.dao.RepositoryCodeGitDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.service.CredentialService
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.scm.IGitService
import com.tencent.devops.repository.service.scm.IScmService
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class CodeGitRepositoryService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryCodeGitDao: RepositoryCodeGitDao,
    private val dslContext: DSLContext,
    private val credentialService: CredentialService,
    private val scmService: IScmService,
    private val gitService: IGitService,
    private val gitOauthService: IGitOauthService
) : CodeRepositoryService<CodeGitRepository> {
    override fun repositoryType(): String {
        return CodeGitRepository::class.java.name
    }

    override fun create(projectId: String, userId: String, token: String, repository: CodeGitRepository): Long {
        var repositoryId: Long = 0L
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            repositoryId = repositoryDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                userId = userId,
                aliasName = repository.aliasName,
                url = repository.getFormatURL(),
                type = ScmType.CODE_GIT
            )
            var accessToken = StringUtils.EMPTY
            // OAUTH授权需获取accessToken
            if (repository.authType == RepoAuthType.OAUTH) {
                accessToken = gitOauthService.getAccessToken(userId = userId)?.accessToken ?: StringUtils.EMPTY
            }
            // Git项目ID
            val gitProjectId =
                getGitProjectId(repo = repository, token = StringUtils.defaultIfBlank(accessToken, token)).toString()
            repositoryCodeGitDao.create(
                dslContext = dslContext,
                repositoryId = repositoryId,
                projectName = GitUtils.getProjectName(repository.url),
                userName = repository.userName,
                credentialId = repository.credentialId,
                authType = repository.authType,
                gitProjectId = gitProjectId
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
        // 提交的参数与数据库中类型不匹配
        if (!StringUtils.equals(record.type, ScmType.CODE_GIT.name)) {
            throw OperationException(MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.GIT_INVALID))
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
            repositoryCodeGitDao.edit(
                dslContext = transactionContext,
                repositoryId = repositoryId,
                projectName = GitUtils.getProjectName(repository.url),
                userName = repository.userName,
                credentialId = repository.credentialId,
                authType = repository.authType
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
            repoHashId = HashUtil.encodeOtherLongId(repository.repositoryId)
        )
    }

    override fun getToken(credentialList: List<String>, repository: CodeGitRepository): String {
        var token: String = StringUtils.EMPTY
        token = when (repository.authType) {
            RepoAuthType.SSH -> {
                credentialList[0]
            }
            RepoAuthType.HTTP -> {
                credentialList[0]
            }
            else -> {
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.REPO_TYPE_NO_NEED_CERTIFICATION,
                    params = arrayOf(repository.authType!!.name)
                )
            }
        }
        return token
    }

    override fun checkToken(
        credentialList: List<String>,
        repository: CodeGitRepository,
        credentialType: CredentialType
    ): TokenCheckResult {
        val token = getToken(credentialList = credentialList, repository = repository)
        val checkResult: TokenCheckResult = when (repository.authType) {
            RepoAuthType.SSH -> {
                val privateKey = getPrivateKey(credentialList = credentialList)
                val passPhrase = getPassPhrase(credentialList = credentialList)
                scmService.checkPrivateKeyAndToken(
                    projectName = repository.projectName,
                    url = repository.getFormatURL(),
                    type = ScmType.CODE_GIT,
                    privateKey = privateKey,
                    passPhrase = passPhrase,
                    token = token,
                    region = null,
                    userName = repository.userName
                )
            }
            RepoAuthType.HTTP -> {
                val username = getUsername(credentialList = credentialList)
                val password = getPassword(credentialList = credentialList)
                scmService.checkUsernameAndPassword(
                    projectName = repository.projectName,
                    url = repository.getFormatURL(),
                    type = ScmType.CODE_GIT,
                    username = username,
                    password = password,
                    token = token,
                    region = null,
                    repoUsername = repository.userName
                )
            }
            else -> {
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.REPO_TYPE_NO_NEED_CERTIFICATION,
                    params = arrayOf(repository.authType!!.name)
                )
            }
        }
        return checkResult
    }

    override fun getAuthMap(repositoryRecordList: Result<TRepositoryRecord>): Map<Long, Record> {
        val gitAuthMap: MutableMap<Long, Record> = HashMap()
        val gitRepoIds = repositoryRecordList.filter {
            it.type == ScmType.CODE_GIT.name ||
                    it.type == ScmType.CODE_TGIT.name
        }.map { it.repositoryId }.toSet()
        repositoryCodeGitDao.list(dslContext, gitRepoIds)?.forEach { gitAuthMap.put(it.repositoryId, it) }
        return gitAuthMap
    }

    override fun needCheckToken(repository: CodeGitRepository): Boolean {
        // 若授权类型不为OAUTH则需要检查Token
        return repository.authType != RepoAuthType.OAUTH
    }

    override fun getCredentialInfo(
        projectId: String,
        repository: CodeGitRepository
    ): Pair<List<String>, CredentialType> {
        return credentialService.getCredentialInfo(projectId = projectId, repository = repository)
    }

    fun getPrivateKey(credentialList: List<String>): String {
        if (credentialList.size < 2) {
            throw OperationException(
                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY)
            )
        }
        val privateKey = credentialList[1]
        if (privateKey.isEmpty()) {
            throw OperationException(
                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_SECRET_EMPTY)
            )
        }
        return privateKey
    }

    fun getPassPhrase(credentialList: List<String>): String? {
        val passPhrase = if (credentialList.size > 2) {
            val p = credentialList[2]
            p.ifEmpty { null }
        } else {
            null
        }
        return passPhrase
    }

    /**
     * 获取用户名
     */
    fun getUsername(credentialList: List<String>): String {
        if (credentialList.size < 2) {
            throw OperationException(
                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
            )
        }
        val username = credentialList[1]
        if (username.isEmpty()) {
            throw OperationException(
                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.USER_NAME_EMPTY)
            )
        }
        return username
    }

    /**
     * 获取用户名
     */
    fun getPassword(credentialList: List<String>): String {
        if (credentialList.size < 3) {
            throw OperationException(
                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
            )
        }
        val password = credentialList[2]
        if (password.isEmpty()) {
            throw OperationException(
                message = MessageCodeUtil.getCodeLanMessage(RepositoryMessageCode.PWD_EMPTY)
            )
        }
        return password
    }

    /**
     * 获取Git项目ID
     */
    fun getGitProjectId(repo: CodeGitRepository, token: String): Int {
        logger.info("the repo is:$repo")
        // 根据仓库授权类型匹配Token类型
        val tokenType = if (repo.authType == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
        val gitProjectInfo = gitService.getGitProjectInfo(id = repo.projectName, token = token, tokenType = tokenType)
        logger.info("the gitProjectInfo is:$gitProjectInfo")
        return gitProjectInfo.data?.id ?: -1
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeGitRepositoryService::class.java)
    }
}
