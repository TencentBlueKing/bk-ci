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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service.scm

import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.repository.pojo.git.GitMrInfo
import com.tencent.devops.repository.pojo.git.GitMrReviewInfo
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class GitScmService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitScmService::class.java)
    }

    fun getMergeRequestReviewersInfo(
        projectId: String,
        mrId: Long?,
        repo: Repository
    ): GitMrReviewInfo? {
        if (mrId == null) return null
        val authType = when (repo) {
            is CodeGitRepository ->
                repo.authType
            is CodeTGitRepository ->
                repo.authType
            else ->
                return null
        }

        return try {
            val tokenType = if (authType == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType
            )
            client.get(ServiceGitResource::class).getMergeRequestReviewersInfo(
                repoName = repo.projectName,
                mrId = mrId,
                tokenType = tokenType,
                token = token,
                repoUrl = repo.url
            ).data!!
        } catch (e: Exception) {
            logger.error("fail to get mr reviews info", e)
            null
        }
    }

    fun getMergeRequestInfo(
        projectId: String,
        mrId: Long?,
        repo: Repository
    ): GitMrInfo? {
        if (mrId == null) return null
        val authType = when (repo) {
            is CodeGitRepository ->
                repo.authType
            is CodeTGitRepository ->
                repo.authType
            else ->
                return null
        }

        return try {
            val tokenType = if (authType == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType
            )
            return client.get(ServiceGitResource::class).getMergeRequestInfo(
                repoName = repo.projectName,
                mrId = mrId,
                tokenType = tokenType,
                token = token,
                repoUrl = repo.url
            ).data!!
        } catch (e: Exception) {
            logger.error("fail to get mr info", e)
            null
        }
    }

    fun getMergeRequestChangeInfo(
        projectId: String,
        mrId: Long?,
        repo: Repository
    ): GitMrChangeInfo? {
        if (mrId == null) return null
        val authType = when (repo) {
            is CodeGitRepository ->
                repo.authType
            is CodeTGitRepository ->
                repo.authType
            else ->
                return null
        }

        return try {
            val tokenType = if (authType == RepoAuthType.OAUTH) TokenTypeEnum.OAUTH else TokenTypeEnum.PRIVATE_KEY
            val token = getToken(
                projectId = projectId,
                credentialId = repo.credentialId,
                userName = repo.userName,
                authType = tokenType
            )
            return client.get(ServiceGitResource::class).getMergeRequestChangeInfo(
                repoName = repo.projectName,
                mrId = mrId,
                tokenType = tokenType,
                token = token,
                repoUrl = repo.url
            ).data!!
        } catch (e: Exception) {
            logger.error("fail to get mr info", e)
            null
        }
    }

    private fun getToken(projectId: String, credentialId: String, userName: String, authType: TokenTypeEnum): String {
        return if (authType == TokenTypeEnum.OAUTH) {
            client.get(ServiceOauthResource::class).gitGet(userName).data?.accessToken ?: ""
        } else {
            val pair = DHUtil.initKey()
            val encoder = Base64.getEncoder()
            val decoder = Base64.getDecoder()
            val credentialResult = client.get(ServiceCredentialResource::class).get(
                projectId = projectId, credentialId = credentialId,
                publicKey = encoder.encodeToString(pair.publicKey)
            )
            if (credentialResult.isNotOk() || credentialResult.data == null) {
                logger.warn("Fail to get the credential($credentialId) of project($projectId) because of ${credentialResult.message}")
                throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
            }

            val credential = credentialResult.data!!

            String(DHUtil.decrypt(
                data = decoder.decode(credential.v1),
                partBPublicKey = decoder.decode(credential.publicKey),
                partAPrivateKey = pair.privateKey
            ))
        }
    }
}