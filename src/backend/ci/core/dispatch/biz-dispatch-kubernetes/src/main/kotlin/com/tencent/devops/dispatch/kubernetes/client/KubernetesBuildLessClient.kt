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

package com.tencent.devops.dispatch.kubernetes.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.buildless.pojo.BuildLessEndInfo
import com.tencent.devops.buildless.pojo.BuildLessStartInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesResult
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.getCodeMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class KubernetesBuildLessClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: KubernetesClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesBuildLessClient::class.java)
    }

    fun startBuildLess(buildLessStartInfo: BuildLessStartInfo): String {
        val url = "/api/buildless/build/start"
        with(buildLessStartInfo) {
            logger.info("[$buildId]|[$vmSeqId] Start buildLess request url: $url, body: $buildLessStartInfo")
            val request = clientCommon.baseRequest("", url)
                .post(
                    JsonUtil.toJson(buildLessStartInfo)
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                )
                .build()
            try {
                OkhttpUtils.doHttp(request).use { response ->
                    val responseContent = response.body!!.string()
                    logger.info("[$buildId]|[$vmSeqId] Start buildLess response: $responseContent")
                    if (response.isSuccessful) {
                        return ""
                    }

                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorType,
                        errorCode = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorCode,
                        formatErrorMessage = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.getErrorMessage(),
                        errorMessage = "Fail to start buildLess, http response code: ${response.code}"
                    )
                }
            } catch (e: SocketTimeoutException) {
                logger.error("[$buildId]|[$vmSeqId] Start buildLess failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_VM_STATUS_INTERFACE_ERROR.getErrorMessage(),
                    errorMessage = "SocketTimeoutException - $url"
                )
            }
        }
    }

    fun endBuildLess(buildLessEndInfo: BuildLessEndInfo): String {
        val url = "/api/buildless/build/end"
        with(buildLessEndInfo) {
            val body = JsonUtil.toJson(buildLessEndInfo)
            val request = clientCommon.baseRequest("", url)
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
            logger.info("[$buildId]|[$vmSeqId] end buildLess, url: $url, body: $body")
            try {
                OkhttpUtils.doHttp(request).use { response ->
                    val responseContent = response.body!!.string()
                    if (!response.isSuccessful) {
                        throw BuildFailureException(
                            errorType = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_ERROR.errorType,
                            errorCode = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_ERROR.errorCode,
                            formatErrorMessage = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_ERROR.getErrorMessage(),
                            errorMessage = "Fail to end buildLess, response code: ${response.code}"
                        )
                    }
                    logger.info("[$buildId]|[$vmSeqId] end buildLess response: $responseContent")
                    val responseData: KubernetesResult<String> = objectMapper.readValue(responseContent)
                    if (responseData.isOk()) {
                        return responseData.data!!
                    } else {
                        val msg = "${responseData.message ?: responseData.getCodeMessage()}"
                        throw BuildFailureException(
                            errorType = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorType,
                            errorCode = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorCode,
                            formatErrorMessage = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.getErrorMessage(),
                            errorMessage = "End buildLess error. $msg"
                        )
                    }
                }
            } catch (e: SocketTimeoutException) {
                logger.error("[$buildId]|[$vmSeqId] End buildLess get SocketTimeoutException.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorType,
                    errorCode = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_OPERATE_VM_INTERFACE_FAIL.getErrorMessage(),
                    errorMessage = "End buildLess timed out - $url"
                )
            }
        }
    }
}
