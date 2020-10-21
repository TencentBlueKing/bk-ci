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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.dao.PipelineBuildStageDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.common.BS_MANUAL_START_STAGE
import com.tencent.devops.process.engine.common.BS_STAGE_CANCELED_END_SOURCE
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import com.tencent.devops.process.service.StageTagService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线Stage相关的服务
 * @version 1.0
 */
@Service
class PipelineStageService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dslContext: DSLContext,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildStageDao: PipelineBuildStageDao,
    private val stageTagService: StageTagService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineStageService::class.java)
    }

    fun getStage(buildId: String, stageId: String?): PipelineBuildStage? {
        val result = pipelineBuildStageDao.get(dslContext, buildId, stageId)
        if (result != null) {
            return pipelineBuildStageDao.convert(result)
        }
        return null
    }

    fun updateStageStatus(buildId: String, stageId: String, buildStatus: BuildStatus) {
        logger.info("[$buildId]|updateStageStatus|status=$buildStatus|stageId=$stageId")
        pipelineBuildStageDao.updateStatus(
            dslContext = dslContext,
            buildId = buildId,
            stageId = stageId,
            buildStatus = buildStatus
        )
    }

    fun listStages(buildId: String): List<PipelineBuildStage> {
        val list = pipelineBuildStageDao.listByBuildId(dslContext, buildId)
        val result = mutableListOf<PipelineBuildStage>()
        if (list.isNotEmpty()) {
            list.forEach {
                result.add(pipelineBuildStageDao.convert(it)!!)
            }
        }
        return result
    }

    fun skipStage(
        buildId: String,
        stageId: String
    ) {
        updateStageStatus(buildId, stageId, BuildStatus.SKIP)
        SpringContextUtil.getBean(PipelineBuildDetailService::class.java).stageSkip(buildId, stageId)
    }

    fun pauseStage(
        pipelineId: String,
        buildId: String,
        stageId: String,
        controlOption: PipelineBuildStageControlOption
    ) {
        logger.info("[$buildId]|pauseStage|stageId=$stageId|controlOption=$controlOption")
        pipelineBuildStageDao.updateStatus(
            dslContext = dslContext,
            buildId = buildId,
            stageId = stageId,
            buildStatus = BuildStatus.PAUSE,
            controlOption = controlOption
        )
        SpringContextUtil.getBean(PipelineBuildDetailService::class.java).stagePause(
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            controlOption = controlOption
        )
    }

    fun startStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        controlOption: PipelineBuildStageControlOption
    ) {
        controlOption.stageControlOption.triggered = true
        pipelineBuildStageDao.updateStatus(
            dslContext = dslContext,
            buildId = buildId,
            stageId = stageId,
            buildStatus = BuildStatus.QUEUE,
            controlOption = controlOption
        )
        SpringContextUtil.getBean(PipelineBuildDetailService::class.java).stageStart(
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            controlOption = controlOption
        )
        pipelineEventDispatcher.dispatch(
            PipelineBuildStageEvent(
                source = BS_MANUAL_START_STAGE,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                stageId = stageId,
                actionType = ActionType.REFRESH
            )
        )
    }

    fun cancelStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String
    ) {
        updateStageStatus(buildId, stageId, BuildStatus.REVIEW_ABORT)
        SpringContextUtil.getBean(PipelineBuildDetailService::class.java)
            .stageCancel(buildId, stageId)
        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishEvent(
                source = BS_STAGE_CANCELED_END_SOURCE,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                status = BuildStatus.STAGE_SUCCESS
            )
        )
    }

    fun getDefaultStageTagId(): String? {
        return stageTagService.getDefaultStageTag().data?.id
    }

    fun updatePipelineRunningCount(pipelineId: String, buildId: String, runningIncrement: Int) {
        pipelineBuildSummaryDao.updateRunningCount(
            dslContext = dslContext,
            pipelineId = pipelineId,
            buildId = buildId,
            runningIncrement = runningIncrement
        )
    }
}
