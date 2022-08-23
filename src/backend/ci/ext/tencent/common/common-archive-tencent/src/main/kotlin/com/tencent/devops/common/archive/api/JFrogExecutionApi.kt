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

package com.tencent.devops.common.archive.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.archive.api.pojo.JFrogApiResponse
import com.tencent.devops.common.archive.api.pojo.JFrogFolderCount
import com.tencent.devops.common.archive.api.pojo.JFrogFolderCountRequest
import com.tencent.devops.common.archive.api.pojo.Url
import com.tencent.devops.common.archive.util.JFrogUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class JFrogExecutionApi @Autowired constructor(
    jFrogConfigProperties: JFrogConfigProperties,
    private val objectMapper: ObjectMapper
) {
//    private val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(30L, TimeUnit.SECONDS)
//            .writeTimeout(30L, TimeUnit.SECONDS)
//            .build()
    private val baseUrl = jFrogConfigProperties.url!!
    private val credential = JFrogUtil.makeCredential(jFrogConfigProperties.username!!, jFrogConfigProperties.password!!)

    fun downloadUrl(path: String): String {
        val url = "$baseUrl/api/plugins/execute/downloadUrl?params=path=$path"
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .get()
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create jfrog $path downloadUrl. $responseContent")
                throw RuntimeException("Fail to create downloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Url>>(responseContent)
            return jFrogApiResponse.data!!.url
        }
    }

    fun internalDownloadUrl(path: String, ttl: Int, downloadUsers: String): String {
        val url = "$baseUrl/api/plugins/execute/internalDownloadUrl?params=path=$path;ttl=$ttl;downloadUsers=$downloadUsers"
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .get()
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create jfrog $path internalDownloadUrl. $responseContent")
                throw RuntimeException("Fail to create internalDownloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Url>>(responseContent)
            return jFrogApiResponse.data!!.url
        }
    }

    fun externalDownloadUrl(path: String, userId: String, ttl: Int, directed: Boolean = false): String {
        val url = "$baseUrl/api/plugins/execute/externalDownloadUrl?params=path=$path;downloadUser=$userId;ttl=$ttl;directed=$directed"
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .get()
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create jfrog $path externalDownloadUrl. $responseContent")
                throw RuntimeException("Fail to create externalDownloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Url>>(responseContent)
            return jFrogApiResponse.data!!.url
        }
    }

    fun batchExternalDownloadUrl(path: String, userIds: Set<String>, ttl: Int, directed: Boolean = false): Map<String, String> {
        if (userIds.isEmpty()) return emptyMap()
        val userIdString = userIds.joinToString(",")
        val url = "$baseUrl/api/plugins/execute/batchExternalDownloadUrl?params=path=$path;downloadUser=$userIdString;ttl=$ttl;directed=$directed"
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .get()
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to batch create jfrog $path externalDownloadUrl. $responseContent")
                throw RuntimeException("Fail to batch create externalDownloadUrl")
            }

            val jFrogApiResponse = objectMapper.readValue<JFrogApiResponse<Map<String, String>>>(responseContent)
            return jFrogApiResponse.data!!
        }
    }

    fun folderCount(path: String): Long {
        val name = path.removeSuffix("/").split("/").last()
        val jFrogFolderCountRequest = JFrogFolderCountRequest(name, path)
        val requestContent = objectMapper.writeValueAsString(jFrogFolderCountRequest)

        val url = "$baseUrl/api/artifactgeneral/artifactsCount?\$no_spinner=true"
        val mediaType = MediaType.parse("application/json")
        val requestBody = RequestBody.create(mediaType, requestContent)
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .post(requestBody)
                .build()
//
//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to count folder $path. $responseContent")
                throw RuntimeException("Fail to count folder")
            }

            val jFrogFolderCount = objectMapper.readValue<JFrogFolderCount>(responseContent)
            val countList = jFrogFolderCount.artifactSize.split(" ")
            if (countList.size != 2) {
                logger.error("folder count artifactSize ${jFrogFolderCount.artifactSize} invalid")
                throw RuntimeException("Fail to count folder")
            }

            val size = countList[0].toFloat()
            val unit = when (countList[1]) {
                "bytes" -> 1L
                "KB" -> 1024L
                "MB" -> 1024*1024L
                "GB" -> 1024*1024*1024L
                "TB" -> 1024*1024*1024*1024L
                else -> throw RuntimeException("folder count unit ${countList[1]} invalid")
            }
            return (size * unit).toLong()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JFrogExecutionApi::class.java)
    }
}
