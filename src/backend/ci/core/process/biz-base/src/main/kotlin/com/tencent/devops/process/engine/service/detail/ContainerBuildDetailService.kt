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

package com.tencent.devops.process.engine.service.detail

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.service.StageTagService
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Suppress("LongParameterList", "MagicNumber")
@Service
class ContainerBuildDetailService(
    dslContext: DSLContext,
    pipelineBuildDao: PipelineBuildDao,
    buildDetailDao: BuildDetailDao,
    pipelineEventDispatcher: PipelineEventDispatcher,
    stageTagService: StageTagService,
    redisOperation: RedisOperation
) : BaseBuildDetailService(
    dslContext,
    pipelineBuildDao,
    buildDetailDao,
    stageTagService,
    pipelineEventDispatcher,
    redisOperation
) {

    fun containerPreparing(projectId: String, buildId: String, containerId: String) {
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false
                override fun onFindContainer(container: Container, stage: Stage): Traverse {
                    val targetContainer = container.getContainerById(containerId)
                    val containerStatus = targetContainer?.status
                    val startVMStatus = targetContainer?.startVMStatus
                    logger.info("[$buildId]|containerPreparing|j($containerId)|$containerStatus|$startVMStatus")
                    if (targetContainer != null && (containerStatus == null ||
                            !BuildStatus.valueOf(containerStatus).isFinish())
                    ) {
                        targetContainer.startEpoch = System.currentTimeMillis()
                        targetContainer.status = BuildStatus.PREPARE_ENV.name
                        targetContainer.startVMStatus = BuildStatus.RUNNING.name
                        update = true
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "containerPreparing#$containerId"
        )
    }

    fun containerStarted(
        projectId: String,
        buildId: String,
        containerId: String,
        containerBuildStatus: BuildStatus
    ) {
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false

                override fun onFindContainer(container: Container, stage: Stage): Traverse {
                    logger.info("[$buildId]|containerStarted|j($containerId)")
                    val targetContainer = container.getContainerById(containerId)
                    if (targetContainer != null) {
                        if (targetContainer.startEpoch != null) {
                            targetContainer.systemElapsed = System.currentTimeMillis() - targetContainer.startEpoch!!
                        }
                        targetContainer.startVMStatus = containerBuildStatus.name
                        // #2074 containerBuildStatus如果是失败的，则将Job整体状态设置为失败
                        if (containerBuildStatus.isFailure()) {
                            targetContainer.status = containerBuildStatus.name
                        } else {
                            targetContainer.status = BuildStatus.RUNNING.name
                        }
                        update = true
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "containerStarted#$containerId"
        )
    }

    fun updateContainerStatus(
        projectId: String,
        buildId: String,
        containerId: String,
        buildStatus: BuildStatus,
        executeCount: Int
    ) {
        logger.info("[$buildId]|updateContainerStatus|j($containerId)|status=$buildStatus|e=$executeCount")
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false

                override fun onFindContainer(container: Container, stage: Stage): Traverse {
                    val targetContainer = container.getContainerById(containerId)
                    if (targetContainer != null) {
                        update = true
                        if (buildStatus.isReadyToRun()) {
                            when (targetContainer) {
                                is VMBuildContainer -> targetContainer.mutexGroup
                                is NormalContainer -> targetContainer.mutexGroup
                                else -> null
                            }?.let {
                                container.name = ContainerUtils.getMutexWaitName(container.name)
                            }
                        } else {
                            container.name = ContainerUtils.getMutexFixedContainerName(container.name)
                        }
                        targetContainer.status = buildStatus.name
                        targetContainer.executeCount = executeCount

                        if (buildStatus.isFinish() && !BuildStatus.parse(targetContainer.startVMStatus).isFinish()) {
                            targetContainer.startVMStatus = targetContainer.status
                        }
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING, operation = "updateContainerStatus#$containerId"
        )
    }

    fun updateMatrixGroupContainer(
        projectId: String,
        buildId: String,
        stageId: String,
        matrixGroupId: String,
        buildStatus: BuildStatus,
        matrixOption: MatrixControlOption,
        modelContainer: Container?
    ) {
        logger.info(
            "[$buildId]|matrix_group|j(${modelContainer?.containerId})|groupId=$matrixGroupId|status=$buildStatus"
        )
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false
                override fun onFindContainer(container: Container, stage: Stage): Traverse {
                    if (stageId == stage.id && container.id == matrixGroupId && container.matrixGroupFlag == true) {
                        update = true
                        container.status = buildStatus.name
                        if (container is VMBuildContainer) {
                            container.matrixControlOption = matrixOption
                            if (modelContainer is VMBuildContainer) {
                                container.groupContainers = modelContainer.groupContainers
                            }
                        } else if (container is NormalContainer) {
                            container.matrixControlOption = matrixOption
                            if (modelContainer is NormalContainer) {
                                container.groupContainers = modelContainer.groupContainers
                            }
                        }
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING, operation = "matrix_group#$matrixGroupId"
        )
    }

    fun containerSkip(projectId: String, buildId: String, containerId: String) {
        logger.info("[$buildId]|container_skip|j($containerId)")
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false

                override fun onFindContainer(container: Container, stage: Stage): Traverse {
                    val targetContainer = container.getContainerById(containerId)
                    if (targetContainer !is TriggerContainer && targetContainer != null) {
                        // 兼容id字段
                        update = true
                        targetContainer.status = BuildStatus.SKIP.name
                        targetContainer.startVMStatus = BuildStatus.SKIP.name
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING, operation = "containerSkip#$containerId"
        )
    }

    fun saveBuildVmInfo(projectId: String, pipelineId: String, buildId: String, containerId: String, vmInfo: VmInfo) {
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false

                override fun onFindContainer(container: Container, stage: Stage): Traverse {
                    val targetContainer = container.getContainerById(containerId)
                    if (targetContainer != null) {
                        if (targetContainer is VMBuildContainer && targetContainer.showBuildResource == true) {
                            targetContainer.name = vmInfo.name
                        }
                        update = true
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "saveBuildVmInfo($projectId,$pipelineId)"
        )
    }
}
