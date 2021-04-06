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
import com.tencent.bkrepo.auth.pojo.BkAuthPermissionRequest
import com.tencent.bkrepo.auth.pojo.BkAuthResponse
import com.tencent.bkrepo.auth.pojo.enums.BkAuthPermission
import com.tencent.bkrepo.auth.pojo.enums.BkAuthResourceType
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
class BkAuthService @Autowired constructor(
    private val bkAuthConfig: BkAuthConfig,
    private val bkAuthTokenService: BkAuthTokenService
) {
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(3L, TimeUnit.SECONDS)
        .readTimeout(5L, TimeUnit.SECONDS)
        .writeTimeout(5L, TimeUnit.SECONDS)
        .build()

    private val resourcePermissionCache = CacheBuilder.newBuilder()
        .maximumSize(20000)
        .expireAfterWrite(40, TimeUnit.SECONDS)
        .build<String, Boolean>()

    fun validateUserResourcePermission(
        user: String,
        serviceCode: BkAuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String,
        permission: BkAuthPermission,
        retryIfTokenInvalid: Boolean = false
    ): Boolean {
        val cacheKey = "$user::$projectCode::${resourceType.value}::$resourceCode::${permission.value}"
        val cacheResult = resourcePermissionCache.getIfPresent(cacheKey)
        if (cacheResult != null) {
            logger.debug("match in cache: $cacheKey|$cacheResult")
            return cacheResult
        }

        val accessToken = bkAuthTokenService.getAccessToken(serviceCode)
        val url = "${bkAuthConfig.getBkAuthServer()}/permission/project/service/policy/resource/user/verfiy?" +
            "access_token=$accessToken"
        val bkAuthPermissionRequest = BkAuthPermissionRequest(
            projectCode = projectCode,
            serviceCode = serviceCode.value,
            resourceCode = resourceCode,
            policyCode = permission.value,
            resourceType = resourceType.value,
            userId = user
        )
        val content = objectMapper.writeValueAsString(bkAuthPermissionRequest)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder().url(url).post(requestBody).build()
        val apiResponse = HttpUtils.doRequest(okHttpClient, request, 2)
        val responseObject = objectMapper.readValue<BkAuthResponse<String>>(apiResponse.content)
        if (responseObject.code != 0 && responseObject.code != 400) {
            if (responseObject.code == 403 && retryIfTokenInvalid) {
                bkAuthTokenService.getAccessToken(serviceCode, accessToken)
                return validateUserResourcePermission(
                    user = user,
                    serviceCode = serviceCode,
                    resourceType = resourceType,
                    projectCode = projectCode,
                    resourceCode = resourceCode,
                    permission = permission,
                    retryIfTokenInvalid = false
                )
            }
            logger.error("validate user resource permission failed. ${apiResponse.content}")
            throw RuntimeException("validate user resource permission failed")
        }
        val hasPermission = responseObject.code == 0
        resourcePermissionCache.put(cacheKey, hasPermission)
        return hasPermission
    }

    fun getUserResourceByPermission(
        user: String,
        serviceCode: BkAuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        permission: BkAuthPermission,
        supplier: (() -> List<String>)?,
        retryIfTokenInvalid: Boolean = false
    ): List<String> {
        val accessToken = bkAuthTokenService.getAccessToken(serviceCode)
        val url = "${bkAuthConfig.getBkAuthServer()}/permission/project/service/policy/user/query/resources?" +
            "access_token=$accessToken&user_id=$user&project_code=$projectCode&service_code=${serviceCode.value}" +
            "&resource_type=${resourceType.value}&policy_code=${permission.value}&is_exact_resource=1"
        val request = Request.Builder().url(url).get().build()
        val apiResponse = HttpUtils.doRequest(okHttpClient, request, 2)
        val responseObject = objectMapper.readValue<BkAuthResponse<List<String>>>(apiResponse.content)
        if (responseObject.code != 0) {
            if (responseObject.code == 400) {
                return listOf()
            }
            if (responseObject.code == 403 && retryIfTokenInvalid) {
                bkAuthTokenService.getAccessToken(serviceCode, accessToken)
                return getUserResourceByPermission(
                    user = user,
                    serviceCode = serviceCode,
                    resourceType = resourceType,
                    projectCode = projectCode,
                    permission = permission,
                    supplier = null,
                    retryIfTokenInvalid = false
                )
            }
            logger.error("get user resource permission failed. ${apiResponse.content}")
            throw RuntimeException("get user resource permission failed")
        }
        return responseObject.data ?: listOf()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthService::class.java)
    }
}
