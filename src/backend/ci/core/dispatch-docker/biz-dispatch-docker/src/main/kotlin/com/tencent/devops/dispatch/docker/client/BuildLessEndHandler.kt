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

package com.tencent.devops.dispatch.docker.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.buildless.pojo.BuildLessEndInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.docker.client.context.BuildLessEndHandlerContext
import com.tencent.devops.dispatch.docker.common.Constants
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.dispatch.docker.service.DockerHostProxyService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BuildLessEndHandler @Autowired constructor(
    private val dockerHostProxyService: DockerHostProxyService
) : Handler<BuildLessEndHandlerContext>() {
    private val logger = LoggerFactory.getLogger(BuildLessEndHandler::class.java)

    override fun handlerRequest(handlerContext: BuildLessEndHandlerContext) {
        with(handlerContext) {
            val buildLessEndInfo = BuildLessEndInfo(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId?.toInt() ?: 0,
                poolNo = 0,
                containerId = containerId
            )

            val request = dockerHostProxyService.getDockerHostProxyRequest(
                dockerHostUri = Constants.BUILD_LESS_END_URI,
                dockerHostIp = buildLessHost,
                clusterType = DockerHostClusterType.BUILD_LESS
            ).delete(
                JsonUtil.toJson(buildLessEndInfo)
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            ).build()

            OkhttpUtils.doHttp(request).use { resp ->
                val responseBody = resp.body!!.string()
                logger.info("$buildLogKey End buildLess, response: $responseBody")
                val response: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
                if (response["status"] == 0) {
                    response["data"] as Boolean
                } else {
                    val msg = response["message"] as String
                    logger.error("$buildLogKey End buildLess failed, msg: $msg")
                    throw DockerServiceException(
                        errorType = ErrorCodeEnum.END_VM_ERROR.errorType,
                        errorCode = ErrorCodeEnum.END_VM_ERROR.errorCode,
                        errorMsg = "End build less failed, msg: $msg"
                    )
                }
            }
        }
    }
}
