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

package com.tencent.devops.process.engine.service.record

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.process.service.StageTagService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Suppress("LongParameterList", "MagicNumber")
@Service
class ContainerBuildRecordService(
    private val dslContext: DSLContext,
    private val buildRecordContainerDao: BuildRecordContainerDao,
    private val buildRecordTaskDao: BuildRecordTaskDao,
    stageTagService: StageTagService,
    buildRecordModelDao: BuildRecordModelDao,
    pipelineEventDispatcher: PipelineEventDispatcher,
    redisOperation: RedisOperation
) : BaseBuildRecordService(
    dslContext = dslContext,
    buildRecordModelDao = buildRecordModelDao,
    stageTagService = stageTagService,
    pipelineEventDispatcher = pipelineEventDispatcher,
    redisOperation = redisOperation
) {

    fun getRecord(
        transactionContext: DSLContext?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int
    ): BuildRecordContainer? {
        return buildRecordContainerDao.getRecord(
            transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = containerId,
            executeCount = executeCount
        )
    }

    fun batchSave(
        transactionContext: DSLContext?,
        containerList: List<BuildRecordContainer>,
        taskList: List<BuildRecordTask>
    ) {
        buildRecordTaskDao.batchSave(transactionContext ?: dslContext, taskList)
        buildRecordContainerDao.batchSave(transactionContext ?: dslContext, containerList)
    }

//    fun batchUpdate(transactionContext: DSLContext?, containerList: List<BuildRecordContainer>) {
//        return buildRecordContainerDao.batchUpdate(transactionContext ?: dslContext, containerList)
//    }

    fun containerPreparing(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int
    ) {
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.RUNNING,
            cancelUser = null, operation = "containerPreparing#$containerId"
        ) {
            updateContainerByMap(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                containerId = containerId, executeCount = executeCount,
                buildStatus = BuildStatus.PREPARE_ENV,
                containerVar = mapOf(
                    Container::startVMStatus.name to BuildStatus.RUNNING.name
                )
            )
        }
    }

    fun containerStarted(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int,
        containerBuildStatus: BuildStatus
    ) {
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.RUNNING,
            cancelUser = null, operation = "containerStarted#$containerId"
        ) {
            updateContainerByMap(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                containerId = containerId, executeCount = executeCount,
                buildStatus = if (containerBuildStatus.isFailure()) {
                    containerBuildStatus
                } else {
                    BuildStatus.RUNNING
                },
                containerVar = mapOf(
                    Container::startVMStatus.name to containerBuildStatus.name
                )
            )
        }
    }

    fun updateContainerStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        operation: String
    ) {
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.RUNNING,
            cancelUser = null, operation = "updateContainerStatus#$containerId"
        ) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val recordContainer = buildRecordContainerDao.getRecord(
                    dslContext = context, projectId = projectId, pipelineId = pipelineId,
                    buildId = buildId, containerId = containerId, executeCount = executeCount
                ) ?: run {
                    logger.warn(
                        "ENGINE|$buildId|updateContainerByMap| get container($containerId) record failed."
                    )
                    return@transaction
                }
                val containerVar = mutableMapOf<String, Any>()
                containerVar.putAll(recordContainer.containerVar)
                val containerName = containerVar[Container::name.name]?.toString() ?: ""
                if (buildStatus.isReadyToRun()) {
                    when (recordContainer.containerType) {
                        VMBuildContainer.classType -> containerVar[VMBuildContainer::mutexGroup.name]
                        NormalContainer.classType -> containerVar[NormalContainer::mutexGroup.name]
                        else -> null
                    }?.let {
                        containerVar[Container::name.name] = ContainerUtils.getMutexWaitName(containerName)
                    }
                } else {
                    ContainerUtils.getMutexFixedContainerName(containerName)
                }
                containerVar[Container::executeCount.name] = executeCount

                if (buildStatus.isFinish() &&
                    !BuildStatus.parse(containerVar[Container::startVMStatus.name]?.toString()).isFinish()) {
                    containerVar[Container::startVMStatus.name] = buildStatus.name
                }
                // TODO 耗时计算
                buildRecordContainerDao.updateRecord(
                    dslContext = context, projectId = projectId, pipelineId = pipelineId,
                    buildId = buildId, containerId = containerId, executeCount = executeCount,
                    containerVar = containerVar.plus(containerVar), buildStatus = buildStatus,
                    timestamps = null
                )
            }
        }
    }

    fun updateMatrixGroupContainer(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        matrixGroupId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        matrixOption: MatrixControlOption,
        modelContainer: Container?
    ) {
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.RUNNING,
            cancelUser = null, operation = "updateMatrixGroupContainer#$matrixGroupId"
        ) {
            logger.info(
                "[$buildId]|matrix_group_record|j(${modelContainer?.containerId})|" +
                    "groupId=$matrixGroupId|status=$buildStatus"
            )
            updateContainerByMap(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                containerId = matrixGroupId, executeCount = executeCount,
                buildStatus = buildStatus,
                containerVar = mapOf(
                    VMBuildContainer::matrixControlOption.name to matrixOption
                )
            )
        }
    }

    fun containerSkip(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        containerId: String
    ) {
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.RUNNING,
            cancelUser = null, operation = "containerSkip#$containerId"
        ) {
            logger.info("[$buildId]|container_skip|j($containerId)")
            updateContainerByMap(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                containerId = containerId, executeCount = executeCount,
                buildStatus = BuildStatus.SKIP, containerVar = mapOf(
                    Container::startVMStatus.name to BuildStatus.SKIP.name
                )
            )
            buildRecordTaskDao.getRecords(
                dslContext = dslContext, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, containerId = containerId, executeCount = executeCount
            ).forEach { task ->
                buildRecordTaskDao.updateRecord(
                    dslContext = dslContext, projectId = projectId, pipelineId = pipelineId,
                    buildId = buildId, taskId = task.taskId, executeCount = executeCount,
                    taskVar = task.taskVar, buildStatus = BuildStatus.SKIP,
                    timestamps = null
                )
            }
        }
    }

    private fun updateContainerByMap(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int,
        containerVar: Map<String, Any>,
        buildStatus: BuildStatus,
        timestamps: List<BuildRecordTimeStamp>? = null
    ) {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordVar = buildRecordContainerDao.getRecordContainerVar(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, containerId = containerId, executeCount = executeCount
            )?.toMutableMap() ?: run {
                logger.warn(
                    "ENGINE|$buildId|updateContainerByMap| get container($containerId) record failed."
                )
                return@transaction
            }
            buildRecordContainerDao.updateRecord(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, containerId = containerId, executeCount = executeCount,
                containerVar = recordVar.plus(containerVar), buildStatus = buildStatus,
                timestamps = timestamps
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerBuildRecordService::class.java)
    }
}
