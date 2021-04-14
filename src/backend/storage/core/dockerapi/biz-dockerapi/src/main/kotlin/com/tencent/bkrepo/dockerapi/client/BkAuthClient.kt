/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.dockerapi.client

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.bkrepo.dockerapi.config.SystemConfig
import com.tencent.bkrepo.dockerapi.util.HttpUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class BkAuthClient(
    private val systemConfig: SystemConfig
) {
    private val accessTokenCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(200, TimeUnit.SECONDS)
        .build<String, String>()

    fun getAccessToken(): String {
        val cachedToken = accessTokenCache.getIfPresent(TOKEN_CACHE_KEY)
        if (cachedToken != null) {
            return cachedToken
        }
        val accessToken = createAccessToken()
        accessTokenCache.put(TOKEN_CACHE_KEY, accessToken)
        return accessToken
    }

    fun refreshAccessToken(): String {
        logger.info("refresh access token")
        accessTokenCache.invalidate(TOKEN_CACHE_KEY)
        return getAccessToken()
    }

    fun createAccessToken(): String {
        logger.info("create access token")
        var url = "${systemConfig.bkssmServer}/api/v1/auth/access-tokens"
        val reqData = mapOf(
            "grant_type" to "client_credentials",
            "id_provider" to "client"
        )
        val httpRequest = Request.Builder().url(url)
            .header("X-BK-APP-CODE", systemConfig.appCode!!)
            .header("X-BK-APP-SECRET", systemConfig.appSecret!!)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    objectMapper.writeValueAsString(reqData)
                )
            )
            .build()
        val apiResponse = HttpUtils.doRequest(httpRequest, 2)
        val projectResponse: PaasResponse<AccessTokenData> = objectMapper.readValue(apiResponse.content)
        if (projectResponse.code != 0) {
            logger.error("get access token failed, code: ${projectResponse.code}, message: ${projectResponse.message}")
            throw RuntimeException("get access token failed")
        }
        return projectResponse.data!!.accessToken
    }

    fun checkProjectPermission(user: String, projectId: String, retry: Boolean = false): Boolean {
        logger.info("checkProjectPermission, user: $user, projectId: $projectId, retry: $retry")
        val accessToken = getAccessToken()
        val url = "${systemConfig.apigwServer}/api/apigw/bcs-app/prod/apis/projects/$projectId/user_perms/"
        val httpRequest = Request.Builder().url(url)
            .header("X-BKAPI-AUTHORIZATION", "{\"access_token\": \"$accessToken\"}")
            .header("X-BKAPI-TOKEN", accessToken)
            .header("X-BKAPI-USERNAME", user)
            .post(
                RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    "{\"action_ids\":[\"project_view\"]}"
                )
            )
            .build()
        val apiResponse = HttpUtils.doRequest(httpRequest, 2)
        val projectResponse: PaasResponse<ProjectPermission> = objectMapper.readValue(apiResponse.content)
        if (projectResponse.code != 0) {
            if (!retry && projectResponse.code == 1308406 /* access_token invalid */) {
                logger.warn("access token invalid($accessToken), retry after refresh access token")
                refreshAccessToken()
                return checkProjectPermission(user, projectId, true)
            }
            logger.error(
                "check project permission failed, code: ${projectResponse.code}," +
                    " message: ${projectResponse.message}"
            )
            throw RuntimeException("check project permission failed")
        }
        return projectResponse.data!!.projectView.allowed
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthClient::class.java)
        private const val TOKEN_CACHE_KEY = "access_token_cache_key"
    }
}
