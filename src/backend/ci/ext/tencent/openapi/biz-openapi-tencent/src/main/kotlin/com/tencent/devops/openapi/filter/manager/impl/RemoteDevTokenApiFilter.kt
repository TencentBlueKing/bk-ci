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
package com.tencent.devops.openapi.filter.manager.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_STORE_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_AUTHORIZATION
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_CLIENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_CLIENT_SECRET
import com.tencent.devops.common.api.auth.DEVX_HEADER_NGGW_CLIENT_ADDRESS
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.filter.manager.ApiFilterFlowState
import com.tencent.devops.openapi.filter.manager.ApiFilterManager
import com.tencent.devops.openapi.filter.manager.FilterContext
import com.tencent.devops.remotedev.api.service.ServiceSDKResource
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RemoteDevTokenApiFilter(
    private val client: Client
) : ApiFilterManager {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevTokenApiFilter::class.java)
        private const val CACHE_MAX_SIZE = 10000L
        private const val CACHE_EXPIRE_MINUTES = 30L
    }

    private val oauthDTOCache = Caffeine.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .expireAfterAccess(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build<String, ClientDetailsDTO?> { key ->
            kotlin.runCatching {
                val (appId, hostIp) = key.split("@@")
                client.get(ServiceSDKResource::class).getAppIdOauthClientDetail(
                    desktopIP = hostIp,
                    appId = appId
                ).data
            }.onFailure { logger.warn("$key getAppIdOauthClientDetail error.", it) }.getOrNull()
        }

    /*返回true时执行check逻辑*/
    override fun canExecute(requestContext: FilterContext): Boolean {
        return if (!requestContext.requestContext.headers.getFirst(AUTH_HEADER_OAUTH2_AUTHORIZATION).isNullOrBlank() &&
            !requestContext.requestContext.headers.getFirst(AUTH_HEADER_DEVOPS_STORE_CODE).isNullOrBlank() &&
            !requestContext.requestContext.headers.getFirst(DEVX_HEADER_NGGW_CLIENT_ADDRESS).isNullOrBlank()
        ) {
            requestContext.needCheckPermissions = true
            true
        } else {
            false
        }
    }

    override fun verify(requestContext: FilterContext): ApiFilterFlowState {
        val appId = requestContext.requestContext.headers.getFirst(AUTH_HEADER_DEVOPS_STORE_CODE)
        val hostIp = requestContext.requestContext.headers.getFirst(DEVX_HEADER_NGGW_CLIENT_ADDRESS)

        val key = "$appId@@$hostIp"
        val cache = oauthDTOCache.get(key)
        if (cache == null) {
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("remotedev sdk access openapi failed").build()
            )
            return ApiFilterFlowState.BREAK
        }
        requestContext.requestContext.headers[AUTH_HEADER_OAUTH2_CLIENT_ID]?.set(0, null)
        if (requestContext.requestContext.headers[AUTH_HEADER_OAUTH2_CLIENT_ID] != null) {
            requestContext.requestContext.headers[AUTH_HEADER_OAUTH2_CLIENT_ID]?.set(0, cache.clientId)
        } else {
            requestContext.requestContext.headers.add(AUTH_HEADER_OAUTH2_CLIENT_ID, cache.clientId)
        }

        requestContext.requestContext.headers[AUTH_HEADER_OAUTH2_CLIENT_SECRET]?.set(0, null)
        if (requestContext.requestContext.headers[AUTH_HEADER_OAUTH2_CLIENT_SECRET] != null) {
            requestContext.requestContext.headers[AUTH_HEADER_OAUTH2_CLIENT_SECRET]?.set(0, cache.clientSecret)
        } else {
            requestContext.requestContext.headers.add(AUTH_HEADER_OAUTH2_CLIENT_SECRET, cache.clientSecret)
        }

        return ApiFilterFlowState.CONTINUE
    }
}
