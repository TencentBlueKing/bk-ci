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

package com.tencent.devops.scm.services

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.HTTP_200
import com.tencent.devops.common.api.constant.HTTP_400
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.constant.HTTP_405
import com.tencent.devops.common.api.constant.HTTP_422
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.pojo.enums.GatewayType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.monitoring.api.service.StatusReportResource
import com.tencent.devops.monitoring.pojo.AddCommitCheckStatus
import com.tencent.devops.scm.constant.ScmMessageCode.ERROR_GIT_BAD_REQUEST
import com.tencent.devops.scm.constant.ScmMessageCode.ERROR_GIT_FORBIDDEN
import com.tencent.devops.scm.constant.ScmMessageCode.ERROR_GIT_METHOD_NOT_ALLOWED
import com.tencent.devops.scm.constant.ScmMessageCode.ERROR_GIT_NOT_FOUND
import com.tencent.devops.scm.constant.ScmMessageCode.ERROR_GIT_SERVER_ERROR
import com.tencent.devops.scm.constant.ScmMessageCode.ERROR_GIT_UNAUTHORIZED
import com.tencent.devops.scm.constant.ScmMessageCode.ERROR_GIT_UNPROCESSABLE
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ScmMonitorService @Autowired constructor(
    private val client: Client,
    private val bkTag: BkTag
) {
    companion object {
        private val executorService = Executors.newFixedThreadPool(5)
        private val logger = LoggerFactory.getLogger(ScmMonitorService::class.java)
    }

    fun reportCommitCheck(
        requestTime: Long,
        responseTime: Long,
        statusCode: Int,
        statusMessage: String?,
        projectName: String,
        commitId: String,
        block: Boolean,
        targetUrl: String
    ) {
        execute {
            try {
                val (errorType, errorCode) = getErrorCode(statusCode)
                client.getGateway(StatusReportResource::class, GatewayType.DEVNET_PROXY)
                    .scmCommitCheck(
                        AddCommitCheckStatus(
                            requestTime = requestTime,
                            responseTime = responseTime,
                            elapseTime = responseTime - requestTime,
                            statusCode = statusCode.toString(),
                            statusMessage = statusMessage,
                            errorType = errorType,
                            errorCode = errorCode,
                            errorMsg = MessageCodeUtil.getCodeMessage(messageCode = errorCode, params = null)
                                ?: statusMessage,
                            projectName = projectName,
                            commitId = commitId,
                            block = block,
                            targetUrl = targetUrl,
                            channel = getChannelCode().name
                        )
                    )
            } catch (e: Throwable) {
                logger.error(
                    "report git commit check error, " +
                            "requestTime:$requestTime, " +
                            "responseTime:$responseTime, " +
                            "statusCode:$statusCode, " +
                            "statusMessage:$statusMessage, " +
                            "projectName:$projectName, " +
                            "commitId:$commitId, " +
                            "block:$block, " +
                            "targetUrl:$targetUrl",
                    e
                )
            }
        }
    }

    private fun getErrorCode(statusCode: Int): Pair<String? /*errorType*/, String /*errorCode*/> {
        return when (statusCode) {
            HTTP_200 -> Pair(null, CommonMessageCode.SUCCESS)
            HTTP_400 -> Pair(ErrorType.USER.name, ERROR_GIT_BAD_REQUEST)
            HTTP_401 -> Pair(ErrorType.USER.name, ERROR_GIT_UNAUTHORIZED)
            HTTP_403 -> Pair(ErrorType.USER.name, ERROR_GIT_FORBIDDEN)
            HTTP_404 -> Pair(ErrorType.USER.name, ERROR_GIT_NOT_FOUND)
            HTTP_405 -> Pair(ErrorType.USER.name, ERROR_GIT_METHOD_NOT_ALLOWED)
            HTTP_422 -> Pair(ErrorType.USER.name, ERROR_GIT_UNPROCESSABLE)
            else -> Pair(ErrorType.SYSTEM.name, ERROR_GIT_SERVER_ERROR)
        }
    }

    private fun getChannelCode(): ChannelCode {
        val consulTag = bkTag.getLocalTag()
        return when {
            consulTag.contains("stream") -> {
                ChannelCode.GIT
            }
            consulTag.contains("auto") -> {
                ChannelCode.GONGFENGSCAN
            }
            else -> {
                ChannelCode.BS
            }
        }
    }

    private fun execute(action: () -> Unit) {
        try {
            executorService.execute {
                action()
            }
        } catch (e: Throwable) {
            logger.error("report scm monitor error", e)
        }
    }
}
