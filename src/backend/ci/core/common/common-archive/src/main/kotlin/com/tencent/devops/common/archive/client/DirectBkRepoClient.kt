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

package com.tencent.devops.common.archive.client

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.io.File
import java.net.URLEncoder
import java.util.Base64

class DirectBkRepoClient {
    @Value("\${bkrepo.url:}")
    private var bkrepoUrl: String = ""

    @Value("\${bkrepo.authorization:}")
    private var bkrepoAuth: String = ""

    /**
     * 上传文件
     */
    fun uploadLocalFile(
        userId: String,
        projectId: String,
        repoName: String,
        path: String,
        file: File,
        metadata: Map<String, String> = mapOf(),
        override: Boolean = true
    ) {
        logger.info("uploadLocalFile, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path, " +
            "file: ${file.canonicalPath}, metadata: $metadata, override: $override")
        buildMetadataHeader(metadata)
        val request = Request.Builder()
            .url("${getBkRepoUrl()}/generic/$projectId/$repoName/${path.removePrefix("/")}")
            .header(AUTHORIZATION, bkrepoAuth)
            .header(BK_REPO_OVERRIDE, override.toString())
            .header(BK_REPO_UID, userId)
            .header(BK_REPO_METADATA, Base64.getEncoder().encodeToString(buildMetadataHeader(metadata).toByteArray()))
            .put(RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                throw RemoteServiceException("upload file failed: ${response.body!!.string()}", response.code)
            }
        }
    }

    /**
     * 上传二进制
     */
    fun uploadByteArray(
        userId: String,
        projectId: String,
        repoName: String,
        path: String,
        byteArray: ByteArray,
        metadata: Map<String, String> = mapOf(),
        override: Boolean = true
    ): String {
        logger.info("uploadByteArray, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path, " +
            "metadata: $metadata, override: $override")
        buildMetadataHeader(metadata)
        val url = "${getBkRepoUrl()}/generic/$projectId/$repoName/${path.removePrefix("/")}"
        val request = Request.Builder()
            .url(url)
            .header(AUTHORIZATION, bkrepoAuth)
            .header(BK_REPO_OVERRIDE, override.toString())
            .header(BK_REPO_UID, userId)
            .header(BK_REPO_METADATA, Base64.getEncoder().encodeToString(buildMetadataHeader(metadata).toByteArray()))
            .put(RequestBody.create("application/octet-stream".toMediaTypeOrNull(), byteArray))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                throw RemoteServiceException("upload file failed: ${response.body!!.string()}", response.code)
            }
            return url
        }
    }

    fun getBkRepoUrl(): String {
        if (bkrepoUrl.isNullOrBlank()) {
            throw IllegalArgumentException("bkrepo.url config is null")
        }
        return if (bkrepoUrl.startsWith("http://") || bkrepoUrl.startsWith("https://")) {
            bkrepoUrl.removeSuffix("/")
        } else {
            "http://${bkrepoUrl.removeSuffix("/")}"
        }
    }

    private fun urlEncode(str: String?): String {
        return if (str.isNullOrBlank()) {
            ""
        } else {
            URLEncoder.encode(str, "UTF-8")
        }
    }

    private fun buildMetadataHeader(metadata: Map<String, String>): String {
        return StringUtils.join(metadata.map { "${urlEncode(it.key)}=${urlEncode(it.value)}" }, "&")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DirectBkRepoClient::class.java)
        private const val BK_REPO_UID = "X-BKREPO-UID"
        private const val BK_REPO_OVERRIDE = "X-BKREPO-OVERWRITE"
        private const val BK_REPO_METADATA = "X-BKREPO-META"
        private const val AUTHORIZATION = "Authorization"
    }
}
