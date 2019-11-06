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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthServiceCode
import com.tencent.devops.common.auth.code.BSRepoAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.repository.tables.TRepositoryGtiToken
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.repository.dao.GitTokenDao
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.repository.service.RepostioryScmService
import com.tencent.devops.scm.api.ServiceGitResource
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLDecoder
import java.net.URLEncoder
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

@Service
class GitOauthService @Autowired constructor(
        private val dslContext: DSLContext,
        private val gitTokenDao: GitTokenDao,
        private val client: Client,
        private val redisOperation: RedisOperation,
        private val repositoryScmService: RepostioryScmService,
        private val bsAuthProjectApi: BSAuthProjectApi,
        private val bsRepoAuthServiceCode: BSRepoAuthServiceCode
) {

    @Value("\${aes.git:#{null}}")
    private val aesKey: String? = ""

    companion object {
        val logger = LoggerFactory.getLogger(GitOauthService::class.java)
    }

    fun getProject(userId: String, projectId: String, repoHashId: String?): AuthorizeResult {
        // 1. 获取accessToken，没有就返回403
        val authParams = mapOf(
                "projectId" to projectId,
                "userId" to userId,
                "repoId" to if (!repoHashId.isNullOrBlank()) HashUtil.decodeOtherIdToLong(repoHashId!!).toString() else "",
                "randomStr" to "BK_DEVOPS__${RandomStringUtils.randomAlphanumeric(8)}"
        )
        val accessToken = getAccessToken(userId) ?: return AuthorizeResult(403, getAuthUrl(authParams))
        val authResult = AuthorizeResult(200, "")
        return try {
            authResult.project.addAll(repositoryScmService.getProject(accessToken.accessToken, userId))
            authResult
        } catch (e: Exception) {
            logger.info("get oauth project fail: ${e.message}")
            AuthorizeResult(403, getAuthUrl(authParams))
        }
    }

    fun isOAuth(userId: String, redirectUrlType: RedirectUrlTypeEnum?, atomCode: String? = null): AuthorizeResult {
        logger.info("isOAuth userId is: $userId,redirectUrlType is: $redirectUrlType")
        val authParams = mapOf(
                "userId" to userId,
                "redirectUrlType" to redirectUrlType?.type,
                "atomCode" to atomCode,
                "randomStr" to "BK_DEVOPS__${RandomStringUtils.randomAlphanumeric(8)}"
        )
        val accessToken = getAccessToken(userId) ?: return AuthorizeResult(403, getAuthUrl(authParams))
        logger.info("isOAuth accessToken is: $accessToken")
        return AuthorizeResult(200, "")
    }

//    private fun getAuthUrl(projectId: String?, userId: String, repoHashId: String?, redirectUrlType: String?): String {
//        return client.get(ServiceGitResource::class).getAuthUrl(userId, projectId, repoHashId, redirectUrlType).data
//                ?: ""
//    }

    private fun getAuthUrl(authParams: Map<String, String?>): String {
        val authParamJsonStr = URLEncoder.encode(JsonUtil.toJson(authParams), "UTF-8")
        logger.info("getAuthUrl authParamJsonStr is: $authParamJsonStr")
        return repositoryScmService.getAuthUrl(authParamJsonStr)
    }

    fun gitCallback(code: String, state: String): Response {
        if (!state.contains("BK_DEVOPS__")) {
            throw OperationException("TGIT call back contain invalid parameter: $state")
        }
        val authParamDecodeJsonStr = URLDecoder.decode(state, "UTF-8")
        val authParams = JsonUtil.toMap(authParamDecodeJsonStr)
        val userId = authParams["userId"] as String
        val token = repositoryScmService.getToken(userId, code) ?: throw RuntimeException("get token fail")
        saveAccessToken(userId, token)
        val redirectUrl = repositoryScmService.getRedirectUrl(state)
        logger.info("gitCallback redirectUrl is: $redirectUrl")
        return Response.temporaryRedirect(UriBuilder.fromUri(redirectUrl).build()).build()
    }

    fun checkAndGetAccessToken(buildId: String, userId: String): GitToken? {
        logger.info("buildId: $buildId, userId: $userId")
        val buildBasicInfoResult = client.get(ServiceBuildResource::class).serviceBasic(buildId)
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to get the basic information based on the buildId: $buildId")
        }
        val buildBasicInfo = buildBasicInfoResult.data ?: throw RemoteServiceException("Failed to get the basic information based on the buildId: $buildId")
        val projectUsers = bsAuthProjectApi.getProjectUsers(bsRepoAuthServiceCode, buildBasicInfo.projectId)
        logger.info("projectId: ${buildBasicInfo.projectId}, projectUsers: $projectUsers")
        if (!projectUsers.contains(userId)) {
            throw RemoteServiceException("user permission denied: userId=$userId, projectCode=${buildBasicInfo.projectId}")
        }
        return getAccessToken(userId)
    }

    fun getAccessToken(userId: String): GitToken? {

        val accessToken = doGetAccessToken(userId) ?: return null

        // 提前半个小时刷新token
        return if (accessToken.expiresIn * 1000 <= System.currentTimeMillis() - 1800 * 1000) {
            val lock = RedisLock(redisOperation, "OAUTH_REFRESH_TOKEN_$userId", 60L)
            lock.use {
                lock.lock()
                val newAccessToken = doGetAccessToken(userId)!!
                if (newAccessToken.expiresIn * 1000 <= System.currentTimeMillis() - 1800 * 1000) {
                    refreshToken(userId, newAccessToken)
                } else {
                    newAccessToken
                }
            }
        } else {
            accessToken
        }
    }

    private fun doGetAccessToken(userId: String): GitToken? {
        return gitTokenDao.getAccessToken(dslContext, userId)?.map {
            with(TRepositoryGtiToken.T_REPOSITORY_GTI_TOKEN) {
                GitToken(
                        AESUtil.decrypt(aesKey!!, it.get(ACCESS_TOKEN)),
                        AESUtil.decrypt(aesKey!!, it.get(REFRESH_TOKEN)),
                        it.get(TOKEN_TYPE),
                        it.get(EXPIRES_IN)
                )
            }
        }
    }

    private fun refreshToken(userId: String, gitToken: GitToken): GitToken {
        val token = repositoryScmService.refreshToken(userId, gitToken)
//        val token = client.get(ServiceGitResource::class).refreshToken(userId, gitToken).data!!
        saveAccessToken(userId, token)
        token.accessToken = AESUtil.decrypt(aesKey!!, token.accessToken)
        token.refreshToken = AESUtil.decrypt(aesKey!!, token.refreshToken)
        return token
    }

    fun saveAccessToken(userId: String, tGitToken: GitToken): Int {
        tGitToken.accessToken = AESUtil.encrypt(aesKey!!, tGitToken.accessToken)
        tGitToken.refreshToken = AESUtil.encrypt(aesKey!!, tGitToken.refreshToken)
        return gitTokenDao.saveAccessToken(dslContext, userId, tGitToken)
    }

    fun deleteToken(userId: String): Int {
        return gitTokenDao.deleteToken(dslContext, userId)
    }
}
