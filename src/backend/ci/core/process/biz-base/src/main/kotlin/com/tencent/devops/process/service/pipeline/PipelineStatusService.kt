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
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.PipelineStatus
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("ALL")
@Service
class PipelineStatusService(private val pipelineRuntimeService: PipelineRuntimeService) {

    // 获取单条流水线的运行状态
    fun getPipelineStatus(projectId: String, pipelineId: String): PipelineStatus? {
        val pipelineInfo = pipelineRuntimeService.getBuildSummaryRecords(
            projectId = projectId, channelCode = ChannelCode.BS, pipelineIds = setOf(pipelineId)
        ).firstOrNull() ?: return null
        val buildStatusOrd = pipelineInfo["LATEST_STATUS"] as Int?
        val finishCount = pipelineInfo["FINISH_COUNT"] as Int? ?: 0
        val runningCount = pipelineInfo["RUNNING_COUNT"] as Int? ?: 0

        val pipelineBuildStatus = if (buildStatusOrd != null) {
            val tmpStatus = BuildStatus.values()[buildStatusOrd.coerceAtMost(BuildStatus.values().size - 1)]
            if (tmpStatus.isFinish()) {
                BuildStatusSwitcher.pipelineStatusMaker.finish(tmpStatus)
            } else {
                tmpStatus
            }
        } else {
            null
        }

        // todo还没想好与Pipeline结合，减少这部分的代码，收归一处
        return PipelineStatus(
            taskCount = pipelineInfo["TASK_COUNT"] as Int,
            buildCount = (finishCount + runningCount).toLong(),
            canManualStartup = pipelineInfo["MANUAL_STARTUP"] as Int == 1,
            currentTimestamp = System.currentTimeMillis(),
            hasCollect = false, // 无关紧要参数
            latestBuildEndTime = (pipelineInfo["LATEST_END_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
            latestBuildEstimatedExecutionSeconds = 1L,
            latestBuildId = pipelineInfo["LATEST_BUILD_ID"] as String,
            latestBuildNum = pipelineInfo["BUILD_NUM"] as Int,
            latestBuildStartTime = (pipelineInfo["LATEST_START_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
            latestBuildStatus = pipelineBuildStatus,
            latestBuildTaskName = pipelineInfo["LATEST_TASK_NAME"] as String?,
            lock = PipelineRunLockType.checkLock(pipelineInfo["RUN_LOCK_TYPE"] as Int?),
            runningBuildCount = pipelineInfo["RUNNING_COUNT"] as Int? ?: 0
        )
    }
}
