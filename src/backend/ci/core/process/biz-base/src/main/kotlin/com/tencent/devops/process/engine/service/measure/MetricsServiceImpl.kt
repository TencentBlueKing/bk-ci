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

package com.tencent.devops.process.engine.service.measure

import com.tencent.devops.common.api.pojo.BuildEndContainerMetricsData
import com.tencent.devops.common.api.pojo.BuildEndPipelineMetricsData
import com.tencent.devops.common.api.pojo.BuildEndStageMetricsData
import com.tencent.devops.common.api.pojo.BuildEndTaskMetricsData
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.event.pojo.measure.BuildEndMetricsBroadCastEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import org.springframework.stereotype.Service
import java.util.Date

@Service
class MetricsServiceImpl constructor(
    private val pipelineInfoService: PipelineInfoService,
    private val measureEventDispatcher: MeasureEventDispatcher
) : MetricsService {

    override fun postMetricsData(buildInfo: BuildInfo, model: Model) {
        val projectId = buildInfo.projectId
        val pipelineId = buildInfo.pipelineId
        val buildId = buildInfo.buildId
        val pipelineName = pipelineInfoService.getPipelineName(projectId, pipelineId)
        val webhookInfo = buildInfo.webhookInfo
        val stageMetricsDatas = mutableListOf<BuildEndStageMetricsData>()
        model.stages.forEach nextStage@{ stage ->
            // 判断stage是否执行过,未执行过的stage无需上报数据
            val stageStatus = stage.status
            if (!checkMetricsReportCondition(stageStatus)) {
                return@nextStage
            }
            val containerMetricsDatas = mutableListOf<BuildEndContainerMetricsData>()
            stage.containers.forEach nextContainer@{ container ->
                // 判断container是否执行过,未执行过的container无需上报数据
                val containerStatus = container.status
                if (!checkMetricsReportCondition(containerStatus)) {
                    return@nextContainer
                }
                val taskMetricsDatas = mutableListOf<BuildEndTaskMetricsData>()
                val containerAtomCodes = mutableListOf<String>()
                container.elements.forEach nextElement@{ element ->
                    // 判断插件是否执行过,未执行过的插件无需上报数据
                    val elementStatus = element.status
                    if (!checkMetricsReportCondition(elementStatus)) {
                        return@nextElement
                    }
                    containerAtomCodes.add(element.getAtomCode())
                    taskMetricsDatas.add(
                        BuildEndTaskMetricsData(
                            taskId = element.id ?: "",
                            atomName = element.atomName ?: element.name,
                            atomCode = element.getAtomCode(),
                            classifyCode = element.classifyCode ?: "",
                            classifyName = element.classifyName ?: "",
                            startTime = element.startEpoch?.let {
                                DateTimeUtil.formatMilliTime(it, DateTimeUtil.YYYY_MM_DD_HH_MM_SS)
                            },
                            endTime = element.startEpoch?.let {
                                DateTimeUtil.formatMilliTime(
                                    time = it + (element.elapsed ?: 0L),
                                    format = DateTimeUtil.YYYY_MM_DD_HH_MM_SS
                                )
                            },
                            costTime = element.elapsed ?: 0L,
                            successFlag = BuildStatus.valueOf(elementStatus!!).isSuccess(),
                            errorType = element.errorType?.let { ErrorType.getErrorType(it)?.num },
                            errorCode = element.errorCode,
                            errorMsg = element.errorMsg
                        )
                    )
                }
                containerMetricsDatas.add(
                    BuildEndContainerMetricsData(
                        containerId = container.containerId ?: "",
                        successFlag = BuildStatus.valueOf(containerStatus!!).isSuccess(),
                        costTime = (container.systemElapsed ?: 0L) + (container.elementElapsed ?: 0L),
                        atomCodes = containerAtomCodes,
                        tasks = taskMetricsDatas
                    )
                )
            }
            stageMetricsDatas.add(
                BuildEndStageMetricsData(
                    stageId = stage.id ?: "",
                    stageTagNames = stage.tag,
                    successFlag = BuildStatus.valueOf(stageStatus!!).isSuccess(),
                    costTime = stage.elapsed ?: 0L,
                    containers = containerMetricsDatas
                )
            )
        }
        val buildEndPipelineMetricsData = BuildEndPipelineMetricsData(
            statisticsTime = DateTimeUtil.formatDate(Date(), DateTimeUtil.YYYY_MM_DD),
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = pipelineName ?: "",
            buildId = buildId,
            buildNum = buildInfo.buildNum,
            repoUrl = webhookInfo?.webhookRepoUrl,
            branch = webhookInfo?.webhookBranch,
            startUser = buildInfo.startUser,
            startTime = buildInfo.startTime?.let { DateTimeUtil.formatMilliTime(it, DateTimeUtil.YYYY_MM_DD_HH_MM_SS) },
            endTime = buildInfo.endTime?.let { DateTimeUtil.formatMilliTime(it, DateTimeUtil.YYYY_MM_DD_HH_MM_SS) },
            costTime = (buildInfo.endTime ?: 0L) - (buildInfo.startTime ?: 0L),
            successFlag = buildInfo.status.isSuccess(),
            errorType = buildInfo.errorType,
            errorCode = buildInfo.errorCode,
            errorMsg = buildInfo.errorMsg,
            stages = stageMetricsDatas
        )
        measureEventDispatcher.dispatch(
            BuildEndMetricsBroadCastEvent(
                pipelineId = pipelineId,
                projectId = projectId,
                buildId = buildId,
                buildEndPipelineMetricsData = buildEndPipelineMetricsData,
            )
        )
    }

    /**
     * 根据构建状态判断是否需要上报metrics数据
     * @param status 构建状态
     * @return 布尔值
     */
    private fun checkMetricsReportCondition(status: String?): Boolean {
        if (status.isNullOrBlank()) {
            return false
        }
        val buildStatus = BuildStatus.valueOf(status)
        val invalidFinishStatusList = listOf(BuildStatus.UNEXEC, BuildStatus.SKIP, BuildStatus.QUOTA_FAILED)
        return buildStatus.isFinish() && !invalidFinishStatusList.contains(buildStatus)
    }
}
