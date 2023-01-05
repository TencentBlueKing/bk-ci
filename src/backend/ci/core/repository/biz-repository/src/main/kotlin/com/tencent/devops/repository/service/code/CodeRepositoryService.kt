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
import com.tencent.devops.model.repository.tables.TRepositoryCodeGit
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.apache.commons.lang3.StringUtils
import org.jooq.Record
import org.jooq.Result
import org.slf4j.LoggerFactory

interface CodeRepositoryService<T> {

    /**
     * 代码库类型
     */
    fun repositoryType(): String

    /**
     * 创建代码库
     */
    fun create(projectId: String, userId: String, token: String, repository: T): Long

    /**
     * 编辑代码库
     */
    fun edit(userId: String, projectId: String, repositoryHashId: String, repository: T, record: TRepositoryRecord)

    /**
     * 代码库组成
     */
    fun compose(repository: TRepositoryRecord): Repository

    /**
     * 获取token
     */
    fun getToken(credentialList: List<String>, repository: T): String {
        return StringUtils.EMPTY
    }

    /**
     * 获取授权信息
     */
    fun getAuthMap(repositoryRecordList: Result<TRepositoryRecord>): Map<Long, Record> {
        return HashMap()
    }

    /**
     * 获取授权信息
     */
    fun getAuth(
        authMap: Map<String, Map<Long, Record>>,
        repository: TRepositoryRecord
    ): Pair<String, String?> {
        val gitAuthMap = authMap[ScmType.CODE_GIT.name]
        val gitRepo = gitAuthMap?.get(repository.repositoryId)
        val gitAuthType = gitRepo?.get(TRepositoryCodeGit.T_REPOSITORY_CODE_GIT.AUTH_TYPE) ?: RepoAuthType.SSH.name
        // OAUTH取用户名，反之取凭证ID
        val gitAuthIdentity = if (gitAuthType == RepoAuthType.OAUTH.name) {
            gitRepo?.get(TRepositoryCodeGit.T_REPOSITORY_CODE_GIT.USER_NAME)
        } else {
            gitRepo?.get(TRepositoryCodeGit.T_REPOSITORY_CODE_GIT.CREDENTIAL_ID)
        }
        return Pair(gitAuthType, gitAuthIdentity)
    }

    /**
     * 是否需要检查Token
     */
    fun needCheckToken(repository: T): Boolean

    /**
     * 检查Token
     */
    fun checkToken(projectId: String, repository: T): String {
        var token = StringUtils.EMPTY
        if (needCheckToken(repository)) {
            val credentialInfo: Pair<List<String>, CredentialType> =
                getCredentialInfo(projectId = projectId, repository = repository)
            val checkResult: TokenCheckResult = checkToken(
                credentialList = credentialInfo.first,
                repository = repository,
                credentialType = credentialInfo.second
            )
            if (!checkResult.result) {
                logger.warn("Fail to check the repo token & private key because of ${checkResult.message}")
                throw OperationException(checkResult.message)
            }
            token = getToken(credentialList = credentialInfo.first, repository = repository)
        }
        return token
    }

    /**
     * 检查token
     */
    fun checkToken(credentialList: List<String>, repository: T, credentialType: CredentialType): TokenCheckResult

    /**
     * 获取凭证信息
     */
    fun getCredentialInfo(projectId: String, repository: T): Pair<List<String>, CredentialType> {
        return Pair(ArrayList(), CredentialType.PASSWORD)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CodeRepositoryService::class.java)
    }
}
