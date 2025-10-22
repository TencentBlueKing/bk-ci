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

package com.tencent.devops.process.trigger.scm.listener

import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.engine.service.WebhookBuildParameterService
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import org.springframework.stereotype.Service

/**
 * 流水线触发参数监听器
 */
@Service
class WebhookTriggerParamListener(
    private val buildLogPrinter: BuildLogPrinter,
    private val webhookBuildParameterService: WebhookBuildParameterService
) : WebhookTriggerListenerSupport() {
    override fun onBuildSuccess(context: WebhookTriggerContext) {
        with(context) {
            if (buildId?.id.isNullOrBlank()) return
            // #2958 webhook触发在触发原子上输出变量
            startParams?.let {
                buildLogPrinter.addLines(
                    buildId = buildId!!.id,
                    logMessages = it.map { entry ->
                        LogMessage(
                            message = "${entry.key}=${entry.value}",
                            timestamp = System.currentTimeMillis(),
                            tag = it[PIPELINE_START_TASK_ID]?.toString() ?: ""
                        )
                    }
                )
            }
            webhookBuildParameterService.save(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId!!.id,
                buildParameters = startParams?.map { BuildParameters(key = it.key, value = it.value ?: "") } ?: listOf()
            )
        }
    }
}
