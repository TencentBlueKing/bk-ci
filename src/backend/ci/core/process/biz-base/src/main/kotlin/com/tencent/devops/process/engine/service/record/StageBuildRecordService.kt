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

import com.tencent.devops.common.api.constant.BUILD_CANCELED
import com.tencent.devops.common.api.constant.BUILD_COMPLETED
import com.tencent.devops.common.api.constant.BUILD_FAILED
import com.tencent.devops.common.api.constant.BUILD_REVIEWING
import com.tencent.devops.common.api.constant.BUILD_RUNNING
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import com.tencent.devops.process.engine.service.detail.BaseBuildDetailService
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
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
class StageBuildRecordService(
    private val dslContext: DSLContext,
    private val buildRecordStageDao: BuildRecordStageDao,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val stageTagService: StageTagService,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation
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

        val stageVar = mutableMapOf<String, Any>()
        if (buildStatus.isRunning() && stageVar[Stage::startEpoch.name] == null) {
            stageVar[Stage::startEpoch.name] = System.currentTimeMillis()
        } else if (buildStatus.isFinish() && stageVar[Stage::startEpoch.name] != null) {
            stageVar[Stage::elapsed.name] =
                System.currentTimeMillis() - stageVar[Stage::startEpoch.name].toString().toLong()
        }
        stageVar[Stage::status.name] = buildStatus.name

        return updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            executeCount = executeCount,
            stageVar = stageVar,
            operation = "updateStageStatus#$stageId",
            buildStatus = buildStatus
        )
    }

    fun stageSkip(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_skip|stageId=$stageId")
        // TODO 刷新所有container为SKIP
        return updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            executeCount = executeCount,
            stageVar = mapOf(
                Stage::status.name to BuildStatus.RUNNING.name
            ),
            operation = "stageSkip#$stageId",
            buildStatus = BuildStatus.RUNNING
        )
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

        val stageVar = mutableMapOf<String, Any>()
        stageVar[Stage::status.name] = BuildStatus.SKIP.name
        stageVar[Stage::startEpoch.name] = System.currentTimeMillis()
        stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
        stageVar[Stage::startEpoch.name] = System.currentTimeMillis()
        checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
        checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }

        return updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            executeCount = executeCount,
            stageVar = stageVar,
            operation = "stagePause#$stageId",
            buildStatus = BuildStatus.REVIEWING,
            reviewers = checkIn?.groupToReview()?.reviewers
        )
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

        val stageVar = mutableMapOf<String, Any>()
        stageVar[Stage::status.name] = BuildStatus.SKIP.name
        stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
        stageVar[Stage::startEpoch.name] = System.currentTimeMillis()
        checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
        checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }

        return updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            executeCount = executeCount,
            stageVar = stageVar,
            operation = "stageCancel#$stageId",
            buildStatus = BuildStatus.RUNNING
        )
    }

    fun stageCheckQuality(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        controlOption: PipelineBuildStageControlOption,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_check_quality|stageId=$stageId|checkIn=$checkIn|checkOut=$checkOut")
        val (oldBuildStatus, newBuildStatus) = if (checkIn?.status == BuildStatus.QUALITY_CHECK_WAIT.name ||
            checkOut?.status == BuildStatus.QUALITY_CHECK_WAIT.name) {
            Pair(BuildStatus.RUNNING, BuildStatus.REVIEWING)
        } else {
            Pair(BuildStatus.REVIEWING, BuildStatus.RUNNING)
        }
        val stageVar = mutableMapOf<String, Any>()
        stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
        checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
        checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }

        val allStageStatus = updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            executeCount = executeCount,
            stageVar = stageVar,
            operation = "stageCheckQuality#$stageId",
            buildStatus = BuildStatus.RUNNING
        )
        pipelineBuildDao.updateStatus(dslContext, projectId, buildId, oldBuildStatus, newBuildStatus)
        return allStageStatus
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

        val stageVar = mutableMapOf<String, Any>()
        stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
        checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
        checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }

        return updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            executeCount = executeCount,
            stageVar = stageVar,
            operation = "stageReview#$stageId",
            buildStatus = BuildStatus.STAGE_SUCCESS
        )
    }

    fun stageStart(
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

        val stageVar = mutableMapOf<String, Any>()
        stageVar[Stage::status.name] = BuildStatus.QUEUE.name
        stageVar[Stage::stageControlOption.name] = controlOption.stageControlOption
        checkIn?.let { stageVar[Stage::checkIn.name] = checkIn }
        checkOut?.let { stageVar[Stage::checkOut.name] = checkOut }

        return updateContainerByMap(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            executeCount = executeCount,
            stageVar = stageVar,
            operation = "stageStart#$stageId",
            buildStatus = BuildStatus.RUNNING
        )
    }

    fun updateContainerByMap(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        executeCount: Int,
        stageVar: Map<String, Any>,
        buildStatus: BuildStatus,
        operation: String,
        reviewers: List<String>? = null,
        errorMsg: String? = null,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        timestamps: List<BuildRecordTimeStamp>? = null,
        timeCost: BuildRecordTimeCost? = null
    ): List<BuildStageStatus> {
        val watcher = Watcher(id = "updateDetail#$buildId#$operation")
        var allStageStatus: List<BuildStageStatus>? = null
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            watcher.start("getRecords")
            val recordStages = buildRecordStageDao.getRecords(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = stageId,
                executeCount = executeCount
            )
            val stage = recordStages.find { it.stageId == stageId } ?: run {
                logger.warn(
                    "ENGINE|$buildId|updateStageStatus| get stage($stageId) record failed."
                )
                return@transaction
            }
            stage.stageVar.putAll(stageVar)
            allStageStatus = fetchHistoryStageStatus(
                recordStages = recordStages,
                buildStatus = buildStatus,
                reviewers = reviewers
            )
            watcher.start("updateRecord")
            buildRecordStageDao.updateRecord(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                executeCount = executeCount,
                stageVar = stageVar,
                startTime = startTime,
                endTime = endTime,
                timestamps = timestamps,
                timeCost = timeCost
            )
            watcher.start("updated")
        }
        watcher.stop()
        return allStageStatus ?: emptyList()
    }

    private fun fetchHistoryStageStatus(
        recordStages: List<BuildRecordStage>,
        buildStatus: BuildStatus,
        reviewers: List<String>? = null,
        errorMsg: String? = null,
        cancelUser: String? = null
    ): List<BuildStageStatus> {
        val stageTagMap: Map<String, String>
            by lazy { stageTagService.getAllStageTag().data!!.associate { it.id to it.stageTagName } }
        // 更新Stage状态至BuildHistory
        val (statusMessage, reason) = if (buildStatus == BuildStatus.REVIEWING) {
            Pair(BUILD_REVIEWING, reviewers?.joinToString(","))
        } else if (buildStatus.isFailure()) {
            Pair(BUILD_FAILED, errorMsg ?: buildStatus.name)
        } else if (buildStatus.isCancel()) {
            Pair(BUILD_CANCELED, cancelUser)
        } else if (buildStatus.isSuccess()) {
            Pair(BUILD_COMPLETED, null)
        } else {
            Pair(BUILD_RUNNING, null)
        }
        return recordStages.map {
            BuildStageStatus(
                stageId = it.stageId,
                name = it.stageVar[Stage::name.name]?.toString() ?: it.stageId,
                status = it.stageVar[Stage::status.name].toString(),
                startEpoch = it.stageVar[Stage::startEpoch.name].toString().toLong(),
                elapsed = it.stageVar[Stage::elapsed.name].toString().toLong(),
                tag = (it.stageVar[Stage::tag.name] as List<String>).map { _it ->
                    stageTagMap.getOrDefault(_it, "null")
                },
                // #6655 利用stageStatus中的第一个stage传递构建的状态信息
                showMsg = if (it.stageId == STATUS_STAGE) {
                    MessageCodeUtil.getCodeLanMessage(statusMessage) + (reason?.let { ": $reason" } ?: "")
                } else null
            )
        }
    }

    companion object {
        const val STATUS_STAGE = "stage-1"
        private val logger = LoggerFactory.getLogger(StageBuildRecordService::class.java)
    }
}
