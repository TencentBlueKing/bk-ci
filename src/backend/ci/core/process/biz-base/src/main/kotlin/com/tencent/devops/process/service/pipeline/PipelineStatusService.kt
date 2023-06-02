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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.PipelineStatus
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineStatusService(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineBuildTaskDao: PipelineBuildTaskDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    // 获取单条流水线的运行状态
    fun getPipelineStatus(projectId: String, pipelineId: String): PipelineStatus? {
        val pipelineInfo = pipelineInfoDao.getPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = ChannelCode.BS,
            pipelineId = pipelineId
        ) ?: return null
        val pipelineSetting = pipelineSettingDao.getSetting(dslContext, projectId, pipelineId) ?: return null
        val pipelineBuildSummary = pipelineRuntimeService.getBuildSummaryRecord(projectId, pipelineId) ?: return null
        val buildStatusOrd = pipelineBuildSummary.latestStatus
        val finishCount = pipelineBuildSummary.finishCount ?: 0
        val runningCount = pipelineBuildSummary.runningCount ?: 0

        val pipelineBuildStatus = getBuildStatus(buildStatusOrd)

        // 获取构建执行进度
        val buildTaskCountList = pipelineBuildTaskDao.countGroupByBuildId(
            dslContext = dslContext,
            projectId = projectId,
            buildIds = listOf(pipelineBuildSummary.latestBuildId)
        )
        val lastBuildTotalCount = buildTaskCountList.sumOf { it.value3() }
        val lastBuildFinishCount =
            buildTaskCountList.filter { it.value2() == BuildStatus.SUCCEED.ordinal }.sumOf { it.value3() }

        // 获取触发方式
        val buildInfo = pipelineBuildDao.getBuildInfo(dslContext, projectId, pipelineBuildSummary.latestBuildId)

        // todo还没想好与Pipeline结合，减少这部分的代码，收归一处
        return PipelineStatus(
            taskCount = pipelineInfo.taskCount,
            buildCount = (finishCount + runningCount).toLong(),
            canManualStartup = pipelineInfo.manualStartup == 1,
            currentTimestamp = System.currentTimeMillis(),
            hasCollect = false, // 无关紧要参数
            latestBuildEndTime = (pipelineBuildSummary.latestEndTime)?.timestampmilli() ?: 0,
            latestBuildEstimatedExecutionSeconds = 1L,
            latestBuildId = pipelineBuildSummary.latestBuildId,
            latestBuildNum = pipelineBuildSummary.buildNum,
            latestBuildStartTime = (pipelineBuildSummary.latestStartTime)?.timestampmilli() ?: 0,
            latestBuildStatus = pipelineBuildStatus,
            latestBuildTaskName = pipelineBuildSummary.latestTaskName,
            lock = PipelineRunLockType.checkLock(pipelineSetting.runLockType),
            runningBuildCount = pipelineBuildSummary.runningCount ?: 0,
            lastBuildFinishCount = lastBuildFinishCount,
            lastBuildTotalCount = lastBuildTotalCount,
            trigger = buildInfo?.trigger
        )
    }

    /**
     * 获取构建状态
     */
    fun getBuildStatus(buildStatusOrd: Int?): BuildStatus? {
        return if (buildStatusOrd != null) {
            val tmpStatus = BuildStatus.values()[buildStatusOrd.coerceAtMost(BuildStatus.values().size - 1)]
            if (tmpStatus.isFinish()) {
                BuildStatusSwitcher.pipelineStatusMaker.finish(tmpStatus)
            } else {
                tmpStatus
            }
        } else {
            null
        }
    }
}
