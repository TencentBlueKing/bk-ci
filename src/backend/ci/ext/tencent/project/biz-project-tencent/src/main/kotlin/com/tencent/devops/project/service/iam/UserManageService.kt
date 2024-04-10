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

package com.tencent.devops.project.service.iam

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.TreeMap

@Service
class UserManageService @Autowired constructor(
    val objectMapper: ObjectMapper
) {
    @Value("\${esb.code:#{null}}")
    val appCode: String? = null

    @Value("\${esb.secret:#{null}}")
    val appSecret: String? = null

    @Value("\${user.manage.url:#{null}}")
    val userManageUrl: String? = null
    fun getDepartment(departId: String): String {
        val header = TreeMap<String, String?>()
        header["bk_app_secret"] = appSecret
        header["bk_app_code"] = appCode
        val headerStr = objectMapper.writeValueAsString(header).replace("\n", "")
        val url = String.format(userManageUrl!!, departId)
        logger.info("getDepartment: url = $url")
        logger.info("header :$headerStr")
        val request = Request.Builder().url(url)
            .addHeader("x-bkapi-authorization", headerStr)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("getDepartment request failed url:$url response $it")
                // 请求错误
                throw RemoteServiceException("getDepartment request failed, response: ($it)")
            }
            val responseStr = it.body!!.string()
            logger.info("getDepartment request responseStr[$responseStr]")
            val response = objectMapper.readValue<Map<String, Any>>(responseStr)
            if (response["code"] != 0 || response["result"] == false) {
                // 请求错误
                throw RemoteServiceException(
                    "request getDepartment failed|message[${response["message"]}]"
                )
            }
            val responseData = response["data"] as HashMap<String, Any>
            return responseData.get("name").toString()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserManageService::class.java)
    }
}
