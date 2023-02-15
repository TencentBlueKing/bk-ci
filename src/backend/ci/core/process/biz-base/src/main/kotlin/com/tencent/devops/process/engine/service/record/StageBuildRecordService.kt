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
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.engine.common.BuildTimeCostUtils
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildStageDao
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.service.StageTagService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("LongParameterList", "MagicNumber")
@Service
class StageBuildRecordService(
    private val dslContext: DSLContext,
    private val recordStageDao: BuildRecordStageDao,
    private val recordContainerDao: BuildRecordContainerDao,
    private val buildStageDao: PipelineBuildStageDao,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val pipelineBuildDao: PipelineBuildDao,
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

    fun updateStageStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        buildStatus: BuildStatus
    ): List<BuildStageStatus> {
        logger.info(
            "[$buildId]|update_stage_status|stageId=$stageId|" +
                "status=$buildStatus|executeCount=$executeCount"
        )
        var allStageStatus: List<BuildStageStatus>? = null
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.RUNNING,
            cancelUser = null, operation = "updateStageStatus#$stageId"
        ) {
            val stageVar = mutableMapOf<String, Any>()
            if (buildStatus.isRunning() && stageVar[Stage::startEpoch.name] == null) {
                stageVar[Stage::startEpoch.name] = System.currentTimeMillis()
            } else if (buildStatus.isFinish() && stageVar[Stage::startEpoch.name] != null) {
                stageVar[Stage::elapsed.name] =
                    System.currentTimeMillis() - stageVar[Stage::startEpoch.name].toString().toLong()
            }
            allStageStatus = updateStageRecord(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                stageId = stageId, executeCount = executeCount, stageVar = stageVar,
                buildStatus = buildStatus
            )
        }
        return allStageStatus ?: emptyList()
    }

    fun stageSkip(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        containers: List<PipelineBuildContainer>
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_skip|stageId=$stageId")
        var allStageStatus: List<BuildStageStatus>? = null
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.RUNNING,
            cancelUser = null, operation = "stageSkip#$stageId"
        ) {
            containers.forEach { container ->
                containerBuildRecordService.containerSkip(
                    projectId, pipelineId, buildId, executeCount, container.containerId
                )
            }
            allStageStatus = updateStageRecord(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                stageId = stageId, executeCount = executeCount, buildStatus = BuildStatus.SKIP,
                stageVar = mutableMapOf()
            )
        }
        return allStageStatus ?: emptyList()
    }

    fun stagePause(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        controlOption: PipelineBuildStageControlOption,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_pause|stageId=$stageId")

        var allStageStatus: List<BuildStageStatus>? = null
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.STAGE_SUCCESS,
            cancelUser = null, operation = "stagePause#$stageId"
        ) {
            val stageVar = mutableMapOf<String, Any>()
            stageVar[Stage::startEpoch.name] = System.currentTimeMillis()
            stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
            stageVar[Stage::startEpoch.name] = System.currentTimeMillis()
            checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
            checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }
            allStageStatus = updateStageRecord(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                stageId = stageId, executeCount = executeCount, stageVar = stageVar,
                buildStatus = null, reviewers = checkIn?.groupToReview()?.reviewers
            )
        }
        return allStageStatus ?: emptyList()
    }

    fun stageCancel(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        controlOption: PipelineBuildStageControlOption,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_cancel|stageId=$stageId")
        var allStageStatus: List<BuildStageStatus>? = null
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.CANCELED,
            cancelUser = null, operation = "stageCancel#$stageId"
        ) {
            val stageVar = mutableMapOf<String, Any>()
            stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
            stageVar[Stage::startEpoch.name] = System.currentTimeMillis()
            checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
            checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }

            allStageStatus = updateStageRecord(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                executeCount = executeCount,
                stageVar = stageVar,
                buildStatus = null
            )
        }
        return allStageStatus ?: emptyList()
    }

    fun stageCheckQuality(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        controlOption: PipelineBuildStageControlOption,
        inOrOut: Boolean,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_check_quality|stageId=$stageId|checkIn=$checkIn|checkOut=$checkOut")
        var allStageStatus: List<BuildStageStatus>? = null
        val timestamps = mutableMapOf<BuildTimestampType, BuildRecordTimeStamp>()
        val timestampType = if (inOrOut) {
            BuildTimestampType.STAGE_CHECK_IN_WAITING
        } else {
            BuildTimestampType.STAGE_CHECK_OUT_WAITING
        }
        val (oldBuildStatus, newBuildStatus) = if (checkIn?.status == BuildStatus.QUALITY_CHECK_WAIT.name ||
            checkOut?.status == BuildStatus.QUALITY_CHECK_WAIT.name
        ) {
            // 即将卡审核
            timestamps[timestampType] = BuildRecordTimeStamp(LocalDateTime.now().timestampmilli(), null)
            Pair(BuildStatus.RUNNING, BuildStatus.REVIEWING)
        } else {
            // 即将完成审核
            timestamps[timestampType] = BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
            Pair(BuildStatus.REVIEWING, BuildStatus.RUNNING)
        }

        update(
            projectId, pipelineId, buildId, executeCount, newBuildStatus,
            cancelUser = null, operation = "stageCheckQuality#$stageId"
        ) {
            val stageVar = mutableMapOf<String, Any>()
            stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
            checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
            checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }
            allStageStatus = updateStageRecord(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                executeCount = executeCount,
                stageVar = stageVar,
                buildStatus = null, // 红线不改变stage原状态
                timestamps = timestamps
            )
            pipelineBuildDao.updateStatus(dslContext, projectId, buildId, oldBuildStatus, newBuildStatus)
        }
        return allStageStatus ?: emptyList()
    }

    fun stageReview(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        controlOption: PipelineBuildStageControlOption,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_review|stageId=$stageId")
        var allStageStatus: List<BuildStageStatus>? = null
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.STAGE_SUCCESS,
            cancelUser = null, operation = "stageReview#$stageId"
        ) {
            val stageVar = mutableMapOf<String, Any>()
            stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
            checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
            checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }

            allStageStatus = updateStageRecord(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                executeCount = executeCount,
                stageVar = stageVar,
                buildStatus = null
            )
        }
        return allStageStatus ?: emptyList()
    }

    fun stageManualStart(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        controlOption: PipelineBuildStageControlOption,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_start|stageId=$stageId")
        var allStageStatus: List<BuildStageStatus>? = null
        update(
            projectId, pipelineId, buildId, executeCount, BuildStatus.RUNNING,
            cancelUser = null, operation = "stageStart#$stageId"
        ) {
            val stageVar = mutableMapOf<String, Any>()
            stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
            checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
            checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }

            allStageStatus = updateStageRecord(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                executeCount = executeCount,
                stageVar = stageVar,
                buildStatus = BuildStatus.QUEUE
            )
        }
        return allStageStatus ?: emptyList()
    }

    fun updateStageRecord(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        stageVar: MutableMap<String, Any>,
        buildStatus: BuildStatus?,
        reviewers: List<String>? = null,
        errorMsg: String? = null,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>? = null
    ): List<BuildStageStatus> {
        var allStageStatus: List<BuildStageStatus>? = null
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordStages = recordStageDao.getRecords(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = executeCount
            )
            val recordStage = recordStages.find { it.stageId == stageId } ?: run {
                logger.warn(
                    "ENGINE|$buildId|updateStageStatus|cannot get stage($stageId) record."
                )
                return@transaction
            }
            // 结束时进行启动状态校准，并计算所有耗时
            var timeCost: BuildRecordTimeCost? = null
            if (buildStatus?.isFinish() == true) {
                buildStageDao.get(dslContext, projectId, buildId, stageId)?.let { buildStage ->
                    val recordContainers = recordContainerDao.getRecords(
                        context, projectId, pipelineId, buildId, executeCount, stageId
                    )
                    timeCost = BuildTimeCostUtils.generateStageTimeCost(
                        buildStage, recordStage, recordContainers
                    )
                    timeCost?.let { stageVar[Stage::timeCost.name] = it }
                }
            }
            allStageStatus = buildStatus?.let {
                fetchHistoryStageStatus(
                    recordStages = recordStages,
                    buildStatus = buildStatus,
                    reviewers = reviewers,
                    errorMsg = errorMsg,
                    timeCost = timeCost
                )
            }
            recordStageDao.updateRecord(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                executeCount = executeCount,
                stageVar = recordStage.stageVar.plus(stageVar),
                buildStatus = buildStatus,
                timestamps = timestamps?.let { mergeTimestamps(timestamps, recordStage.timestamps) }
            )
        }
        return allStageStatus ?: emptyList()
    }

    companion object {
        const val TRIGGER_STAGE = "stage-1"
        private val logger = LoggerFactory.getLogger(StageBuildRecordService::class.java)
    }
}
