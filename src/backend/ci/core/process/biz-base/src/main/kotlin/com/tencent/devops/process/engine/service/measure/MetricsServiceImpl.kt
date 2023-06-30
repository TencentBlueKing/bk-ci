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

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.event.pojo.measure.BuildEndContainerMetricsData
import com.tencent.devops.common.event.pojo.measure.BuildEndMetricsBroadCastEvent
import com.tencent.devops.common.event.pojo.measure.BuildEndPipelineMetricsData
import com.tencent.devops.common.event.pojo.measure.BuildEndStageMetricsData
import com.tencent.devops.common.event.pojo.measure.BuildEndTaskMetricsData
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.dao.PipelineStageTagDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import java.util.Date
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Service
@RefreshScope
class MetricsServiceImpl constructor(
    private val pipelineInfoDao: PipelineInfoDao,
    private val measureEventDispatcher: MeasureEventDispatcher,
    private val dslContext: DSLContext,
    private val pipelineStageTagDao: PipelineStageTagDao
) : MetricsService {

    @Value("\${metrics.allowReportProjectConfig:}")
    val allowReportProjectConfig: String = ""

    private val stageTagCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(6, TimeUnit.HOURS)
        .build<String, String>()

    private fun getStageTagName(stageTagId: String): String? {
        var stageTagName = stageTagCache.getIfPresent(stageTagId)
        if (stageTagName == null) {
            stageTagName = pipelineStageTagDao.getStageTag(dslContext, stageTagId)?.stageTagName
        }
        stageTagName?.let { stageTagCache.put(stageTagId, it) }
        return stageTagName
    }

    override fun postMetricsData(buildInfo: BuildInfo, model: Model) {
        if (!ChannelCode.webChannel(buildInfo.channelCode)) {
            // 页面不可见的构建无需上报数据
            return
        }
        val projectId = buildInfo.projectId
        // 判断该项目是否允许进行数据上报
        if (allowReportProjectConfig.isNotBlank() &&
            !allowReportProjectConfig.split(",").contains(projectId)) {
            return
        }
        if (buildInfo.endTime == null) {
            logger.warn("Warning: The post of metrics data is abnormal, build info end time is null.")
            return
        }
        val pipelineId = buildInfo.pipelineId
        val buildId = buildInfo.buildId
        val pipelineName = pipelineInfoDao.getPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            delete = null
        )?.pipelineName
        val webhookInfo = buildInfo.webhookInfo
        val stageMetricsDatas = mutableListOf<BuildEndStageMetricsData>()
        handleMetricsData(model, stageMetricsDatas)
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
            endTime = DateTimeUtil.formatMilliTime(buildInfo.endTime!!, DateTimeUtil.YYYY_MM_DD_HH_MM_SS),
            costTime = if (buildInfo.queueTime == 0L) {
                buildInfo.queueTime
            } else {
                buildInfo.endTime!! - buildInfo.queueTime
            },
            successFlag = buildInfo.status.isSuccess(),
            errorInfos = buildInfo.errorInfoList,
            stages = stageMetricsDatas,
            channelCode = buildInfo.channelCode.name
        )
        measureEventDispatcher.dispatch(
            BuildEndMetricsBroadCastEvent(
                pipelineId = pipelineId,
                projectId = projectId,
                buildId = buildId,
                buildEndPipelineMetricsData = buildEndPipelineMetricsData
            )
        )
    }

    private fun handleMetricsData(model: Model, stageMetricsDatas: MutableList<BuildEndStageMetricsData>) {
        model.stages.forEachIndexed nextStage@{ stageIndex, stage ->
            // 判断stage是否执行过,未执行过的stage无需上报数据
            val stageStatus = stage.status
            if (!checkMetricsReportCondition(stageStatus)) {
                return@nextStage
            }
            val containerMetricsDatas = mutableListOf<BuildEndContainerMetricsData>()
            handleContainer(stage = stage, stageIndex = stageIndex, containerMetricsDatas = containerMetricsDatas)
            var stageTagNames: MutableList<String>? = null
            stage.tag?.forEach { stageTagId ->
                if (stageTagNames == null) {
                    stageTagNames = mutableListOf()
                }
                val stageTagName = getStageTagName(stageTagId)
                stageTagName?.let { stageTagNames?.add(stageTagName) }
            }
            stageMetricsDatas.add(
                BuildEndStageMetricsData(
                    stageId = stage.id ?: "",
                    stageTagNames = stageTagNames,
                    successFlag = BuildStatus.valueOf(stageStatus!!).isSuccess(),
                    costTime = stage.elapsed ?: 0L,
                    containers = containerMetricsDatas
                )
            )
        }
    }

    private fun handleContainer(
        stage: Stage,
        stageIndex: Int,
        containerMetricsDatas: MutableList<BuildEndContainerMetricsData>
    ) {
        stage.containers.forEachIndexed nextContainer@{ containerIndex, container ->
            val groupContainers = container.fetchGroupContainers()
            if (!groupContainers.isNullOrEmpty()) {
                groupContainers.forEachIndexed { groupContainerIndex, groupContainer ->
                    val groupContainerStatus = groupContainer.status
                    if (!checkMetricsReportCondition(groupContainerStatus) || groupContainer is TriggerContainer) {
                        return@nextContainer
                    }
                    doContainerBus(
                        container = groupContainer,
                        stageIndex = stageIndex,
                        containerIndex = containerIndex,
                        groupContainerIndex = groupContainerIndex,
                        containerMetricsDatas = containerMetricsDatas,
                        containerStatus = groupContainerStatus
                    )
                }
            } else {
                // 判断container是否执行过,未执行过的container无需上报数据
                val containerStatus = container.status
                if (!checkMetricsReportCondition(containerStatus) || container is TriggerContainer) {
                    return@nextContainer
                }
                doContainerBus(
                    container = container,
                    stageIndex = stageIndex,
                    containerIndex = containerIndex,
                    groupContainerIndex = null,
                    containerMetricsDatas = containerMetricsDatas,
                    containerStatus = containerStatus
                )
            }
        }
    }

    private fun doContainerBus(
        container: Container,
        stageIndex: Int,
        containerIndex: Int,
        groupContainerIndex: Int? = null,
        containerMetricsDatas: MutableList<BuildEndContainerMetricsData>,
        containerStatus: String?
    ) {
        val taskMetricsDatas = mutableListOf<BuildEndTaskMetricsData>()
        val containerAtomCodes = mutableListOf<String>()
        handleElement(
            container = container,
            stageIndex = stageIndex,
            groupContainerIndex = groupContainerIndex,
            containerIndex = containerIndex,
            containerAtomCodes = containerAtomCodes,
            taskMetricsDatas = taskMetricsDatas
        )
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

    private fun handleElement(
        container: Container,
        stageIndex: Int,
        containerIndex: Int,
        groupContainerIndex: Int? = null,
        containerAtomCodes: MutableList<String>,
        taskMetricsDatas: MutableList<BuildEndTaskMetricsData>
    ) {
        container.elements.forEachIndexed nextElement@{ elementIndex, element ->
            // 判断插件是否执行过,未执行过的插件无需上报数据
            val elementStatus = element.status
            if (!checkMetricsReportCondition(elementStatus)) {
                return@nextElement
            }
            containerAtomCodes.add(element.getAtomCode())
            val elementPosition = if (groupContainerIndex == null) {
                "$stageIndex-$containerIndex-$elementIndex"
            } else {
                // 存在矩阵的情况，task在model中的位置有4级
                "$stageIndex-$containerIndex-$groupContainerIndex-$elementIndex"
            }
            addTaskMetricsData(
                taskMetricsDatas = taskMetricsDatas,
                element = element,
                elementPosition = elementPosition,
                elementStatus = elementStatus
            )
        }
    }

    private fun addTaskMetricsData(
        taskMetricsDatas: MutableList<BuildEndTaskMetricsData>,
        element: Element,
        elementPosition: String,
        elementStatus: String?
    ) {
        taskMetricsDatas.add(
            BuildEndTaskMetricsData(
                taskId = element.id ?: "",
                atomName = element.atomName ?: element.name,
                atomCode = element.getAtomCode(),
                atomPosition = elementPosition,
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
                // TODO 新构建详情中不带有错误信息，需要读取build表
                errorType = element.errorType?.let { ErrorType.getErrorType(it)?.num },
                errorCode = element.errorCode,
                errorMsg = element.errorMsg
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

    companion object {
        private val logger = LoggerFactory.getLogger(MetricsServiceImpl::class.java)
    }
}
