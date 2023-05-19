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

package com.tencent.devops.dispatch.kubernetes.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.BK_KUBERNETES_TASK_EXECUTE_TIMEOUT
import com.tencent.devops.dispatch.kubernetes.pojo.BK_KUBERNETES_TASK_STATUS_API_EXCEPTION
import com.tencent.devops.dispatch.kubernetes.pojo.BK_KUBERNETES_TASK_STATUS_API_TIMEOUT
import com.tencent.devops.dispatch.kubernetes.pojo.KubernetesResult
import com.tencent.devops.dispatch.kubernetes.pojo.TaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.TaskStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.getCodeMessage
import com.tencent.devops.dispatch.kubernetes.pojo.isFailed
import com.tencent.devops.dispatch.kubernetes.pojo.isRunning
import com.tencent.devops.dispatch.kubernetes.pojo.isSuccess
import java.net.SocketTimeoutException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KubernetesTaskClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val clientCommon: KubernetesClientCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesBuilderClient::class.java)
    }

    fun getTasksStatus(
        userId: String,
        taskId: String,
        retryFlag: Int = 3
    ): KubernetesResult<TaskStatusResp> {
        val url = "/api/tasks/$taskId/status"
        val request = clientCommon.baseRequest(userId, url).get().build()
        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (response.isSuccessful) {
                    logger.info("Get task: $taskId status response: ${JsonUtil.toJson(responseContent)}")
                    return objectMapper.readValue(responseContent)
                }

                logger.error("Get task: $taskId status failed, responseCode: ${response.code}")
                throw BuildFailureException(
                    ErrorCodeEnum.BCS_TASK_STATUS_INTERFACE_ERROR.errorType,
                    ErrorCodeEnum.BCS_TASK_STATUS_INTERFACE_ERROR.errorCode,
                    ErrorCodeEnum.BCS_TASK_STATUS_INTERFACE_ERROR.getErrorMessage(),
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_KUBERNETES_TASK_STATUS_API_EXCEPTION,
                        params = arrayOf(taskId)
                    ) + "：http response code: ${response.code}"
                )
            }
        } catch (e: SocketTimeoutException) {
            // 接口超时失败，重试三次
            if (retryFlag > 0) {
                logger.info("$taskId get task SocketTimeoutException. retry: $retryFlag")
                return getTasksStatus(userId, taskId, retryFlag - 1)
            } else {
                logger.error("$taskId get task status failed.", e)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.BCS_TASK_STATUS_INTERFACE_ERROR.errorType,
                    errorCode = ErrorCodeEnum.BCS_TASK_STATUS_INTERFACE_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.BCS_TASK_STATUS_INTERFACE_ERROR.getErrorMessage(),
                    errorMessage = "${I18nUtil.getCodeLanMessage(BK_KUBERNETES_TASK_STATUS_API_TIMEOUT)}, url: $url"
                )
            }
        }
    }

    fun waitTaskFinish(userId: String, taskId: String): Pair<TaskStatusEnum, String?> {
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 10 * 60 * 1000) {
                logger.error("$taskId kubernetes task timeout")
                return Pair(
                    TaskStatusEnum.TIME_OUT,
                    "${I18nUtil.getCodeLanMessage(BK_KUBERNETES_TASK_EXECUTE_TIMEOUT)}（10min）"
                )
            }
            Thread.sleep(1 * 1000)
            val (status, errorMsg) = getTaskResult(userId, taskId).apply {
                if (first == null) {
                    return Pair(TaskStatusEnum.FAILED, second)
                }
            }
            return when {
                status!!.isRunning() -> continue@loop
                status.isSuccess() -> {
                    Pair(TaskStatusEnum.SUCCEEDED, null)
                }
                else -> Pair(status, errorMsg)
            }
        }
    }

    private fun getTaskResult(userId: String, taskId: String): Pair<TaskStatusEnum?, String?> {
        val taskResponse = getTasksStatus(userId, taskId)
        val status = TaskStatusEnum.realNameOf(taskResponse.data?.status)
        if (taskResponse.isNotOk() || taskResponse.data == null) {
            // 创建失败
            val msg = "${taskResponse.message ?: taskResponse.getCodeMessage()}"
            logger.error("Execute task: $taskId failed, actionCode is ${taskResponse.status}, msg: $msg")
            return Pair(status, msg)
        }
        // 请求成功但是任务失败
        if (status != null && status.isFailed()) {
            return Pair(status, taskResponse.data.detail)
        }

        return Pair(status, null)
    }
}
