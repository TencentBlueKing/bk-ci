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

package com.tencent.devops.artifactory.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.api.pojo.JFrogFileDetail
import com.tencent.devops.common.archive.api.pojo.JFrogFileInfo
import com.tencent.devops.common.archive.api.pojo.JFrogFileInfoList
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import javax.ws.rs.NotFoundException

@Service
class JFrogService @Autowired constructor(private val objectMapper: ObjectMapper) {
    @Value("\${jfrog.url:#{null}}")
    private val JFROG_BASE_URL: String? = null
    @Value("\${jfrog.username:#{null}}")
    private val JFROG_USERNAME: String? = null
    @Value("\${jfrog.password:#{null}}")
    private val JFROG_PASSWORD: String? = null

    fun list(path: String, deep: Boolean, depth: Int, includeFolders: Boolean = true): List<JFrogFileInfo> {
        val isDeep = if (deep) 1 else 0
        val listFolders = if (includeFolders) 1 else 0
        val url = "$JFROG_BASE_URL/api/storage/$path?list&deep=$isDeep&depth=$depth&listFolders=$listFolders&mdTimestamps=1&includeRootPath=0"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    logger.info("JFrog $path not found")
                    return emptyList()
                }
                logger.error("Fail to list $path. $responseContent")
                throw RuntimeException("Fail to list artifact")
            }

            val jFrogFileList = objectMapper.readValue<JFrogFileInfoList>(responseContent)
            return jFrogFileList.files
        }
    }

    fun exist(path: String): Boolean {
        try {
            file(path)
            return true
        } catch (e: NotFoundException) {
        }
        return false
    }

    fun file(path: String): JFrogFileDetail {
        val url = "$JFROG_BASE_URL/api/storage/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get jfrog $path. $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("File not found")
                }
                throw RuntimeException("Fail to get artifact")
            }

            return objectMapper.readValue(responseContent)
        }
    }

    fun get(path: String): Pair<ByteArray, MediaType> {
        val url = "$JFROG_BASE_URL/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.bytes()
            val mediaType = response.body()!!.contentType()!!
            if (!response.isSuccessful) {
                logger.error("Fail to get $path. ${String(responseContent)}")
                throw RuntimeException("Fail to get artifact")
            }
            return Pair(responseContent, mediaType)
        }
    }

    fun deploy(path: String, inputStream: InputStream, properties: Map<String, String>? = null) {
        val sb = StringBuilder()
        sb.append("$JFROG_BASE_URL/$path")
        properties?.forEach { key, value ->
            sb.append(";$key=$value")
        }
        val url = sb.toString()
        val mediaType = MediaType.parse("application/octet-stream")
        val requestBody = object : RequestBody() {
            override fun writeTo(sink: BufferedSink?) {
                val source = Okio.source(inputStream)
                sink!!.writeAll(source)
            }

            override fun contentType(): MediaType? {
                return mediaType
            }
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .put(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to deploy $path. $responseContent")
                throw RuntimeException("Fail to deploy artifact")
            }
        }
    }

    fun copy(fromPath: String, toPath: String) {
        val url = "$JFROG_BASE_URL/api/copy/$fromPath?to=$toPath"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, "")
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to copy jfrog from $fromPath to $toPath. $responseContent")
                throw RuntimeException("Fail to copy artifact")
            }
        }
    }

    fun move(fromPath: String, toPath: String) {
        val url = "$JFROG_BASE_URL/api/move/$fromPath?to=$toPath"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, "")
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to move jfrog from $fromPath to $toPath. $responseContent")
                throw RuntimeException("Fail to move artifact")
            }
        }
    }

    fun delete(path: String) {
        val url = "$JFROG_BASE_URL/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to delete jfrog $path. $responseContent")
                throw RuntimeException("Fail to delete artifact")
            }
        }
    }

    fun tryDelete(path: String) {
        val url = "$JFROG_BASE_URL/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful && response.code() != 404) {
                logger.error("Fail to delete jfrog $path. $responseContent")
                throw RuntimeException("Fail to delete artifact")
            }
        }
    }

    fun mkdir(path: String, userId: String? = null) {
        val folder = "${path.removeSuffix("/")}/"
        val url = if (userId == null) "$JFROG_BASE_URL/$folder" else "$JFROG_BASE_URL/$folder;userId=$userId"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, "")
        val request = Request.Builder()
            .url(url)
            .header("Authorization", makeCredential())
            .put(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to make jfrog directory $path. $responseContent")
                throw RuntimeException("Fail to mkdir")
            }
        }
    }

    private fun makeCredential(): String = Credentials.basic(JFROG_USERNAME!!, JFROG_PASSWORD!!)

    companion object {
        private val logger = LoggerFactory.getLogger(JFrogService::class.java)
    }
}