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

package com.tencent.devops.common.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.EsbCreateApiReq
import com.tencent.devops.common.auth.api.pojo.EsbPermissionUrlReq
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

@Suppress("ALL")
class IamEsbService {

    @Value("\${esb.code:#{null}}")
    val appCode: String? = null
    @Value("\${esb.secret:#{null}}")
    val appSecret: String? = null
    @Value("\${bk.paas.host:#{null}}")
    val iamHost: String? = null

    fun createRelationResource(iamCreateApiReq: EsbCreateApiReq): Boolean {
        var url = "api/c/compapi/v2/iam/authorization/resource_creator_action/"
        url = getAuthRequestUrl(url)
        iamCreateApiReq.bk_app_code = appCode!!
        iamCreateApiReq.bk_app_secret = appSecret!!
        val content = objectMapper.writeValueAsString(iamCreateApiReq)
        logger.info("v3 createRelationResource url[$url]")
        logger.info("v3 createRelationResource body[$content]")
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder().url(url)
                .post(requestBody)
                .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("bkiam v3 request failed url:$url response $it")
                // 请求错误
                throw RemoteServiceException("bkiam v3 request failed, response: ($it)")
            }
            val responseStr = it.body!!.string()
            logger.info("v3 createRelationResource responseStr[$responseStr]")
            val iamApiRes = objectMapper.readValue<Map<String, Any>>(responseStr)
            if (iamApiRes["code"] != 0 || iamApiRes["result"] == false) {
                // 请求错误
                throw RemoteServiceException(
                    "bkiam v3 failed|message[${iamApiRes["message"]}], request_id[${iamApiRes["request_id"]}])"
                )
            }
            return true
        }
    }

    fun getPermissionUrl(iamPermissionUrl: EsbPermissionUrlReq): String? {
        var url = "api/c/compapi/v2/iam/application/"
        url = getAuthRequestUrl(url)
        iamPermissionUrl.bk_app_code = appCode!!
        iamPermissionUrl.bk_app_secret = appSecret!!
        val content = objectMapper.writeValueAsString(iamPermissionUrl)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, content)
        logger.info("getPermissionUrl url:$url")
        logger.info("getPermissionUrl content:$content body:$requestBody")
        val request = Request.Builder().url(url)
                .post(requestBody)
                .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                // 请求错误
                throw RemoteServiceException("bkiam v3 request failed, response: ($it)")
            }
            val responseStr = it.body!!.string()
            logger.info("v3 getPermissionUrl responseStr[$responseStr]")
            val iamApiRes = objectMapper.readValue<Map<String, Any>>(responseStr)
            if (iamApiRes["code"] != 0 || iamApiRes["result"] == false) {
                // 请求错误
                throw RemoteServiceException(
                    "bkiam v3 failed|message[${iamApiRes["message"]}], request_id[${iamApiRes["request_id"]}])"
                )
            }
            return iamApiRes["data"].toString().substringAfter("url=").substringBeforeLast("}")
        }
    }

    /**
	 * 生成请求url
	 */
    private fun getAuthRequestUrl(uri: String): String {
        val newUrl = if (iamHost?.endsWith("/")!!) {
            iamHost + uri
        } else {
            "$iamHost/$uri"
        }
        return newUrl.replace("//", "/")
    }

    companion object {
        val logger = LoggerFactory.getLogger(IamEsbService::class.java)
        val objectMapper = ObjectMapper()
    }
}
