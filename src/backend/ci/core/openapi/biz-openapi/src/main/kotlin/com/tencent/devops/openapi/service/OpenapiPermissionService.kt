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
package com.tencent.devops.openapi.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import io.swagger.annotations.ApiOperation
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class OpenapiPermissionService(
    private val client: Client,
    private val clientTokenService: ClientTokenService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OpenapiPermissionService::class.java)
        private const val CACHE_SIZE = 100000L
        private const val CACHE_EXPIRE_MIN = 5L
    }

    private val projectCache = Caffeine.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRE_MIN))
        .build<String, String>()

    fun validProjectPermission(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        method: MethodSignature
    ) {
        if (userId == null) {
            val tags = method.method.getAnnotation(ApiOperation::class.java)?.tags?.joinToString(separator = "|")
            logger.warn(
                "validProjectPermission|user_is_null|" +
                    "$apigwType|$appCode|$projectId|${tags ?: method}"
            )
            return
//            throw ErrorCodeException(
//                statusCode = Response.Status.FORBIDDEN.statusCode,
//                errorCode = ERROR_NEED_PARAM_,
//                defaultMessage = "X-DEVOPS-UID cannot be empty, please put it in header",
//                params = arrayOf("X-DEVOPS-UID cannot be empty, please put it in header")
//            )
        }
        if (projectCache.getIfPresent("${userId}_$projectId") != null) return
        val hasViewPermission = kotlin.runCatching {
            client.get(ServiceProjectAuthResource::class)
                .isProjectUser(
                    token = clientTokenService.getSystemToken(null) ?: "",
                    type = null,
                    userId = userId,
                    projectCode = projectId
                ).data
        }.getOrNull() ?: false

        if (!hasViewPermission) {
            val tags = method.method.getAnnotation(ApiOperation::class.java)?.tags?.joinToString(separator = "|")
            logger.warn(
                "validProjectManagerPermission|permission_is_false|" +
                    "$apigwType|$appCode|$userId|$projectId|${tags ?: method}"
            )
            return
//            val defaultMessage =
//                "[user($userId) No access to this project($projectId) because you are not a member of the project"
//            throw ErrorCodeException(
//                statusCode = Response.Status.FORBIDDEN.statusCode,
//                errorCode = PERMISSION_DENIED,
//                defaultMessage = defaultMessage,
//                params = arrayOf(defaultMessage)
//            )
        }

        projectCache.put("${userId}_$projectId", "")
    }

    fun validProjectManagerPermission(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String
    ) {
        if (userId == null) {
            val stack = Throwable().stackTrace.getOrNull(1).toString()
            logger.warn("validProjectManagerPermission|user_is_null|$apigwType|$appCode|$projectId|$stack")
            return
        }

        val hasViewPermission = kotlin.runCatching {
            client.get(ServiceProjectAuthResource::class)
                .checkManager(
                    token = clientTokenService.getSystemToken(null) ?: "",
                    userId = userId,
                    projectId = projectId
                ).data
        }.getOrNull() ?: false

        if (!hasViewPermission) {
            val stack = Throwable().stackTrace.getOrNull(1).toString()
            logger.warn(
                "validProjectManagerPermission|permission_is_false|" +
                    "$apigwType|$appCode|$userId|$projectId|$stack"
            )
            return
//            val defaultMessage = "[user($userId) is not manager to this project($projectId)]"
//            throw ErrorCodeException(
//                statusCode = Response.Status.FORBIDDEN.statusCode,
//                errorCode = PERMISSION_DENIED,
//                defaultMessage = defaultMessage,
//                params = arrayOf(defaultMessage)
//            )
        }
    }
}
