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

package com.tencent.devops.dispatch.docker.client

import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerRoutingType
import com.tencent.devops.common.dispatch.sdk.service.DockerRoutingSdkService
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.dispatch.docker.client.context.BuildLessStartHandlerContext
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.process.engine.common.VMUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BuildLessStartPrepareHandler @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val dockerRoutingSdkService: DockerRoutingSdkService,
    private val buildLessStartDispatchHandler: BuildLessStartDispatchHandler
) : Handler<BuildLessStartHandlerContext>() {
    private val logger = LoggerFactory.getLogger(BuildLessStartPrepareHandler::class.java)

    override fun handlerRequest(handlerContext: BuildLessStartHandlerContext) {
        with(handlerContext) {
            // 设置日志打印关键字
            handlerContext.buildLogKey = "${event.pipelineId}|${event.buildId}|${event.vmSeqId}|$retryTime"
            logger.info("$buildLogKey start select buildLess.")

            // 区分无编译集群（k8s集群和原始docker集群）
            val dockerRoutingType = dockerRoutingSdkService.getDockerRoutingType(event.projectId)
            if (dockerRoutingType == DockerRoutingType.KUBERNETES) {
                clusterType = DockerHostClusterType.K8S_BUILD_LESS
            }

            if (event.retryTime == 0) {
                buildLogPrinter.addLine(
                    buildId = event.buildId,
                    message = "Prepare BuildLess Job(#${event.vmSeqId})...",
                    tag = VMUtils.genStartVMTaskId(event.vmSeqId),
                    containerHashId = event.containerHashId,
                    executeCount = event.executeCount ?: 1,
                    jobId = null,
                    stepId = VMUtils.genStartVMTaskId(event.vmSeqId)
                )
            }

            buildLessStartDispatchHandler.handlerRequest(this)
        }
    }
}
