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

package com.tencent.devops.repository.service.tgit

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.config.GitConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class TGitService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val gitConfig: GitConfig
) : ITGitService {

    override fun getToken(userId: String, code: String): GitToken {
        logger.info("Start to get the token of user $userId by code $code")
        val startEpoch = System.currentTimeMillis()
        try {
            val tokenUrl =
                "${gitConfig.tGitUrl}/oauth/token?client_id=${gitConfig.tGitClientId}" +
                    "&client_secret=${gitConfig.tGitClientSecret}&code=$code" +
                    "&grant_type=authorization_code&redirect_uri=${gitConfig.tGitWebhookUrl}"
            logger.info("getToken url>> $tokenUrl")
            val request = Request.Builder()
                .url(tokenUrl)
                .post(RequestBody.create("application/x-www-form-urlencoded;charset=utf-8".toMediaTypeOrNull(), ""))
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                return objectMapper.readValue(data, GitToken::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the token")
        }
    }

    @BkTimed(extraTags = ["operation", "获取项目中成员信息"], value = "bk_tgit_api_time")
    override fun getUserInfoByToken(token: String, tokenType: TokenTypeEnum): GitUserInfo {
        logger.info("Start to get the user info by token[$token]")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = StringBuilder("${gitConfig.tGitUrl}/user")
            setToken(tokenType, url, token)
            logger.info("getToken url>> $url")
            val request = Request.Builder()
                .url(url.toString())
                .get()
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                return objectMapper.readValue(data, GitUserInfo::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the token")
        }
    }

    @BkTimed(extraTags = ["operation", "刷新token"], value = "bk_tgit_api_time")
    override fun refreshToken(userId: String, accessToken: GitToken): GitToken {
        logger.info("Start to refresh the token of user $userId")
        val startEpoch = System.currentTimeMillis()
        try {
            val url = "${gitConfig.tGitUrl}/oauth/token" +
                "?client_id=${gitConfig.tGitClientId}" +
                "&client_secret=${gitConfig.tGitClientSecret}" +
                "&grant_type=refresh_token" +
                "&refresh_token=${accessToken.refreshToken}" +
                "&redirect_uri=${gitConfig.tGitWebhookUrl}"
            val request = Request.Builder()
                .url(url)
                .post(RequestBody.create("application/x-www-form-urlencoded;charset=utf-8".toMediaTypeOrNull(), ""))
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body!!.string()
                return objectMapper.readValue(data, GitToken::class.java)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to refresh the token")
        }
    }

    private fun setToken(tokenType: TokenTypeEnum, url: StringBuilder, token: String) {
        if (TokenTypeEnum.OAUTH == tokenType) {
            url.append("?access_token=$token")
        } else {
            url.append("?private_token=$token")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TGitService::class.java)
        private const val PAGE_SIZE = 100
        private const val SLEEP_MILLS_FOR_RETRY_500: Long = 500
    }
}
