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

package com.tencent.devops.dockerhost.cron

import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.services.DockerHostBuildService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

// @Component
class Runner @Autowired constructor(private val dockerHostBuildService: DockerHostBuildService) {
    private val logger = LoggerFactory.getLogger(Runner::class.java)
    private val maxRunningContainerNum = 200

//    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 5 * 1000)
    fun startBuild() {
        try {
            val containerNum = dockerHostBuildService.getContainerNum()
            if (containerNum >= maxRunningContainerNum) {
                logger.warn("Too many containers in this host, break to start build.")
                return
            }

            val dockerStartBuildInfo = try {
                dockerHostBuildService.startBuild()
            } catch (e: Exception) {
                logger.warn("Fail to start build", e)
                return
            }
            if (dockerStartBuildInfo != null) {
                if (dockerStartBuildInfo.status == PipelineTaskStatus.RUNNING.status) {
                    logger.warn("Create container, dockerStartBuildInfo: $dockerStartBuildInfo")

                    try {
                        val containerId = dockerHostBuildService.createContainer(dockerStartBuildInfo)
                        // 上报containerId给dispatch
                        dockerHostBuildService.reportContainerId(dockerStartBuildInfo.buildId, dockerStartBuildInfo.vmSeqId, containerId)

                        if (dockerHostBuildService.isContainerRunning(containerId)) {
                            dockerHostBuildService.log(dockerStartBuildInfo.buildId, "构建环境启动成功，等待Agent启动...")
                        } else {
                            dockerHostBuildService.rollbackBuild(dockerStartBuildInfo.buildId, dockerStartBuildInfo.vmSeqId, true)
                        }
                    } catch (e: ContainerException) {
                        logger.error("Create container failed, rollback build. buildId: ${dockerStartBuildInfo.buildId}, vmSeqId: ${dockerStartBuildInfo.vmSeqId}")
                        dockerHostBuildService.rollbackBuild(dockerStartBuildInfo.buildId, dockerStartBuildInfo.vmSeqId, false)
                    }
                }
            }
        } catch (t: Throwable) {
            logger.error("StartBuild encounter unknown exception", t)
        }
    }

//    @Scheduled(initialDelay = 120 * 1000, fixedDelay = 20 * 1000)
    fun endBuild() {
        try {
            val dockerEndBuildInfo = try {
                dockerHostBuildService.endBuild()
            } catch (e: Exception) {
                logger.warn("Fail to end build", e)
                return
            }
            if (dockerEndBuildInfo != null) {
                logger.warn("dockerEndBuidlInfo: $dockerEndBuildInfo")
                if (dockerEndBuildInfo.status == PipelineTaskStatus.DONE.status || dockerEndBuildInfo.status == PipelineTaskStatus.FAILURE.status) {
                    logger.warn("Stop the container, containerId: ${dockerEndBuildInfo.containerId}")
                    dockerHostBuildService.stopContainer(dockerEndBuildInfo)
                }
            }
        } catch (t: Throwable) {
            logger.error("EndBuild encounter unknown exception", t)
        }
    }

//    @Scheduled(initialDelay = 300 * 1000, fixedDelay = 3600 * 1000)
    fun clearExitedContainer() {
        try {
            dockerHostBuildService.clearContainers()
        } catch (t: Throwable) {
            logger.error("EndBuild encounter unknown exception", t)
        }
    }
}
