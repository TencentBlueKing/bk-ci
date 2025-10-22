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

package com.tencent.devops.repository.service.tgit

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.OauthParams
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitOauthCallback
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.pojo.Project
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDateTime

@Service
@Suppress("ALL")
class TGitOAuthService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val gitConfig: GitConfig,
    private val tGitTokenService: TGitTokenService,
    private val tGitService: ITGitService,
    private val redisOperation: RedisOperation
) {

    private fun getOauthUrl(id: String): String {
        logger.info("getAuthUrl state is: $id")
        return "${gitConfig.tGitUrl}/oauth/authorize?client_id=${gitConfig.tGitClientId}" +
            "&redirect_uri=${gitConfig.tGitWebhookUrl}&response_type=code&state=${URLEncoder.encode(id, "UTF-8")}"
    }

    private fun getOauthUrl(
        gitProjectId: Long?,
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?
    ): AuthorizeResult {
        val id = initRedisUser(
            OauthParams(
                gitProjectId = gitProjectId,
                userId = userId,
                redirectUrlType = redirectUrlType,
                redirectUrl = redirectUrl
            )
        )
        return AuthorizeResult(403, getOauthUrl(id))
    }

    fun getProject(userId: String, projectId: String, repoHashId: String?, search: String?): AuthorizeResult {
        logger.info("start to get project: userId:$userId")
        val accessToken =
            getAccessToken(userId) ?: return isOAuth(
                userId = userId,
                redirectUrlType = RedirectUrlTypeEnum.SPEC,
                redirectUrl = gitConfig.redirectUrl + "/$projectId/#popupTGit",
                refreshToken = true
            )
        val authResult = AuthorizeResult(200, "")
        return try {
            authResult.project.addAll(
                tGitService.getProjectList(
                    accessToken = accessToken.accessToken,
                    page = 1,
                    pageSize = 100,
                    search = search,
                    orderBy = null, sort = null, owned = null, minAccessLevel = null
                ).map {
                    Project(
                        id = it.id.toString(),
                        name = it.name.toString(),
                        nameWithNameSpace = it.nameWithNamespace.toString(),
                        sshUrl = it.sshUrlToRepo.toString(),
                        httpUrl = it.httpsUrlToRepo.toString(),
                        lastActivity = DateTimeUtil.convertLocalDateTimeToTimestamp(
                            LocalDateTime.parse(it.lastActivityAt.toString().removeSuffix("+0000"))
                        ) * 1000L
                    )
                }
            )
            authResult
        } catch (e: Exception) {
            logger.info("get oauth project fail: ${e.message}")
            isOAuth(
                userId = userId,
                redirectUrlType = RedirectUrlTypeEnum.SPEC,
                redirectUrl = gitConfig.redirectUrl + "/$projectId/#popupTGit",
                refreshToken = true
            )
        }
    }

    fun getProjectList(userId: String, page: Int?, pageSize: Int?): List<Project> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get project: userId:$userId")
        val accessToken = getAccessToken(userId) ?: return mutableListOf()
        return tGitService.getProjectList(
            accessToken = accessToken.accessToken,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            null, null, null, null, null
        ).map {
            Project(
                id = it.id.toString(),
                name = it.name.toString(),
                nameWithNameSpace = it.nameWithNamespace.toString(),
                sshUrl = it.sshUrlToRepo.toString(),
                httpUrl = it.httpsUrlToRepo.toString(),
                lastActivity = DateTimeUtil.convertLocalDateTimeToTimestamp(
                    LocalDateTime.parse(it.lastActivityAt.toString().removeSuffix("+0000"))
                ) * 1000L
            )
        }
    }

    fun getBranch(userId: String, repository: String, page: Int?, pageSize: Int?): List<GitBranch> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get branch: userId:$userId repository: $repository")
        val accessToken = getAccessToken(userId) ?: return mutableListOf()
        return tGitService.getBranch(
            accessToken = accessToken.accessToken,
            userId = userId,
            repository = repository,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            search = null
        )
    }

    fun getTag(userId: String, repository: String, page: Int?, pageSize: Int?): List<GitTag> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        logger.info("start to get tag: userId:$userId repository: $repository")
        val accessToken = getAccessToken(userId) ?: return mutableListOf()
        return tGitService.getTag(
            accessToken = accessToken.accessToken,
            userId = userId,
            repository = repository,
            page = pageNotNull,
            pageSize = pageSizeNotNull
        )
    }

    fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum? = RedirectUrlTypeEnum.DEFAULT,
        redirectUrl: String? = null,
        gitProjectId: Long? = null,
        refreshToken: Boolean? = null,
        validationCheck: Boolean? = false
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
        val accessToken = if (refreshToken == true) {
            null
        } else {
            tGitTokenService.getAccessToken(userId)
        } ?: kotlin.run {
            return getOauthUrl(gitProjectId, userId, redirectUrlType, redirectUrl)
        }

        if (validationCheck == true) {
            kotlin.runCatching { tGitService.getUserInfoByToken(accessToken.accessToken) }.onFailure {
                logger.info("Oauth token expired, need reauthorize.|$userId")
                return getOauthUrl(gitProjectId, userId, redirectUrlType, redirectUrl)
            }
        }
        logger.info("isOAuth accessToken is: $accessToken")
        return AuthorizeResult(200, "")
    }

    fun gitCallback(code: String, state: String): GitOauthCallback {
        val decodeId = URLDecoder.decode(state, "UTF-8")
        if (decodeId.isBlank()) throw OperationException("state can not empty.")
        val authParams = checkAndGetRedisUser(decodeId)
        logger.info("gitCallback authParams is: $authParams")
        val userId = authParams.userId
        val gitProjectId = authParams.gitProjectId
        val token = tGitService.getToken(userId, code)
        // 在oauth授权过程中,可以输入公共账号去鉴权，所以需要再验证token所属人
        val oauthUserId = tGitService.getUserInfoByToken(token.accessToken).username ?: userId
        tGitTokenService.saveAccessToken(userId, oauthUserId, token)
        val redirectUrl = getRedirectUrl(authParams)
        logger.info("gitCallback redirectUrl is: $redirectUrl")
        return GitOauthCallback(
            gitProjectId = gitProjectId,
            userId = userId,
            oauthUserId = oauthUserId,
            redirectUrl = redirectUrl
        )
    }

    fun getAccessToken(userId: String): GitToken? {
        return tGitTokenService.getAccessToken(userId)
    }

    private fun getRedirectUrl(authParam: OauthParams): String {
        val type = authParam.redirectUrlType
        val specRedirectUrl = authParam.redirectUrl
        return when (type) {
            RedirectUrlTypeEnum.SPEC -> specRedirectUrl ?: gitConfig.redirectUrl
            else -> gitConfig.redirectUrl
        }
    }

    private fun initRedisUser(params: OauthParams): String {
        val key = UUIDUtil.generate()
        redisOperation.set(
            key = REDIS_KEY + key,
            value = JsonUtil.toJson(params, false),
            expiredInSecond = EXPIRED_SECOND
        )
        return key
    }

    private fun checkAndGetRedisUser(key: String): OauthParams {
        val value = redisOperation.get(REDIS_KEY + key)
        if (value.isNullOrBlank()) {
            throw OperationException("Session is already registered or has expired.Please reapply for authorization.")
        }
        redisOperation.delete(REDIS_KEY + key)
        return JsonUtil.to(value, object : TypeReference<OauthParams>() {})
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TGitOAuthService::class.java)
        private const val REDIS_KEY = "repository_oauth_set:tgit:"
        private const val EXPIRED_SECOND = 300L
    }
}
