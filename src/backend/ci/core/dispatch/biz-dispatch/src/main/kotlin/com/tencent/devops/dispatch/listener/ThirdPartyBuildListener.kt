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

package com.tencent.devops.dispatch.listener

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyDevCloudDispatchType
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.service.ThirdPartyDispatchService
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ThirdPartyBuildListener @Autowired constructor(
    private val thirdPartyDispatchService: ThirdPartyDispatchService,
    private val tpaCommonUtil: TPACommonUtil
) : BuildListener {

    override fun getStartupQueue(): String {
        return ""
    }

    override fun getStartupDemoteQueue(): String {
        return ""
    }

    override fun getShutdownQueue(): String {
        return ""
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        // 包一层用来计算耗时
        try {
            doOnStartup(dispatchMessage)
        } catch (e: Throwable) {
            // 抓到了肯定是需要结束的异常
            if (dispatchMessage.event.dispatchQueueStartTimeMilliSecond != null) {
                tpaCommonUtil.updateQueueTime(
                    event = dispatchMessage.event,
                    createTime = dispatchMessage.event.dispatchQueueStartTimeMilliSecond!!,
                    endTime = LocalDateTime.now().timestampmilli()
                )
            }
            throw e
        }
    }

    private fun doOnStartup(dispatchMessage: DispatchMessage) {
        try {
            thirdPartyDispatchService.startUp(dispatchMessage)
        } catch (e: DispatchRetryMQException) {
            // 重试构建消息
            retry(
                sleepTimeInMS = 5000,
                retryTimes = (12 * (dispatchMessage.event.queueTimeoutMinutes ?: 10)),
                errorMessage = e.message
            )
        }
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        thirdPartyDispatchService.startUp(dispatchMessage)
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        thirdPartyDispatchService.finishBuild(event)
    }

    override fun consumerFilter(dispatchType: DispatchType): Boolean {
        return dispatchType is ThirdPartyAgentIDDispatchType ||
            dispatchType is ThirdPartyAgentEnvDispatchType ||
            dispatchType is ThirdPartyDevCloudDispatchType
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.OTHER
    }
}
