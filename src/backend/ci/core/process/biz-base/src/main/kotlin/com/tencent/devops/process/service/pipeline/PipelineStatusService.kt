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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.PipelineStatus
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineStatusService(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    // 获取单条流水线的运行状态
    fun getPipelineStatus(pipelineId: String): PipelineStatus? {
        val watcher = Watcher(id = "getPipelineStatus|$pipelineId")
        try {
            watcher.start("s_r_summary")
            val pipelineInfo = pipelineRepositoryService.getPipelineInfo(pipelineId) ?: return null
            val summary = pipelineRuntimeService.getBuildSummaryRecord(pipelineId) ?: return null
            val buildStatusOrd = summary["LATEST_STATUS"] as Int?
            val finishCount = summary["FINISH_COUNT"] as Int? ?: 0
            val runningCount = summary["RUNNING_COUNT"] as Int? ?: 0
            return PipelineStatus( // todo还没想好与Pipeline结合，减少这部分的代码，收归一处
                taskCount = pipelineInfo.taskCount,
                buildCount = (finishCount + runningCount).toLong(),
                canManualStartup = pipelineInfo.canManualStartup,
                currentTimestamp = System.currentTimeMillis(),
                hasCollect = false, // 无关紧要参数
                latestBuildEndTime = (summary["LATEST_END_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                latestBuildEstimatedExecutionSeconds = 1L,
                latestBuildId = summary["LATEST_BUILD_ID"] as String,
                latestBuildNum = summary["BUILD_NUM"] as Int,
                latestBuildStartTime = (summary["LATEST_START_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                latestBuildStatus = if (buildStatusOrd != null) {
                    if (buildStatusOrd == BuildStatus.QUALITY_CHECK_FAIL.ordinal) {
                        BuildStatus.FAILED
                    } else {
                        BuildStatus.values()[buildStatusOrd.coerceAtMost(BuildStatus.values().size - 1)]
                    }
                } else {
                    null
                },
                latestBuildTaskName = summary["LATEST_TASK_NAME"] as String?,
                lock = PipelineRunLockType.checkLock(summary["RUN_LOCK_TYPE"] as Int?),
                runningBuildCount = summary["RUNNING_COUNT"] as Int? ?: 0
            )
        } finally {
            LogUtils.printCostTimeWE(watcher, warnThreshold = 50, errorThreshold = 200)
        }
    }
}
