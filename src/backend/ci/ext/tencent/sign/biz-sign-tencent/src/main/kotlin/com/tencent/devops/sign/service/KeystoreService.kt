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

package com.tencent.devops.sign.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.sign.api.constant.SignMessageCode.KEYSTORE_RESOURCE_NOT_EXISTS
import com.tencent.devops.sign.pojo.IosProfile
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KeystoreService {

    @Value("\${keystore.url:#{null}}")
    val keyStoreUrl: String? = null

    fun getHost(): String {
        return HomeHostUtil.getHost(keyStoreUrl!!)
    }

    fun getInHouseCertList(appId: String): Result<List<IosProfile?>> {
        logger.info("getInHouseCertList from KeyStore with appId:$appId")
        OkhttpUtils.doGet(getKeystoreUrl(appId)).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("[${getHost()}|$appId] Fail to get ios Certs from keystore with response [${response.code}|${response.message}|$responseContent]")
                throw OperationException("[${getHost()}|$appId]| Fail to get ios Certs from keystore")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["code"] as Int
            if (0 != code) {
                val message = responseData["msg"] as String
                logger.warn("[${getHost()}|$appId]|getInHouseCertList|return error [${response.code}|$message|$responseContent]")
                throw ErrorCodeException(
                    errorCode = KEYSTORE_RESOURCE_NOT_EXISTS,
                    defaultMessage = message,
                    params = arrayOf(appId)
                )
            }
            return Result(responseData["data"] as List<IosProfile>)
        }
    }

    private fun getKeystoreUrl(appId: String): String =
        "${getHost()}/api/auth/getInHouseCertList?appId=$appId"

    companion object {
        private val logger = LoggerFactory.getLogger(KeystoreService::class.java)
    }
}
