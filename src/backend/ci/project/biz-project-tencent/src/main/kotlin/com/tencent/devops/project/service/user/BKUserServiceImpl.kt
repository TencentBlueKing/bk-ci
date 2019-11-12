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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.user.UserVO
import com.tencent.devops.project.service.UserService
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.collections.set

/**
 * 蓝鲸用户管理
 */
@Service
class BKUserServiceImpl constructor(
    private val objectMapper: ObjectMapper
) : UserService {

    private val logger = LoggerFactory.getLogger(BKUserServiceImpl::class.java)

    @Value("\${bk_login.path}")
    lateinit var path: String

    @Value("\${bk_login.getUser}")
    lateinit var getUser: String

    @Value("\${bk_login.bk_app_code}")
    lateinit var bkAppCode: String

    @Value("\${bk_login.bk_app_secret}")
    lateinit var bkAppSecret: String

    override fun getStaffInfo(userId: String, bkToken: String?): UserVO {
        val url = (path + getUser)
        val map = HashMap<String, String>()
        map["bk_app_code"] = bkAppCode
        map["bk_app_secret"] = bkAppSecret
        map["bk_username"] = userId

        val mediaType = MediaType.parse("application/json")
        val json = objectMapper.writeValueAsString(map)
        logger.info("Get the user from url $url with body $json")
        val requestBody = RequestBody.create(mediaType, json)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("CommitResourceApi $path$getUser fail.")
                throw RemoteServiceException("CommitResourceApi $path$getUser fail")
            }
            logger.info("Get the user response - $responseContent")
            val resultMap = objectMapper.readValue(responseContent, Result(LinkedHashMap<String, String>())::class.java)
            val data = resultMap.data
            val userVO =
                UserVO(chineseName = "", avatarUrl = "", bkpaasUserId = userId, username = userId, permissions = "")
            if (data != null) {
                userVO.chineseName = data["chname"] ?: ""
            }
            return userVO
        }
    }
}