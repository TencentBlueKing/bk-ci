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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.service.PipelineContainerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DependOnControl @Autowired constructor(
    private val pipelineContainerService: PipelineContainerService,
    private val buildLogPrinter: BuildLogPrinter
) {

    fun dependOnJobStatus(container: PipelineBuildContainer): BuildStatus {
        val dependRel = container.controlOption.jobControlOption.dependOnContainerId2JobIds
            ?: return BuildStatus.SUCCEED // 没有设置依赖关系，直接返回成功
        val logBuilder = StringBuilder("Current job depends on ${dependRel.values}, current status: \n")

        val buildStatus = checkJobStatusByDepRel(container = container, dependRel = dependRel, logBuilder = logBuilder)

        buildLogPrinter.addLine(
            buildId = container.buildId,
            message = logBuilder.toString(),
            tag = VMUtils.genStartVMTaskId(container.seq.toString()),
            jobId = container.containerHashId,
            executeCount = container.executeCount
        )
        return buildStatus
    }

    private fun checkJobStatusByDepRel(
        container: PipelineBuildContainer,
        dependRel: Map<String, String>,
        logBuilder: StringBuilder
    ): BuildStatus {
        var foundFailure = false
        var foundSkip = false
        var successCnt = 0
        val jobStatusMap = pipelineContainerService.listContainers(
            projectId = container.projectId,
            buildId = container.buildId,
            stageId = container.stageId
        ).associate { it.containerId to it.status }

        for (it in dependRel.entries) {
            val dependOnJobStatus = jobStatusMap[it.key]
            logBuilder.append("${it.value} $dependOnJobStatus \n")
            // 无状态（兼容），成功状态，计数+1
            if (dependOnJobStatus == BuildStatus.SKIP) { // 如果发现依赖的Job被跳过
                foundSkip = true
            } else if (dependOnJobStatus == null || dependOnJobStatus.isSuccess()) {
                successCnt++
            } else if (dependOnJobStatus.isFailure() || dependOnJobStatus.isCancel()) { // 发现非正常构建结束，则表示失败
                foundFailure = true
            }

            if (foundSkip || foundFailure) {
                break
            }
        }

        return when {
            foundSkip -> {
                logBuilder.append("Skip\n")
                BuildStatus.SKIP
            }
            foundFailure -> {
                logBuilder.append("Terminated\n")
                BuildStatus.FAILED
            }
            successCnt == dependRel.size -> { // 全部依赖计数
                logBuilder.append("Begin\n")
                BuildStatus.SUCCEED
            }
            else -> {
                logBuilder.append("successJob: $successCnt, dependOnJob: ${dependRel.size}, Waiting...\n")
                container.status
            }
        }
    }
}
