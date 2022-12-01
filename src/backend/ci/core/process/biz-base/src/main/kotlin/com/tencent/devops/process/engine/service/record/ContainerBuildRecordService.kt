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

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.time.BuildRecordTimeCost
import com.tencent.devops.process.pojo.pipeline.record.time.BuildRecordTimeStamp
import com.tencent.devops.process.service.StageTagService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("LongParameterList", "MagicNumber")
@Service
class ContainerBuildRecordService(
    private val dslContext: DSLContext,
    private val buildRecordContainerDao: BuildRecordContainerDao,
    private val buildRecordTaskDao: BuildRecordTaskDao,
    pipelineBuildDao: PipelineBuildDao,
    buildDetailDao: BuildDetailDao,
    pipelineEventDispatcher: PipelineEventDispatcher,
    stageTagService: StageTagService,
    redisOperation: RedisOperation
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

    fun batchSave(transactionContext: DSLContext?, containerList: List<BuildRecordContainer>) {
        return buildRecordContainerDao.batchSave(transactionContext ?: dslContext, containerList)
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
        updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = containerId,
            executeCount = executeCount,
            containerVar = mapOf(
                Container::status.name to BuildStatus.PREPARE_ENV.name,
                Container::startVMStatus.name to BuildStatus.RUNNING.name
            ),
            startTime = LocalDateTime.now(),
            operation = "containerPreparing#$containerId"
        )
    }

    fun containerStarted(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int,
        containerBuildStatus: BuildStatus
    ) {
        updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = containerId,
            executeCount = executeCount,
            containerVar = mapOf(
                Container::status.name to if (containerBuildStatus.isFailure()) {
                    containerBuildStatus.name
                } else {
                    BuildStatus.RUNNING.name
                },
                Container::startVMStatus.name to containerBuildStatus.name
            ),
            operation = "containerStarted#$containerId"
        )
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
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordContainer = buildRecordContainerDao.getRecord(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                containerId = containerId,
                executeCount = executeCount
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
            containerVar[Container::status.name] = buildStatus.name
            containerVar[Container::executeCount.name] = executeCount

            if (buildStatus.isFinish() &&
                !BuildStatus.parse(containerVar[Container::startVMStatus.name]?.toString()).isFinish()) {
                containerVar[Container::startVMStatus.name] = buildStatus.name
            }
            // TODO 耗时计算
            buildRecordContainerDao.updateRecord(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                containerId = containerId,
                executeCount = executeCount,
                containerVar = containerVar.plus(containerVar),
                startTime = null,
                endTime = if (buildStatus.isFinish()) LocalDateTime.now() else null,
                timestamps = null,
                timeCost = null
            )
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
        logger.info(
            "[$buildId]|matrix_group_record|j(${modelContainer?.containerId})|" +
                "groupId=$matrixGroupId|status=$buildStatus"
        )
        updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = matrixGroupId,
            executeCount = executeCount,
            containerVar = mapOf(
                Container::status.name to buildStatus.name,
                VMBuildContainer::matrixControlOption.name to matrixOption
            ),
            startTime = LocalDateTime.now(),
            operation = "updateMatrixGroupContainer#$matrixGroupId"
        )
    }

    fun containerSkip(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        containerId: String
    ) {
        logger.info("[$buildId]|container_skip|j($containerId)")
        updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = containerId,
            executeCount = executeCount,
            containerVar = mapOf(
                Container::status.name to BuildStatus.SKIP.name,
                Container::startVMStatus.name to BuildStatus.SKIP.name
            ),
            startTime = LocalDateTime.now(),
            operation = "containerSkip#$containerId"
        )
        buildRecordTaskDao.getRecords(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = containerId,
            executeCount = executeCount
        ).forEach { task ->
            buildRecordTaskDao.updateRecord(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = task.taskId,
                executeCount = executeCount,
                startTime = null,
                endTime = null,
                taskVar = task.taskVar.plus(
                    mapOf(Element::status.name to BuildStatus.SKIP.name)
                ),
                timestamps = null,
                timeCost = null
            )
        }
    }

    private fun updateContainerByMap(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        executeCount: Int,
        containerVar: Map<String, Any>,
        operation: String,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        timestamps: List<BuildRecordTimeStamp>? = null,
        timeCost: BuildRecordTimeCost? = null
    ) {
        val watcher = Watcher(id = "updateDetail#$buildId#$operation")
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            watcher.start("getRecord")
            val recordVar = buildRecordContainerDao.getRecordContainerVar(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                containerId = containerId,
                executeCount = executeCount
            )?.toMutableMap() ?: run {
                logger.warn(
                    "ENGINE|$buildId|updateContainerByMap| get container($containerId) record failed."
                )
                return@transaction
            }
            watcher.start("updateRecord")
            buildRecordContainerDao.updateRecord(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                containerId = containerId,
                executeCount = executeCount,
                containerVar = recordVar.plus(containerVar),
                startTime = startTime,
                endTime = endTime,
                timestamps = timestamps,
                timeCost = timeCost
            )
            watcher.start("updated")
        }
        watcher.stop()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerBuildRecordService::class.java)
    }
}
