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

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.RepoAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.repository.tables.TRepositoryGitToken
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.GitTokenDao
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitOauthCallback
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.pojo.Project
import java.net.URLDecoder
import java.net.URLEncoder
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class GitOauthService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitTokenDao: GitTokenDao,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val gitService: IGitService,
    private val authProjectApi: AuthProjectApi,
    private val repoAuthServiceCode: RepoAuthServiceCode
) : IGitOauthService {

    @Value("\${aes.git:#{null}}")
    private val aesKey: String? = ""

    companion object {
        val logger = LoggerFactory.getLogger(GitOauthService::class.java)
    }

    override fun getProject(userId: String, projectId: String, repoHashId: String?, search: String?): AuthorizeResult {
        logger.info("start to get project: userId:$userId")
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
            authResult.project.addAll(
                gitService.getProjectList(
                    accessToken = accessToken.accessToken,
                    userId = userId,
                    page = 1,
                    pageSize = 100,
                    search = search
                )
            )
            authResult
        } catch (e: Exception) {
            logger.info("get oauth project fail: ${e.message}")
            AuthorizeResult(403, getAuthUrl(authParams))
        }
    }

    override fun getProjectList(userId: String, page: Int?, pageSize: Int?): List<Project> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get project: userId:$userId")
        val accessToken = getAccessToken(userId) ?: return mutableListOf()
        return gitService.getProjectList(
            accessToken = accessToken.accessToken,
            userId = userId,
            page = pageNotNull,
            pageSize = pageSizeNotNull
        )
    }

    override fun getBranch(userId: String, repository: String, page: Int?, pageSize: Int?): List<GitBranch> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get branch: userId:$userId repository: $repository")
        val accessToken = getAccessToken(userId) ?: return mutableListOf()
        return gitService.getBranch(
            accessToken = accessToken.accessToken,
            userId = userId,
            repository = repository,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            search = null
        )
    }

    override fun getTag(userId: String, repository: String, page: Int?, pageSize: Int?): List<GitTag> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get tag: userId:$userId repository: $repository")
        val accessToken = getAccessToken(userId) ?: return mutableListOf()
        return gitService.getTag(
            accessToken = accessToken.accessToken,
            userId = userId,
            repository = repository,
            page = pageNotNull,
            pageSize = pageSizeNotNull
        )
    }

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long?,
        refreshToken: Boolean?
    ): AuthorizeResult {
        logger.info("isOAuth userId is: $userId,redirectUrlType is: $redirectUrlType")
        if (redirectUrlType == RedirectUrlTypeEnum.SPEC) {
            if (redirectUrl.isNullOrEmpty()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("redirectUrl")
                )
            }
        }
        val authParams = mapOf(
            "gitProjectId" to gitProjectId?.toString(),
            "userId" to userId,
            "redirectUrlType" to redirectUrlType?.type,
            "redirectUrl" to redirectUrl,
            "randomStr" to "BK_DEVOPS__${RandomStringUtils.randomAlphanumeric(8)}"
        )
        val accessToken = if (refreshToken == true) {
            null
        } else {
            getAccessToken(userId)
        } ?: return AuthorizeResult(403, getAuthUrl(authParams))
        logger.info("isOAuth accessToken is: $accessToken")
        return AuthorizeResult(200, "")
    }

    private fun getAuthUrl(authParams: Map<String, String?>): String {
        val authParamJsonStr = URLEncoder.encode(JsonUtil.toJson(authParams), "UTF-8")
        logger.info("getAuthUrl authParamJsonStr is: $authParamJsonStr")
        return gitService.getAuthUrl(authParamJsonStr = authParamJsonStr)
    }

    override fun gitCallback(code: String, state: String): GitOauthCallback {
        if (!state.contains("BK_DEVOPS__")) {
            throw OperationException("TGIT call back contain invalid parameter: $state")
        }
        val authParamDecodeJsonStr = URLDecoder.decode(state, "UTF-8")
        val authParams = JsonUtil.toMap(authParamDecodeJsonStr)
        logger.info("gitCallback authParams is: $authParams")
        val userId = authParams["userId"] as String
        val gitProjectId = authParams["gitProjectId"] as String?
        val token = gitService.getToken(userId, code)
        // 在oauth授权过程中,可以输入公共账号去鉴权，所以需要再验证token所属人
        val oauthUserId = gitService.getUserInfoByToken(token.accessToken).username ?: userId
        saveAccessToken(oauthUserId, token)
        val redirectUrl = gitService.getRedirectUrl(state)
        logger.info("gitCallback redirectUrl is: $redirectUrl")
        return GitOauthCallback(
            gitProjectId = gitProjectId?.toLong(),
            userId = userId,
            oauthUserId = oauthUserId,
            redirectUrl = redirectUrl
        )
    }

    override fun checkAndGetAccessToken(projectId: String, buildId: String, userId: String): GitToken? {
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

    override fun getAccessToken(userId: String): GitToken? {

        val accessToken = doGetAccessToken(userId) ?: return null

        return if (isTokenExpire(accessToken)) {
            logger.info("try to refresh the git token of user($userId)")
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

    private fun isTokenExpire(accessToken: GitToken): Boolean {
        // 提前半个小时刷新token
        return (accessToken.createTime ?: 0) + accessToken.expiresIn * 1000 - 1800 * 1000 <= System.currentTimeMillis()
    }

    private fun doGetAccessToken(userId: String): GitToken? {
        return gitTokenDao.getAccessToken(dslContext, userId)?.let {
            with(TRepositoryGitToken.T_REPOSITORY_GIT_TOKEN) {
                GitToken(
                    accessToken = AESUtil.decrypt(aesKey!!, it.accessToken),
                    refreshToken = AESUtil.decrypt(aesKey!!, it.refreshToken),
                    tokenType = it.tokenType,
                    expiresIn = it.expiresIn,
                    createTime = it.createTime.timestampmilli()
                )
            }
        }
    }

    private fun refreshToken(userId: String, gitToken: GitToken): GitToken {
        val token = gitService.refreshToken(userId, gitToken)
        saveAccessToken(userId, token)
        token.accessToken = AESUtil.decrypt(aesKey!!, token.accessToken)
        token.refreshToken = AESUtil.decrypt(aesKey!!, token.refreshToken)
        return token
    }

    override fun saveAccessToken(userId: String, tGitToken: GitToken): Int {
        tGitToken.accessToken = AESUtil.encrypt(aesKey!!, tGitToken.accessToken)
        tGitToken.refreshToken = AESUtil.encrypt(aesKey!!, tGitToken.refreshToken)
        return gitTokenDao.saveAccessToken(dslContext, userId, tGitToken)
    }

    override fun deleteToken(userId: String): Int {
        return gitTokenDao.deleteToken(dslContext, userId)
    }
}
