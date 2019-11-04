/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.common.archive.shorturl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.shorturl.pojo.ShortUrlRequest
import com.tencent.devops.common.archive.shorturl.pojo.ShortUrlResponse
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ShortUrlApi @Autowired constructor(private val objectMapper: ObjectMapper) {

    fun getShortUrl(url: String, ttl: Int): String {
        val timestamp = LocalDateTime.now().plusSeconds(ttl.toLong()).timestamp()
        val shortUrlRequest = ShortUrlRequest(1, APPID, PASSWORD, url, 1, timestamp)
        val requestContent = objectMapper.writeValueAsString(shortUrlRequest)

        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, requestContent)
        val request = Request.Builder()
                .url(URL)
                .post(requestBody)
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get short url. $responseContent")
                throw RuntimeException("Fail to get short url")
            }

            val shortUrlResponse = objectMapper.readValue<ShortUrlResponse>(responseContent)
            return shortUrlResponse.shortUrl
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ShortUrlApi::class.java)
        private const val URL = "http://makeshorturl.wsd.com"
        private const val APPID = 71
        private const val PASSWORD = "3IEZwzKABD6vZEWw"
    }
}