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

package com.tencent.devops.common.web.aop

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.SensitiveApiUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import com.tencent.devops.common.web.service.ServiceSensitiveApiPermissionResource
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
class SensitiveApiPermissionAspect constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val enableSensitiveApi: Boolean
) {

    @Pointcut("@annotation(com.tencent.devops.common.web.annotation.SensitiveApiPermission)")
    fun pointCut() = Unit

    private val apiPermissionCache = Caffeine.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .build<String/*atomCode:apiName*/, Boolean>()

    @Before("pointCut()")
    fun doBefore(jp: JoinPoint) {
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
        val buildId = request.getHeader(AUTH_HEADER_DEVOPS_BUILD_ID)
        val vmSeqId = request.getHeader(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        val method = (jp.signature as MethodSignature).method
        val apiName = method.getAnnotation(SensitiveApiPermission::class.java)?.value

        var atomCode: String? = null
        if (buildId != null && vmSeqId != null) {
            val redisKey = SensitiveApiUtil.getRunningAtomCodeKey(buildId, vmSeqId)
            atomCode = redisOperation.get(redisKey)
        }

        if (apiName != null && atomCode != null) {
            logger.info("$buildId|$vmSeqId|$atomCode|$apiName|using sensitive api")
            if (enableSensitiveApi && !verifyApi(atomCode, apiName)) {
                logger.warn("$buildId|$vmSeqId|$atomCode|$apiName|verify sensitive api failed")
                throw ErrorCodeException(
                    statusCode = 401,
                    errorCode = CommonMessageCode.ERROR_SENSITIVE_API_NO_AUTH,
                    params = arrayOf(apiName, atomCode),
                    defaultMessage = "Unauthorized: sensitive api $apiName cannot be used by $atomCode"
                )
            }
        }
    }

    private fun verifyApi(atomCode: String, apiName: String): Boolean {
        val cacheKey = "$atomCode:$apiName"
        return apiPermissionCache.getIfPresent(cacheKey) ?: run {
            val apiPermission = client.get(ServiceSensitiveApiPermissionResource::class).verifyApi(
                atomCode = atomCode,
                apiName = apiName
            ).data == true
            // 只有验证通过的插件才缓存,没有验证通过的插件状态是动态的
            if (apiPermission) {
                apiPermissionCache.put(cacheKey, true)
            }
            apiPermission
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SensitiveApiPermissionAspect::class.java)
        private const val CACHE_MAX_SIZE = 1000L
    }
}
