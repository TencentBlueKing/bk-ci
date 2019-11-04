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

package com.tencent.devops.dockerhost.services

import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.DockerLogsResponse
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunResponse
import com.tencent.devops.dockerhost.pojo.Status
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Service
class DockerService @Autowired constructor(private val dockerHostBuildService: DockerHostBuildService) {

    private val executor = Executors.newFixedThreadPool(10)
    private val buildTask = mutableMapOf<String, Future<Pair<Boolean, String?>>>()

    fun buildImage(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        elementId: String?,
        dockerBuildParam: DockerBuildParam,
        outer: Boolean = false
    ): Boolean {
        logger.info("[$buildId]|projectId=$projectId|pipelineId=$pipelineId|vmSeqId=$vmSeqId|param=$dockerBuildParam")

        val future = executor.submit(Callable<Pair<Boolean, String?>> {
            dockerHostBuildService.dockerBuildAndPushImage(
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                dockerBuildParam = dockerBuildParam,
                buildId = buildId,
                elementId = elementId,
                outer = outer
            )
        })

        buildTask[getKey(vmSeqId, buildId)] = future

        return true
    }

    fun getBuildResult(vmSeqId: String, buildId: String): Pair<Status, String> {
        logger.info("vmSeqId: $vmSeqId, buildId: $buildId")
        val status = getStatus(vmSeqId, buildId)
        logger.info("status: $status")
        if (status.first == Status.SUCCESS || status.first == Status.FAILURE) {
            logger.info("Delete the build image task: vmSeqId: $vmSeqId, buildId: $buildId, status: $status")
            buildTask.remove(getKey(vmSeqId, buildId))
        }
        return status
    }

    fun dockerRun(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        dockerRunParam: DockerRunParam
    ): DockerRunResponse {
        logger.info("projectId: $projectId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId, dockerRunParam: $dockerRunParam")

        val (containerId, timeStamp) = dockerHostBuildService.dockerRun(
            projectId,
            pipelineId,
            vmSeqId,
            buildId,
            dockerRunParam
        )
        return DockerRunResponse(containerId, timeStamp)
    }

    fun dockerStop(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, containerId: String) {
        logger.info("projectId: $projectId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId, containerId: $containerId")
        dockerHostBuildService.dockerStop(projectId, pipelineId, vmSeqId, buildId, containerId)
    }

    fun getDockerRunLogs(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        containerId: String,
        logStartTimeStamp: Int
    ): DockerLogsResponse {
        val isRunning = dockerHostBuildService.isContainerRunning(containerId)
        val exitCode = when {
            !isRunning -> dockerHostBuildService.getDockerRunExitCode(containerId)
            else -> null
        }
        val logs = dockerHostBuildService.getDockerLogs(containerId, logStartTimeStamp)
        return DockerLogsResponse(isRunning, exitCode, logs)
    }

    private fun getStatus(vmSeqId: String, buildId: String): Pair<Status, String> {
        val future = buildTask[getKey(vmSeqId, buildId)]
        return when {
            future == null -> Pair(Status.NO_EXISTS, "")
            future.isDone -> {
                when {
                    future.get().first -> Pair(Status.SUCCESS, "")
                    else -> Pair(Status.FAILURE, future.get().second ?: "")
                }
            }
            else -> Pair(Status.RUNNING, "")
        }
    }

    private fun getKey(vmSeqId: String, buildId: String): String {
        return "$buildId-$vmSeqId"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerService::class.java)
    }
}