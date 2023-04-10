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

package com.tencent.devops.repository.service.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.repository.github.service.GithubUserService
import com.tencent.devops.repository.pojo.github.GithubAppUrl
import com.tencent.devops.repository.pojo.github.GithubOauth
import com.tencent.devops.repository.pojo.github.GithubOauthCallback
import com.tencent.devops.repository.pojo.github.GithubToken
import com.tencent.devops.repository.pojo.oauth.GithubTokenType
import com.tencent.devops.scm.config.GitConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import javax.ws.rs.core.Response

@Service
@Suppress("ALL")
class GithubOAuthService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val gitConfig: GitConfig,
    private val githubTokenService: GithubTokenService,
    private val githubUserService: GithubUserService
) {

    fun getGithubOauth(projectId: String, userId: String, repoHashId: String?): GithubOauth {
        val repoId = if (!repoHashId.isNullOrBlank()) HashUtil.decodeOtherIdToLong(repoHashId).toString() else ""
        val state = "$userId,$projectId,$repoId,BK_DEVOPS__${RandomStringUtils.randomAlphanumeric(RANDOM_ALPHA_NUM)}"
        val redirectUrl = "$GITHUB_URL/login/oauth/authorize" +
            "?client_id=${gitConfig.githubClientId}&redirect_uri=${gitConfig.githubWebhookUrl}&state=$state"
        return GithubOauth(redirectUrl)
    }

    fun getGithubAppUrl() = GithubAppUrl(gitConfig.githubAppUrl)

    fun oauthUrl(redirectUrl: String, userId: String?, tokenType: GithubTokenType): String {
        val clientId = when (tokenType) {
            GithubTokenType.GITHUB_APP -> gitConfig.githubClientId
            GithubTokenType.OAUTH_APP -> gitConfig.oauthAppClientId
        }
        val stateParams = mutableMapOf(
            "redirectUrl" to redirectUrl,
            "randomStr" to RandomStringUtils.randomAlphanumeric(RANDOM_ALPHA_NUM)
        )
        // 如果非空将以该userId入库，否则会以github login name 入库
        if (userId != null) stateParams["userId"] = userId

        val state = URLEncoder.encode(JsonUtil.toJson(stateParams), "UTF-8")

        return when (tokenType) {
            GithubTokenType.GITHUB_APP -> "$GITHUB_URL/login/oauth/authorize" +
                "?client_id=$clientId&redirect_uri=${gitConfig.githubCallbackUrl}&state=$state"
            GithubTokenType.OAUTH_APP -> "$GITHUB_URL/login/oauth/authorize" +
                "?client_id=$clientId&state=$state&scope=user,repo"
        }
    }

    fun githubCallback(
        code: String,
        state: String?,
        channelCode: String? = null,
        tokenType: GithubTokenType = GithubTokenType.GITHUB_APP
    ): GithubOauthCallback {
        return if (channelCode == ChannelCode.GIT.name || state?.contains("redirectUrl") == true) {
            githubCallbackForGIT(code = code, state = state, githubTokenType = tokenType)
        } else {
            githubCallbackForBS(code = code, state = state, githubTokenType = tokenType)
        }
    }

    fun githubCallbackForBS(
        code: String,
        state: String?,
        githubTokenType: GithubTokenType = GithubTokenType.GITHUB_APP
    ): GithubOauthCallback {
        if (state.isNullOrBlank() || !state.contains(",BK_DEVOPS__")) {
            throw OperationException("TGIT call back contain invalid parameter: $state")
        }

        val arrays = state.split(",")
        val userId = arrays[0]
        val projectId = arrays[1]
        val repoHashId = if (arrays[2].isNotBlank()) HashUtil.encodeOtherLongId(arrays[2].toLong()) else ""
        val githubToken = getAccessTokenImpl(code, githubTokenType)

        githubTokenService.createAccessToken(
            userId = userId,
            accessToken = githubToken.accessToken,
            tokenType = githubToken.tokenType,
            scope = githubToken.scope,
            githubTokenType = githubTokenType
        )
        return GithubOauthCallback(
            userId = userId,
            redirectUrl = "${gitConfig.githubRedirectUrl}/$projectId#popupGithub$repoHashId"
        )
    }

    fun githubCallbackForGIT(
        code: String,
        state: String?,
        githubTokenType: GithubTokenType = GithubTokenType.GITHUB_APP
    ): GithubOauthCallback {
        logger.info("github callback for git|code:$code|state:$state")
        val githubToken = getAccessTokenImpl(code, githubTokenType)
        val userResponse = githubUserService.getUser(githubToken.accessToken)
        val stateMap = kotlin.runCatching { JsonUtil.toMap(state ?: "{}") }.getOrDefault(emptyMap())
        githubTokenService.createAccessToken(
            userId = stateMap["userId"]?.toString() ?: userResponse.login,
            accessToken = githubToken.accessToken,
            tokenType = githubToken.tokenType,
            scope = githubToken.scope,
            githubTokenType = githubTokenType
        )
        return GithubOauthCallback(
            userId = userResponse.login,
            email = userResponse.email,
            redirectUrl = stateMap["redirectUrl"]?.toString() ?: ""
        )
    }

    private fun getAccessTokenImpl(
        code: String,
        githubTokenType: GithubTokenType = GithubTokenType.GITHUB_APP
    ): GithubToken {
        val clientId = when (githubTokenType) {
            GithubTokenType.GITHUB_APP -> gitConfig.githubClientId
            GithubTokenType.OAUTH_APP -> gitConfig.oauthAppClientId
        }

        val secret = when (githubTokenType) {
            GithubTokenType.GITHUB_APP -> gitConfig.githubClientSecret
            GithubTokenType.OAUTH_APP -> gitConfig.oauthAppClientSecret
        }
        val url = "$GITHUB_URL/login/oauth/access_token" +
            "?client_id=$clientId&client_secret=$secret&code=$code"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(RequestBody.create("application/x-www-form-urlencoded;charset=utf-8".toMediaTypeOrNull(), ""))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body!!.string()
            if (!response.isSuccessful) {
                logger.info("Github get code(${response.code}) and response($data)")
                throw CustomException(
                    Response.Status.fromStatusCode(response.code)
                        ?: Response.Status.BAD_REQUEST,
                    "get Github access_token fail: $data"
                )
            }
            return objectMapper.readValue(data)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GithubOAuthService::class.java)
        private const val RANDOM_ALPHA_NUM = 8
        private const val GITHUB_URL = "https://github.com"
    }
}
