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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.security.manager

import com.tencent.bkrepo.auth.api.ServiceAccountResource
import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.HttpAuthProperties
import org.slf4j.LoggerFactory

/**
 * 认证管理器
 */
class AuthenticationManager(
    private val serviceUserResource: ServiceUserResource,
    private val serviceAccountResource: ServiceAccountResource,
    private val httpAuthProperties: HttpAuthProperties
) {

    fun checkUserAccount(uid: String, token: String): String {
        if (preCheck()) return uid
        val response = serviceUserResource.checkUserToken(uid, token)
        return if (response.data == true) uid else throw AuthenticationException("Authorization value check failed")
    }

    fun checkPlatformAccount(accessKey: String, secretKey: String): String {
        val response = serviceAccountResource.checkCredential(accessKey, secretKey)
        return response.data ?: throw AuthenticationException("AccessKey/SecretKey check failed.")
    }

    private fun preCheck(): Boolean {
        if (!httpAuthProperties.enabled) {
            logger.debug("Auth disabled, skip authenticate.")
            return true
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthenticationManager::class.java)
    }
}
