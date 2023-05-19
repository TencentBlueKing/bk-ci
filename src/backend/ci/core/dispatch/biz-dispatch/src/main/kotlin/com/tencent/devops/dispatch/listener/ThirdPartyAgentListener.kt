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

package com.tencent.devops.dispatch.listener

import com.tencent.devops.common.api.constant.CommonMessageCode.JOB_BUILD_STOPS
import com.tencent.devops.common.api.constant.CommonMessageCode.UNABLE_GET_PIPELINE_JOB_STATUS
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.exception.VMTaskFailException
import com.tencent.devops.dispatch.service.PipelineDispatchService
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import feign.RetryableException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ThirdPartyAgentListener @Autowired constructor(
    private val pipelineDispatchService: PipelineDispatchService,
    private val client: Client
) {
    @BkTimed
    fun listenAgentStartUpEvent(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {
        try {
            if (checkRunning(pipelineAgentStartupEvent)) {
                pipelineDispatchService.startUp(pipelineAgentStartupEvent)
            }
        } catch (e: RetryableException) {
            logger.warn("[${pipelineAgentStartupEvent.buildId}]|feign fail, do retry again", e)
            pipelineDispatchService.reDispatch(pipelineAgentStartupEvent)
        } catch (ignored: Throwable) {
            logger.error("Fail to start the pipe build($pipelineAgentStartupEvent)", ignored)
        }
    }

    fun listenAgentShutdownEvent(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        try {
            pipelineDispatchService.shutdown(pipelineAgentShutdownEvent)
        } catch (ignored: Throwable) {
            logger.error("Fail to start the pipe build($pipelineAgentShutdownEvent)", ignored)
        }
    }

    private fun checkRunning(event: PipelineAgentStartupEvent): Boolean {
        // 判断流水线当前container是否在运行中
        val statusResult = client.get(ServicePipelineTaskResource::class).getTaskStatus(
            projectId = event.projectId,
            buildId = event.buildId,
            taskId = VMUtils.genStartVMTaskId(event.containerId)
        )

        if (statusResult.isNotOk() || statusResult.data == null) {
            logger.warn("The build event($event) fail to check if pipeline task is running " +
                            "because of ${statusResult.message}")
            throw VMTaskFailException(
                I18nUtil.getCodeLanMessage(UNABLE_GET_PIPELINE_JOB_STATUS)
            )
        }

        if (!statusResult.data!!.isRunning()) {
            logger.warn("The build event($event) is not running")
            throw VMTaskFailException(
                I18nUtil.getCodeLanMessage(JOB_BUILD_STOPS)
            )
        }

        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentListener::class.java)
    }
}
