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

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeLine
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.common.BuildTimeCostUtils.generateContainerTimeCost
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.service.StageTagService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("LongParameterList", "MagicNumber", "LongMethod")
@Service
class ContainerBuildRecordService(
    private val dslContext: DSLContext,
    private val recordContainerDao: BuildRecordContainerDao,
    private val recordTaskDao: BuildRecordTaskDao,
    private val buildTaskDao: PipelineBuildTaskDao,
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
        return recordContainerDao.getRecord(
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
        recordTaskDao.batchSave(transactionContext ?: dslContext, taskList)
        recordContainerDao.batchSave(transactionContext ?: dslContext, containerList)
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
            updateContainerRecord(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                containerId = containerId, executeCount = executeCount,
                buildStatus = BuildStatus.PREPARE_ENV,
                containerVar = mapOf(
                    Container::startVMStatus.name to BuildStatus.RUNNING.name
                ),
                timestamps = mapOf(
                    BuildTimestampType.JOB_CONTAINER_STARTUP to
                        BuildRecordTimeStamp(LocalDateTime.now().timestampmilli(), null)
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
            updateContainerRecord(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                containerId = containerId, executeCount = executeCount,
                buildStatus = if (containerBuildStatus.isFailure()) {
                    containerBuildStatus
                } else {
                    BuildStatus.RUNNING
                },
                containerVar = mapOf(
                    Container::startVMStatus.name to containerBuildStatus.name,
                    Container::startEpoch.name to System.currentTimeMillis()
                ),
                timestamps = mapOf(
                    BuildTimestampType.JOB_CONTAINER_STARTUP to
                        BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
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
                val recordContainer = recordContainerDao.getRecord(
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
                var startTime: LocalDateTime? = null
                var endTime: LocalDateTime? = null
                // 存在互斥组的先将名字修改
                if (buildStatus.isReadyToRun()) {
                    if (recordContainer.startTime == null) {
                        startTime = LocalDateTime.now()
                    }
                    when (recordContainer.containerType) {
                        VMBuildContainer.classType -> containerVar[VMBuildContainer::mutexGroup.name]
                        NormalContainer.classType -> containerVar[NormalContainer::mutexGroup.name]
                        else -> null
                    }?.let {
                        containerVar[Container::name.name] = ContainerUtils.getMutexWaitName(containerName)
                    }
                } else {
                    containerVar[Container::name.name] = ContainerUtils.getMutexFixedContainerName(containerName)
                }

                // 结束时进行启动状态校准，并计算所有耗时
                val newTimestamps = mutableMapOf<BuildTimestampType, BuildRecordTimeStamp>()

                if (buildStatus.isFinish()) {
                    if (recordContainer.endTime == null) {
                        endTime = LocalDateTime.now()
                    }
                    if (!BuildStatus.parse(containerVar[Container::startVMStatus.name]?.toString()).isFinish()) {
                        containerVar[Container::startVMStatus.name] = buildStatus.name
                    }
                    newTimestamps[BuildTimestampType.JOB_CONTAINER_SHUTDOWN] = BuildRecordTimeStamp(
                        null, LocalDateTime.now().timestampmilli()
                    )
                    val recordTasks = recordTaskDao.getRecords(
                        context, projectId, pipelineId, buildId, executeCount, containerId
                    )
                    buildTaskDao.getByContainerId(context, projectId, buildId, containerId)
                        .associateBy { it.taskId }
                    val (cost, timeLine) = recordContainer.generateContainerTimeCost(recordTasks)
                    containerVar[Container::timeCost.name] = cost
                    containerVar[BuildRecordTimeLine::class.java.simpleName] = timeLine
                }
                recordContainerDao.updateRecord(
                    dslContext = context, projectId = projectId, pipelineId = pipelineId,
                    buildId = buildId, containerId = containerId, executeCount = executeCount,
                    containerVar = containerVar.plus(containerVar), buildStatus = buildStatus,
                    startTime = startTime, endTime = endTime,
                    timestamps = mergeTimestamps(newTimestamps, recordContainer.timestamps)
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
            updateContainerRecord(
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
            updateContainerRecord(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                containerId = containerId, executeCount = executeCount, buildStatus = BuildStatus.SKIP,
                containerVar = mapOf(
                    Container::startVMStatus.name to BuildStatus.SKIP.name
                )
            )
            recordTaskDao.getRecords(
                dslContext = dslContext, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, containerId = containerId, executeCount = executeCount
            ).forEach { task ->
                recordTaskDao.updateRecord(
                    dslContext = dslContext, projectId = projectId, pipelineId = pipelineId,
                    buildId = buildId, taskId = task.taskId, executeCount = executeCount,
                    taskVar = task.taskVar, buildStatus = BuildStatus.SKIP,
                    startTime = null, endTime = null, timestamps = null
                )
            }
        }
    }

    fun updateContainerRecord(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int,
        containerVar: Map<String, Any>,
        buildStatus: BuildStatus?,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>? = null
    ) {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordContainer = recordContainerDao.getRecord(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, containerId = containerId, executeCount = executeCount
            ) ?: run {
                logger.warn(
                    "ENGINE|$buildId|updateContainerByMap| get container($containerId) record failed."
                )
                return@transaction
            }
            var startTime: LocalDateTime? = null
            var endTime: LocalDateTime? = null
            if (buildStatus?.isRunning() == true && recordContainer.startTime == null) {
                startTime = LocalDateTime.now()
            }
            if (buildStatus?.isFinish() == true && recordContainer.endTime == null) {
                endTime = LocalDateTime.now()
            }
            recordContainerDao.updateRecord(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, containerId = containerId, executeCount = executeCount,
                containerVar = recordContainer.containerVar.plus(containerVar),
                startTime = startTime, endTime = endTime, buildStatus = buildStatus,
                timestamps = timestamps?.let { mergeTimestamps(timestamps, recordContainer.timestamps) }
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerBuildRecordService::class.java)
    }
}
