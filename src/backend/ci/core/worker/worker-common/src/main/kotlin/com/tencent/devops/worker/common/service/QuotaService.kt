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

package com.tencent.devops.worker.common.service

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.quota.QuotaApi
import com.tencent.devops.common.util.HttpRetryUtils
import org.slf4j.LoggerFactory

object QuotaService {

    private val logger = LoggerFactory.getLogger(QuotaService::class.java)

    private val quotaApi = ApiFactory.create(QuotaApi::class)

    /**
     * agent启动成功时上报agent运行
     */
    fun addRunningAgent(buildVariables: BuildVariables) {
        val pipelineRetryCount = buildVariables.variables[PIPELINE_RETRY_COUNT] ?: "0"
        var retryCount = 0
        val result = HttpRetryUtils.retry {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|addRunningAgent")
            }
            quotaApi.addRunningAgent(
                projectId = buildVariables.projectId,
                buildId = buildVariables.buildId,
                vmSeqId = buildVariables.vmSeqId,
                executeCount = pipelineRetryCount.toInt() + 1,
                retryCount = retryCount++
            )
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Remove running agent failed.")
        }
    }

    /**
     * 构建结束时在quota中移除运行agent
     */
    fun removeRunningAgent(buildVariables: BuildVariables) {
        val pipelineRetryCount = buildVariables.variables[PIPELINE_RETRY_COUNT] ?: "0"

        var retryCount = 0
        val result = HttpRetryUtils.retry {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|removeRunningAgent")
            }
            quotaApi.removeRunningAgent(
                projectId = buildVariables.projectId,
                buildId = buildVariables.buildId,
                vmSeqId = buildVariables.vmSeqId,
                executeCount = pipelineRetryCount.toInt() + 1,
                retryCount = retryCount++
            )
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Remove running agent failed.")
        }
    }
}
