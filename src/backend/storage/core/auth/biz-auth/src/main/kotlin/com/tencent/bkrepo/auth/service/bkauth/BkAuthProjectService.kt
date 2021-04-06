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

package com.tencent.bkrepo.auth.service.bkauth

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.tencent.bkrepo.auth.config.BkAuthConfig
import com.tencent.bkrepo.auth.pojo.BkAuthResponse
import com.tencent.bkrepo.auth.pojo.enums.BkAuthServiceCode
import com.tencent.bkrepo.auth.util.HttpUtils
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class BkAuthProjectService @Autowired constructor(
    private val bkAuthConfig: BkAuthConfig,
    private val bkAuthTokenService: BkAuthTokenService
) {
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(3L, TimeUnit.SECONDS)
        .readTimeout(5L, TimeUnit.SECONDS)
        .writeTimeout(5L, TimeUnit.SECONDS)
        .build()

    private val projectPermissionCache = CacheBuilder.newBuilder()
        .maximumSize(20000)
        .expireAfterWrite(40, TimeUnit.SECONDS)
        .build<String, Boolean>()

    fun isProjectMember(user: String, projectCode: String, retryIfTokenInvalid: Boolean = false): Boolean {
        val cacheKey = "$user::$projectCode"
        val cacheResult = projectPermissionCache.getIfPresent(cacheKey)
        if (cacheResult != null) {
            logger.debug("match in cache: $cacheKey|$cacheResult")
            return cacheResult
        }

        val accessToken = bkAuthTokenService.getAccessToken(BkAuthServiceCode.ARTIFACTORY)
        val url = "${bkAuthConfig.getBkAuthServer()}/projects/$projectCode/users/$user/verfiy?access_token=$accessToken"
        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "")
        val request = Request.Builder().url(url).post(body).build()
        val apiResponse = HttpUtils.doRequest(okHttpClient, request, 2)
        val responseObject = objectMapper.readValue<BkAuthResponse<Any>>(apiResponse.content)
        if (responseObject.code != 0) {
            if (responseObject.code == 403 && retryIfTokenInvalid) {
                bkAuthTokenService.getAccessToken(BkAuthServiceCode.ARTIFACTORY, accessToken)
                return isProjectMember(user, projectCode, false)
            }
            if (responseObject.code == 400) {
                logger.info("user[$user] not member of project $projectCode")
                projectPermissionCache.put(cacheKey, false)
                return false
            }
            logger.error("verify project member failed. ${apiResponse.content}")
            throw RuntimeException("verify project member failed")
        }
        projectPermissionCache.put(cacheKey, true)
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
