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

package com.tencent.devops.repository.service.github

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.RepoAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.GithubTokenDao
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.repository.pojo.oauth.GithubTokenType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import jakarta.ws.rs.core.Response

@Service
class GithubTokenService @Autowired constructor(
    private val dslContext: DSLContext,
    private val githubTokenDao: GithubTokenDao,
    private val client: Client,
    private val authProjectApi: AuthProjectApi,
    private val repoAuthServiceCode: RepoAuthServiceCode
) {
    @Value("\${aes.github:#{null}}")
    private val aesKey = ""

    /**
     * 保存token
     * @param userId server端的用户名
     * @param operator 蓝盾平台操作人用户名
     */
    fun createAccessToken(
        userId: String,
        accessToken: String,
        tokenType: String,
        scope: String,
        githubTokenType: GithubTokenType = GithubTokenType.GITHUB_APP,
        operator: String
    ) {
        val encryptedAccessToken = BkCryptoUtil.encryptSm4ButAes(aesKey, accessToken)
        val githubTokenRecord = githubTokenDao.getOrNull(dslContext, operator, githubTokenType)
        if (githubTokenRecord == null) {
            githubTokenDao.create(
                dslContext = dslContext,
                userId = userId,
                accessToken = encryptedAccessToken,
                tokenType = tokenType,
                scope = scope,
                githubTokenType = githubTokenType,
                operator = operator
            )
        } else {
            if (githubTokenRecord.operator != operator) {
                logger.info(
                    "the operator of the gitHub token has changed|userId=$userId|" +
                            "operator=$operator|oldOperator=${githubTokenRecord.operator}"
                )
            }
            githubTokenDao.update(
                dslContext = dslContext,
                userId = userId,
                accessToken = encryptedAccessToken,
                tokenType = tokenType,
                scope = scope,
                githubTokenType = githubTokenType,
                operator = operator
            )
        }
    }

    fun deleteAccessToken(userId: String) {
        githubTokenDao.delete(dslContext, userId)
    }

    fun getAccessToken(
        userId: String,
        tokenType: GithubTokenType = GithubTokenType.GITHUB_APP
    ): GithubToken? {
        val githubTokenRecord = githubTokenDao.getOrNull(dslContext, userId, tokenType) ?: return null
        return GithubToken(
            BkCryptoUtil.decryptSm4OrAes(aesKey, githubTokenRecord.accessToken),
            githubTokenRecord.tokenType,
            githubTokenRecord.scope,
            githubTokenRecord.createTime.timestampmilli(),
            githubTokenRecord.userId,
            githubTokenRecord.operator
        )
    }

    fun checkAndGetAccessToken(projectId: String, buildId: String, userId: String): GithubToken? {
        logger.info("buildId: $buildId, userId: $userId")
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(projectId, buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to get the basic information based on the buildId: $buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data
            ?: throw RemoteServiceException("Failed to get the basic information based on the buildId: $buildId")
        val projectUserCheck = authProjectApi.checkProjectUser(
            user = userId,
            serviceCode = repoAuthServiceCode,
            projectCode = buildBasicInfo.projectId
        )
        if (!projectUserCheck) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                params = arrayOf(userId, buildBasicInfo.projectId)
            )
        }
        return getAccessToken(userId)
    }

    @Throws(CustomException::class)
    fun getAccessTokenMustExist(userId: String): GithubToken {
        return getAccessToken(userId)
            ?: throw CustomException(status = Response.Status.NOT_FOUND, message = "$userId githubToken not exist")
    }

    companion object {
        val logger = LoggerFactory.getLogger(GithubTokenService::class.java)
    }
}
