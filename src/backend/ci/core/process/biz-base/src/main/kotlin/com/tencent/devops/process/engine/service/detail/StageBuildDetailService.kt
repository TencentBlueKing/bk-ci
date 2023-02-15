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
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.service.StageTagService
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Suppress("LongParameterList", "MagicNumber")
@Service
class StageBuildDetailService(
    dslContext: DSLContext,
    pipelineBuildDao: PipelineBuildDao,
    buildDetailDao: BuildDetailDao,
    stageTagService: StageTagService,
    pipelineEventDispatcher: PipelineEventDispatcher,
    redisOperation: RedisOperation
) : BaseBuildDetailService(
    dslContext,
    pipelineBuildDao,
    buildDetailDao,
    stageTagService,
    pipelineEventDispatcher,
    redisOperation
) {

    fun updateStageStatus(
        projectId: String,
        buildId: String,
        stageId: String,
        buildStatus: BuildStatus,
        executeCount: Int
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|update_stage_status|stageId=$stageId|status=$buildStatus")
        var allStageStatus: List<BuildStageStatus>? = null
        update(projectId, buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = buildStatus.name
                    stage.executeCount = executeCount
                    if (buildStatus.isRunning() && stage.startEpoch == null) {
                        stage.startEpoch = System.currentTimeMillis()
                    } else if (buildStatus.isFinish() && stage.startEpoch != null) {
                        stage.elapsed = System.currentTimeMillis() - stage.startEpoch!!
                    }
                    allStageStatus = fetchHistoryStageStatus(model, buildStatus)
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING, operation = "updateStageStatus#$stageId")
        return allStageStatus ?: emptyList()
    }

    fun stageSkip(projectId: String, buildId: String, stageId: String): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_skip|stageId=$stageId")
        var allStageStatus: List<BuildStageStatus>? = null
        update(projectId, buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = BuildStatus.SKIP.name
                    stage.containers.forEach {
                        it.status = BuildStatus.SKIP.name
                    }
                    allStageStatus = fetchHistoryStageStatus(model, BuildStatus.RUNNING)
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING, operation = "stageSkip#$stageId")
        return allStageStatus ?: emptyList()
    }

    fun stagePause(
        projectId: String,
        buildId: String,
        stageId: String,
        controlOption: PipelineBuildStageControlOption,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_pause|stageId=$stageId")
        var allStageStatus: List<BuildStageStatus>? = null
        update(projectId, buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = BuildStatus.PAUSE.name
                    stage.stageControlOption = controlOption.stageControlOption
                    stage.startEpoch = System.currentTimeMillis()
                    stage.checkIn = checkIn
                    stage.checkOut = checkOut
                    allStageStatus = fetchHistoryStageStatus(
                        model = model,
                        buildStatus = BuildStatus.REVIEWING,
                        reviewers = checkIn?.groupToReview()?.reviewers
                    )
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.STAGE_SUCCESS, operation = "stagePause#$stageId")
        return allStageStatus ?: emptyList()
    }

    fun stageCancel(
        projectId: String,
        buildId: String,
        stageId: String,
        controlOption: PipelineBuildStageControlOption,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ) {
        logger.info("[$buildId]|stage_cancel|stageId=$stageId")
        update(projectId, buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = ""
                    stage.stageControlOption = controlOption.stageControlOption
                    stage.checkIn = checkIn
                    stage.checkOut = checkOut
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.STAGE_SUCCESS, operation = "stageCancel#$stageId")
    }

    fun stageCheckQuality(
        projectId: String,
        buildId: String,
        stageId: String,
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
        var allStageStatus: List<BuildStageStatus>? = null
        update(projectId, buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.stageControlOption = controlOption.stageControlOption
                    stage.checkIn = checkIn
                    stage.checkOut = checkOut
                    allStageStatus = fetchHistoryStageStatus(model, newBuildStatus)
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, newBuildStatus, operation = "stageCheckQuality#$stageId")
        pipelineBuildDao.updateStatus(dslContext, projectId, buildId, oldBuildStatus, newBuildStatus)
        return allStageStatus ?: emptyList()
    }

    fun stageReview(
        projectId: String,
        buildId: String,
        stageId: String,
        controlOption: PipelineBuildStageControlOption,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ) {
        logger.info("[$buildId]|stage_review|stageId=$stageId")
        update(projectId, buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.stageControlOption = controlOption.stageControlOption
                    stage.checkIn = checkIn
                    stage.checkOut = checkOut
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.STAGE_SUCCESS, operation = "stageReview#$stageId")
    }

    fun stageStart(
        projectId: String,
        buildId: String,
        stageId: String,
        controlOption: PipelineBuildStageControlOption,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|stage_start|stageId=$stageId")
        var allStageStatus: List<BuildStageStatus>? = null
        update(projectId, buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = BuildStatus.QUEUE.name
                    stage.stageControlOption = controlOption.stageControlOption
                    stage.checkIn = checkIn
                    stage.checkOut = checkOut
                    allStageStatus = fetchHistoryStageStatus(model, BuildStatus.RUNNING)
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING, operation = "stageStart#$stageId")
        return allStageStatus ?: emptyList()
    }
}
