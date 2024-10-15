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
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.DEVX_HEADER_CDS_TOKEN
import com.tencent.devops.common.api.auth.DEVX_HEADER_NGGW_CLIENT_ADDRESS
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.filter.manager.ApiFilterFlowState
import com.tencent.devops.openapi.filter.manager.ApiFilterManager
import com.tencent.devops.openapi.filter.manager.FilterContext
import com.tencent.devops.remotedev.api.service.ServiceSDKResource
import com.tencent.devops.remotedev.pojo.CdsToken
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RemoteDevCdsTokenApiFilter(
    private val client: Client
) : ApiFilterManager {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevCdsTokenApiFilter::class.java)
        private const val CACHE_MAX_SIZE = 10000L
        private const val CACHE_EXPIRE_MINUTES = 1L
    }

    private val cdsCache = Caffeine.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build<String, CdsToken> { content ->
            kotlin.runCatching {
                client.get(ServiceSDKResource::class).checkCdsToken(
                    cdsToken = content
                ).data
            }.onFailure {
                logger.error("get cdsTokenCheck fail.|$content", it)
            }.getOrNull() ?: CdsToken(projectId = "", userId = "", regionId = "", hostName = "")
        }

    /*返回true时执行check逻辑*/
    override fun canExecute(requestContext: FilterContext): Boolean {
        return !requestContext.requestContext.headers.getFirst(DEVX_HEADER_CDS_TOKEN).isNullOrBlank()
//            || !requestContext.requestContext.headers.getFirst(DEVX_HEADER_NGGW_CLIENT_ADDRESS).isNullOrBlank()
    }

    override fun verify(requestContext: FilterContext): ApiFilterFlowState {
        val bkCdsToken = requestContext.requestContext.headers.getFirst(DEVX_HEADER_CDS_TOKEN)

        if (bkCdsToken.isNullOrBlank()) {
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                    .entity("Desktop sdk illegal access openapi.").build()
            )
            return ApiFilterFlowState.BREAK
        }

        val cache = cdsCache.get(bkCdsToken)
        if (cache?.hostName.isNullOrBlank()) {
            logger.info("Desktop sdk access openapi with illegal cds token($bkCdsToken).")
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Desktop sdk access openapi with illegal cds token($bkCdsToken).").build()
            )
            return ApiFilterFlowState.BREAK
        }
        val ip = cache?.hostName?.substringAfter(".") ?: ""
        val user = cache?.userId ?: ""
        requestContext.requestContext.headers[DEVX_HEADER_NGGW_CLIENT_ADDRESS]?.set(0, null)
        if (requestContext.requestContext.headers[DEVX_HEADER_NGGW_CLIENT_ADDRESS] != null) {
            requestContext.requestContext.headers[DEVX_HEADER_NGGW_CLIENT_ADDRESS]?.set(0, ip)
        } else {
            requestContext.requestContext.headers.add(DEVX_HEADER_NGGW_CLIENT_ADDRESS, ip)
        }

        requestContext.requestContext.headers[AUTH_HEADER_USER_ID]?.set(0, null)
        if (requestContext.requestContext.headers[AUTH_HEADER_USER_ID] != null) {
            requestContext.requestContext.headers[AUTH_HEADER_USER_ID]?.set(0, user)
        } else {
            requestContext.requestContext.headers.add(AUTH_HEADER_USER_ID, user)
        }

        return ApiFilterFlowState.CONTINUE
    }
}
