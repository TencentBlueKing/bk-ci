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

package com.tencent.devops.dockerhost.services

import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.common.Constants
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.DockerHostLoad
import com.tencent.devops.dockerhost.pojo.DockerLogsResponse
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunResponse
import com.tencent.devops.dockerhost.pojo.Status
import com.tencent.devops.dockerhost.services.container.ContainerAgentUpHandler
import com.tencent.devops.dockerhost.services.container.ContainerCustomizedRunHandler
import com.tencent.devops.dockerhost.services.container.ContainerHandlerContext
import com.tencent.devops.dockerhost.services.container.ContainerPullImageHandler
import com.tencent.devops.dockerhost.services.container.ContainerRunHandler
import com.tencent.devops.dockerhost.utils.SystemInfoUtil.getAverageCpuLoad
import com.tencent.devops.dockerhost.utils.SystemInfoUtil.getAverageDiskIOLoad
import com.tencent.devops.dockerhost.utils.SystemInfoUtil.getAverageDiskLoad
import com.tencent.devops.dockerhost.utils.SystemInfoUtil.getAverageMemLoad
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Service
class DockerService @Autowired constructor(
    private val dockerHostBuildService: DockerHostBuildService,
    private val dockerHostImageService: DockerHostImageService,
    private val dockerHostBuildApi: DockerHostBuildResourceApi,
    private val containerPullImageHandler: ContainerPullImageHandler,
    private val containerRunHandler: ContainerRunHandler,
    private val containerAgentUpHandler: ContainerAgentUpHandler,
    private val containerCustomizedRunHandler: ContainerCustomizedRunHandler
) {

    private val executor = Executors.newFixedThreadPool(20)
    private val buildTask = mutableMapOf<String, Future<Pair<Boolean, String?>>>()

    fun buildImage(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        elementId: String?,
        dockerBuildParam: DockerBuildParam,
        outer: Boolean = false,
        scanFlag: Boolean? = false
    ): Boolean {
        logger.info("[$buildId]|projectId=$projectId|pipelineId=$pipelineId|vmSeqId=$vmSeqId|param=$dockerBuildParam")
        val future = executor.submit(Callable<Pair<Boolean, String?>> {
            dockerHostImageService.dockerBuildAndPushImage(
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                dockerBuildParam = dockerBuildParam,
                buildId = buildId,
                elementId = elementId,
                outer = outer,
                scanFlag = scanFlag ?: false
            )
        })
        buildTask[getKey(vmSeqId, buildId)] = future

        logger.info("[$buildId]|projectId=$projectId|pipelineId=$pipelineId|vmSeqId=$vmSeqId|future:$future")
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
        pipelineTaskId: String?,
        dockerRunParam: DockerRunParam
    ): DockerRunResponse {
        logger.info("$buildId|dockerRun|vmSeqId=$vmSeqId|image=${dockerRunParam.imageName}|${dockerRunParam.command}")

        val qpcGitProjectList = dockerHostBuildApi.getQpcGitProjectList(
            projectId = projectId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            poolNo = dockerRunParam.poolNo?.toInt() ?: 1
        )?.data

        var qpcUniquePath = ""
        if (qpcGitProjectList != null && qpcGitProjectList.isNotEmpty()) {
            qpcUniquePath = qpcGitProjectList.first()
        }

        val containerHandlerContext = ContainerHandlerContext(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId.toInt(),
            poolNo = dockerRunParam.poolNo?.toInt() ?: 1,
            userName = "",
            originImageName = dockerRunParam.imageName,
            imageType = ImageType.THIRD.type,
            agentId = null,
            secretKey = null,
            registryUser = dockerRunParam.registryUser,
            registryPwd = dockerRunParam.registryPwd,
            dockerRunParam = dockerRunParam,
            qpcUniquePath = qpcUniquePath,
            pipelineTaskId = pipelineTaskId
        )

        containerPullImageHandler.setNextHandler(containerCustomizedRunHandler).handlerRequest(containerHandlerContext)

        return containerHandlerContext.dockerRunResponse!!

        /*val (containerId, timeStamp, portBindingList) = dockerHostBuildService.dockerRun(
            projectId = projectId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            buildId = buildId,
            dockerRunParam = dockerRunParam
        )

        logger.info("$buildId|dockerRunEnd|vmSeqId=$vmSeqId|poolNo=${dockerRunParam.poolNo}")
        return DockerRunResponse(containerId, timeStamp, portBindingList)*/
    }

    fun dockerStop(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, containerId: String) {
        logger.info("$buildId|dockerStop|vmSeqId=$vmSeqId|containerId=$containerId dockerStop.")
        dockerHostBuildService.stopContainer(containerId, buildId)
    }

    fun getDockerRunLogs(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        containerId: String,
        logStartTimeStamp: Int,
        printLog: Boolean? = true
    ): DockerLogsResponse {
        logger.info("$buildId|getDockerRunLogs|vmSeqId=$vmSeqId|$containerId|logStartTimeStamp=$logStartTimeStamp")
        val containerState = dockerHostBuildService.getContainerState(containerId)
        val isRunning = if (containerState != null) {
            containerState.running ?: false
        } else {
            true
        }

        val exitCode = when {
            containerState != null -> if (containerState.exitCodeLong == null) {
                Constants.DOCKER_EXIST_CODE
            } else containerState.exitCodeLong!!.toInt()
            else -> null
        }

        val logs = if (printLog != null && !printLog) {
            emptyList()
        } else {
            dockerHostBuildService.getDockerLogs(containerId, logStartTimeStamp)
        }

        logger.info("$buildId|getDockerRunLogsEnd|vmSeqId=$vmSeqId|$containerId")
        return DockerLogsResponse(isRunning, exitCode, logs)
    }

    fun startBuild(dockerHostBuildInfo: DockerHostBuildInfo): String {
        logger.warn("Create container, dockerStartBuildInfo: $dockerHostBuildInfo")
        with(dockerHostBuildInfo) {
            val containerHandlerContext = ContainerHandlerContext(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                poolNo = poolNo,
                userName = "",
                originImageName = imageName,
                imageType = imageType,
                agentId = agentId,
                secretKey = secretKey,
                registryUser = registryUser,
                registryPwd = registryPwd,
                containerHashId = containerHashId,
                customBuildEnv = customBuildEnv,
                buildType = buildType,
                qpcUniquePath = qpcUniquePath,
                dockerResource = dockerResource,
                specialProjectList = specialProjectList
            )

            containerPullImageHandler.setNextHandler(
                containerRunHandler.setNextHandler(containerAgentUpHandler)
            ).handlerRequest(containerHandlerContext)

            return containerHandlerContext.containerId!!
        }
    }

    fun getDockerHostLoad(): DockerHostLoad {
        return DockerHostLoad(
            usedContainerNum = dockerHostBuildService.getContainerNum(),
            averageCpuLoad = getAverageCpuLoad(),
            averageMemLoad = getAverageMemLoad(),
            averageDiskLoad = getAverageDiskLoad(),
            averageDiskIOLoad = getAverageDiskIOLoad()
        )
    }

    fun getContainerStatus(containerId: String): Boolean {
        return dockerHostBuildService.isContainerRunning(containerId)
    }

    private fun getStatus(vmSeqId: String, buildId: String): Pair<Status, String> {
        val future = buildTask[getKey(vmSeqId, buildId)]
        return when {
            future == null -> Pair(Status.NO_EXISTS, "")
            future.isDone -> {
                when {
                    future.get().first -> Pair(Status.SUCCESS, future.get().second ?: "")
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
