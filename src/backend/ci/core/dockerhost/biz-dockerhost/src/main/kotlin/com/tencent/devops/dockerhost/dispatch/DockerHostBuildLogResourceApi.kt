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

package com.tencent.devops.dockerhost.dispatch

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.docker.pojo.FormatLog
import com.tencent.devops.dispatch.docker.pojo.LogType
import com.tencent.devops.dockerhost.config.DockerHostConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class DockerHostBuildLogResourceApi constructor(
    dockerHostConfig: DockerHostConfig
) : AbstractBuildResourceApi(dockerHostConfig) {
    private val logger = LoggerFactory.getLogger(DockerHostBuildLogResourceApi::class.java)

    fun sendFormatLog(logMap: Map<String, String>) {
        try {
            val path = "/${getUrlPrefix()}/api/dockerhost/log/sendFormatLog"

            val formatLog = FormatLog(
                logType = LogType.DOCKERHOST_CONTAINER_LOAD,
                washTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                logMessageMap = logMap
            )
            val request = buildPost(path = path,
                requestBody = RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                    JsonUtil.toJson(formatLog)
                )
            )

            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("DockerHostBuildLogResourceApi $path fail. $responseContent")
                }
            }
        } catch (e: Exception) {
            logger.error("DockerHostBuildLogResourceApi sendFormatLog error.", e)
        }
    }
}
