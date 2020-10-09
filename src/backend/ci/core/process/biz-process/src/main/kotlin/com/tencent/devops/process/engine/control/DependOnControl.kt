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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DependOnControl @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val buildLogPrinter: BuildLogPrinter
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun dependOnStatus(
        event: PipelineBuildContainerEvent,
        container: PipelineBuildContainer
    ): BuildStatus {
        val containerId2JobIds = container.controlOption?.jobControlOption?.dependOnContainerId2JobIds
            ?: return BuildStatus.SUCCEED
        val containers = pipelineRuntimeService.listContainers(container.buildId, container.stageId)
        var successCnt = 0

        val jobStatusMap = containers.associate { it.containerId to it.status }
        val logBuilder = StringBuilder("Current job depends on ${containerId2JobIds.values} succeed, current status：\n")
        containerId2JobIds.forEach container@{
            val dependOnJobStatus = jobStatusMap[it.key]
            if (dependOnJobStatus == null) {
                logBuilder.append("${it.value} SKIP\n")
                successCnt++
                return@container
            }
            if (dependOnJobStatus == BuildStatus.SKIP ||
                BuildStatus.isFailure(dependOnJobStatus)) {
                logger.warn("[${event.buildId}]|stage=${event.stageId}|container=${event.containerId}| failure due to the status of  depend on jobId:(${it.value}) is ($dependOnJobStatus)")
                logBuilder.append("${it.value} $dependOnJobStatus\nTerminated")
                buildLogPrinter.addRedLine(
                    buildId = container.buildId, message = logBuilder.toString(),
                    tag = VMUtils.genStartVMTaskId(container.seq.toString()), jobId = container.containerId,
                    executeCount = container.executeCount
                )
                return BuildStatus.FAILED
            }
            logBuilder.append("${it.value} $dependOnJobStatus\n")
            if (BuildStatus.isSuccess(dependOnJobStatus)) {
                successCnt++
            }
        }
        logger.info("[${event.buildId}]|stage=${event.stageId}|container=${event.containerId}| successCnt:$successCnt, dependOnCnt:${containerId2JobIds.size}")
        var buildStatus = container.status
        if (successCnt == containerId2JobIds.size) {
            buildStatus = BuildStatus.SUCCEED
            logBuilder.append("Begin\n")
        } else {
            logBuilder.append("Waiting...\n")
        }
        buildLogPrinter.addLine(
            buildId = container.buildId, message = logBuilder.toString(),
            tag = VMUtils.genStartVMTaskId(container.seq.toString()), jobId = container.containerId,
            executeCount = container.executeCount
        )
        return buildStatus
    }

    fun updateContainerStatus(
        container: PipelineBuildContainer,
        buildStatus: BuildStatus
    ) {
        with(container) {
            pipelineRuntimeService.updateContainerStatus(
                buildId = buildId,
                stageId = stageId,
                containerId = containerId,
                buildStatus = buildStatus,
                startTime = LocalDateTime.now(),
                endTime = LocalDateTime.now()
            )
            pipelineBuildDetailService.updateContainerStatus(buildId, containerId, buildStatus)
        }
    }
}