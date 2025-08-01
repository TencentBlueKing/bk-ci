/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.image.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.image.api.ServiceDockerImageResource
import com.tencent.devops.image.pojo.CheckDockerImageRequest
import com.tencent.devops.image.pojo.CheckDockerImageResponse
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class InspectImageService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(InspectImageService::class.java)
    }

    @Value("\${image.checkImageUrl:}")
    var checkImageUrl: String? = null

    fun checkDockerImage(
        userId: String,
        checkDockerImageRequestList: List<CheckDockerImageRequest>
    ): List<CheckDockerImageResponse> {
        logger.info("checkImage userId: $userId, checkDockerImageRequestList: $checkDockerImageRequestList")

        if (!checkImageUrl.isNullOrBlank()) {
            return checkRemoteDockerImage(userId, checkDockerImageRequestList)
        } else {
            return checkKubernetesDockerImage(userId, checkDockerImageRequestList)
        }
    }

    fun checkRemoteDockerImage(
        userId: String,
        checkDockerImageRequestList: List<CheckDockerImageRequest>
    ): List<CheckDockerImageResponse> {
        try {
            val url = "$checkImageUrl/api/service/docker-image/checkDockerImage"
            val request = Request
                .Builder()
                .url(url)
                .headers(mutableMapOf(AUTH_HEADER_USER_ID to userId).toHeaders())
                .post(
                    JsonUtil.toJson(checkDockerImageRequestList)
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                )
                .build()

            OkhttpUtils.doLongHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info("$userId check remoteImage: $responseContent")
                if (!response.isSuccessful) {
                    logger.error("Check remoteImage fail. $responseContent")
                    throw RuntimeException("Check remoteImage fail. $responseContent")
                }

                val responseMap: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
                return JsonUtil.to(JsonUtil.toJson(responseMap["data"] ?: ""),
                    object : TypeReference<List<CheckDockerImageResponse>>() {})
            }
        } catch (e: Exception) {
            logger.error("Check remoteImage error: ${e.message}")
            throw RuntimeException("Check remoteImage error: ${e.message}")
        }
    }

    fun checkKubernetesDockerImage(
        userId: String,
        checkDockerImageRequestList: List<CheckDockerImageRequest>
    ): List<CheckDockerImageResponse> {
        try {
            val dispatchCheckDockerImageRequestList =
                checkDockerImageRequestList.stream().map { checkDockerImageRequest ->
                    CheckDockerImageRequest(
                        imageName = checkDockerImageRequest.imageName,
                        registryHost = checkDockerImageRequest.registryHost,
                        registryUser = checkDockerImageRequest.registryUser,
                        registryPwd = checkDockerImageRequest.registryPwd
                    )
                }.collect(Collectors.toList())

            val response = client.getWithoutRetry(ServiceDockerImageResource::class).checkDockerImage(
                userId = userId,
                checkDockerImageRequestList = dispatchCheckDockerImageRequestList
            ).data

            return response?.stream()?.map {
                CheckDockerImageResponse(
                errorCode = it.errorCode,
                errorMessage = it.errorMessage,
                arch = it.arch,
                author = it.author,
                comment = it.comment,
                created = it.created,
                dockerVersion = it.dockerVersion,
                id = it.id,
                os = it.os,
                osVersion = it.osVersion,
                parent = it.parent,
                size = it.size,
                repoTags = it.repoTags,
                repoDigests = it.repoDigests,
                virtualSize = it.virtualSize
            ) }?.collect(Collectors.toList()) ?: emptyList()
        } catch (e: Exception) {
            logger.error("Check dispatch image error: ${e.message}")
            return emptyList()
        }
    }
}
