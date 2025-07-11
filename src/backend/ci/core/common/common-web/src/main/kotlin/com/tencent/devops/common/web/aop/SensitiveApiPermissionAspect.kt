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

package com.tencent.devops.common.web.aop

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_OS_ARCH
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_OS_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SHA_CONTENT
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SIGN_FILE_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_STORE_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_STORE_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_STORE_VERSION
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.auth.REFERER
import com.tencent.devops.common.api.auth.SIGN_HEADER_NONCE
import com.tencent.devops.common.api.auth.SIGN_HEADER_TIMESTAMP
import com.tencent.devops.common.api.auth.SING_HEADER_SIGNATURE
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.PROFILE_DEVX
import com.tencent.devops.common.util.ApiSignUtil
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import com.tencent.devops.common.web.service.ServiceSensitiveApiPermissionResource
import com.tencent.devops.common.web.utils.AtomRuntimeUtil
import jakarta.servlet.http.HttpServletRequest
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
        .build<String, Boolean>()

    @Before("pointCut()")
    fun doBefore(jp: JoinPoint) {
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
        val method = (jp.signature as MethodSignature).method
        val apiName = method.getAnnotation(SensitiveApiPermission::class.java)?.value
        val referer = request.getHeader(REFERER)
        var verifyFlag = false
        var storeCode: String? = request.getHeader(AUTH_HEADER_DEVOPS_STORE_CODE)
        if (referer?.contains(PROFILE_DEVX) == true) {
            verifyFlag = doShaValidateBus(request, storeCode, apiName)
        } else {
            val buildId = request.getHeader(AUTH_HEADER_DEVOPS_BUILD_ID)
            val vmSeqId = request.getHeader(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
            val (atomCode, signToken) = if (buildId != null && vmSeqId != null) {
                AtomRuntimeUtil.getRunningAtomValue(
                    redisOperation = redisOperation, buildId = buildId, vmSeqId = vmSeqId
                ) ?: Pair(null, null)
            } else {
                Pair(null, null)
            }
            storeCode = atomCode
            if (apiName != null && storeCode != null) {
                verifyFlag = verifyToken(request, signToken) || verifyApi("ATOM", storeCode, apiName)
            }
        }

        logger.info("$storeCode|$apiName|using sensitive api")
        if (enableSensitiveApi && !verifyFlag) {
            logger.warn("$storeCode|$apiName|verify sensitive api failed")
            throw ErrorCodeException(
                statusCode = 401,
                errorCode = CommonMessageCode.ERROR_SENSITIVE_API_NO_AUTH,
                params = arrayOf(storeCode ?: "", apiName ?: ""),
                defaultMessage = "Unauthorized: sensitive api $apiName cannot be used by $storeCode"
            )
        }
    }

    private fun doShaValidateBus(
        request: HttpServletRequest,
        storeCode: String?,
        apiName: String?
    ): Boolean {
        val storeType = request.getHeader(AUTH_HEADER_DEVOPS_STORE_TYPE)
        val version = request.getHeader(AUTH_HEADER_DEVOPS_STORE_VERSION)
        val checkParamFlag = !storeType.isNullOrBlank() && !version.isNullOrBlank()
        if (checkParamFlag && !apiName.isNullOrBlank() && !storeCode.isNullOrBlank()) {
            val signFileName = request.getHeader(AUTH_HEADER_DEVOPS_SIGN_FILE_NAME)
            val fileShaContent = request.getHeader(AUTH_HEADER_DEVOPS_SHA_CONTENT)
            val osName = request.getHeader(AUTH_HEADER_DEVOPS_OS_NAME)
            val osArch = request.getHeader(AUTH_HEADER_DEVOPS_OS_ARCH)
            return verifyApi(
                storeType = storeType,
                storeCode = storeCode,
                apiName = apiName,
                version = version,
                osName = osName,
                osArch = osArch,
                signFileName = signFileName,
                fileShaContent = fileShaContent
            )
        }
        return false
    }

    @Suppress("LongParameterList")
    private fun verifyApi(
        storeType: String,
        storeCode: String,
        apiName: String,
        version: String? = null,
        osName: String? = null,
        osArch: String? = null,
        signFileName: String? = null,
        fileShaContent: String? = null
    ): Boolean {
        val cacheKey = "$storeType:$storeCode:$apiName:$version:$osName:$osArch:$signFileName:$fileShaContent"
        return apiPermissionCache.getIfPresent(cacheKey) ?: run {
            val apiPermission = client.get(ServiceSensitiveApiPermissionResource::class).verifyApi(
                signFileName = signFileName,
                fileShaContent = fileShaContent,
                osName = osName,
                osArch = osArch,
                storeCode = storeCode,
                apiName = apiName,
                storeType = storeType,
                version = version
            ).data == true
            // 只有验证通过的插件才缓存,没有验证通过的插件状态是动态的
            if (apiPermission) {
                apiPermissionCache.put(cacheKey, true)
            }
            apiPermission
        }
    }

    private fun verifyToken(request: HttpServletRequest, signToken: String?): Boolean {
        val signature = request.getHeader(SING_HEADER_SIGNATURE)
        val timestamp = request.getHeader(SIGN_HEADER_TIMESTAMP)
        val nonce = request.getHeader(SIGN_HEADER_NONCE)
        val buildId = request.getHeader(AUTH_HEADER_DEVOPS_BUILD_ID)
        val vmSeqId = request.getHeader(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        val method = request.method
        val url = if (request.queryString.isNullOrBlank()) {
            request.requestURI
        } else {
            "${request.requestURI}?${request.queryString}"
        }
        // 如果没有签名头,则需要验证插件是否开通了接口权限
        if (isBlank(signature = signature, timestamp = timestamp, nonce = nonce, signToken = signToken)) {
            return false
        }
        val serverSign = ApiSignUtil.signToRequest(
            method = method,
            url = url,
            timestamp = timestamp,
            nonce = nonce,
            token = signToken!!
        )
        logger.info(
            "buildId:$buildId|vmSeqId:$vmSeqId|timestamp:$timestamp|" +
                "nonce:$nonce|signature:$signature|serverSign:$serverSign|verify sensitive api sign"
        )
        return serverSign == signature
    }

    private fun isBlank(signature: String?, timestamp: String?, nonce: String?, signToken: String?): Boolean {
        return signature.isNullOrBlank() ||
            timestamp.isNullOrBlank() ||
            nonce.isNullOrBlank() ||
            signToken.isNullOrBlank()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SensitiveApiPermissionAspect::class.java)
        private const val CACHE_MAX_SIZE = 2000L
    }
}
