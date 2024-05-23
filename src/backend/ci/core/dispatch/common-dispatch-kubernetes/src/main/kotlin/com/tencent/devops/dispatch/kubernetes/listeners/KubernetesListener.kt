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

package com.tencent.devops.dispatch.kubernetes.listeners

import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.dispatch.kubernetes.service.DispatchBuildService
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KubernetesListener @Autowired constructor(
    private val dispatchBuildService: DispatchBuildService
) : BuildListener {

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesListener::class.java)
    }

    override fun getStartupQueue() = DispatchRouteKeySuffix.KUBERNETES.routeKeySuffix

    override fun getShutdownQueue() = DispatchRouteKeySuffix.KUBERNETES.routeKeySuffix

    override fun getStartupDemoteQueue(): String = DispatchRouteKeySuffix.KUBERNETES.routeKeySuffix

    override fun getVmType() = JobQuotaVmType.KUBERNETES

    override fun onStartup(dispatchMessage: DispatchMessage) {
        logger.info("On start up - ($dispatchMessage)")
        startup(dispatchMessage)
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        logger.info("On startup demote - ($dispatchMessage)")
        startup(dispatchMessage)
    }

    private fun startup(dispatchMessage: DispatchMessage) {
        val retry = dispatchBuildService.preStartUp(dispatchMessage)
        if (retry) {
            retry()
        } else {
            dispatchBuildService.startUp(dispatchMessage)
        }
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        dispatchBuildService.doShutdown(event)
    }
}
