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

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.constant.KEY_CHANNEL
import com.tencent.devops.common.api.constant.KEY_END_TIME
import com.tencent.devops.common.api.constant.KEY_START_TIME
import com.tencent.devops.common.api.pojo.AtomMonitorData
import com.tencent.devops.common.api.pojo.BuildEndContainerMetricsData
import com.tencent.devops.common.api.pojo.BuildEndPipelineMetricsData
import com.tencent.devops.common.api.pojo.BuildEndStageMetricsData
import com.tencent.devops.common.api.pojo.BuildEndTaskMetricsData
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.OrganizationDetailInfo
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.measure.AtomMonitorReportBroadCastEvent
import com.tencent.devops.common.event.pojo.measure.BuildEndMetricsBroadCastEvent
import com.tencent.devops.common.event.pojo.measure.MeasureRequest
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildTaskFinishBroadCastEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.measure.pojo.ElementMeasureData
import com.tencent.devops.measure.pojo.PipelineBuildData
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.ProjectCacheService
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.store.pojo.common.KEY_VERSION
import org.apache.lucene.util.RamUsageEstimator
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.Date

@Suppress("ALL", "UNUSED")
class MeasureServiceImpl constructor(
    private val projectCacheService: ProjectCacheService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildVariableService: BuildVariableService,
    private val templateService: TemplateService,
    private val pipelineInfoService: PipelineInfoService,
    private val redisOperation: RedisOperation,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val atomMonitorSwitch: String,
    private val maxMonitorDataSize: String = "1677216",
    private val measureEventDispatcher: MeasureEventDispatcher
) : MeasureService {

    override fun postPipelineData(
        projectId: String,
        pipelineId: String,
        buildId: String,
        startTime: Long,
        startType: String,
        username: String,
        buildStatus: BuildStatus,
        buildNum: Int,
        model: Model?,
        errorInfoList: String?
    ) {
        try {
            if (model == null) {
                return
            }

            val parentPipelineId = buildVariableService.getVariable(
                projectId = projectId,
                buildId = buildId,
                varName = PIPELINE_START_PARENT_PIPELINE_ID
            ) ?: ""
            val parentBuildId = buildVariableService.getVariable(
                projectId = projectId,
                buildId = buildId,
                varName = PIPELINE_START_PARENT_BUILD_ID
            ) ?: ""
            val metaInfo = mapOf(
                "parentPipelineId" to parentPipelineId,
                "parentBuildId" to parentBuildId
            )

            val data = PipelineBuildData(
                projectId = projectId,
                pipelineId = pipelineId,
                templateId = templateService.getTemplateIdByPipeline(projectId, pipelineId) ?: "",
                buildId = buildId,
                beginTime = startTime,
                endTime = System.currentTimeMillis(),
                startType = StartType.toStartType(startType),
                buildUser = username,
                isParallel = false,
                buildResult = buildStatus,
                pipeline = JsonUtil.getObjectMapper().writeValueAsString(model),
                buildNum = buildNum,
                metaInfo = metaInfo,
                errorInfoList = errorInfoList
            )

            val requestBody = JsonUtil.toJson(data, formatted = false)
            measureEventDispatcher.dispatch(
                MeasureRequest(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    type = MeasureRequest.MeasureType.PIPELINE,
                    request = requestBody
                )
            )
        } catch (ignored: Throwable) {
            logger.warn("MK_postPipelineData|$buildId|message: ${ignored.message}")
        }
    }

    override fun postCancelData(projectId: String, pipelineId: String, buildId: String, userId: String) {
        try {
            val tasks = pipelineTaskService.getAllBuildTask(projectId, buildId)
            if (tasks.isEmpty()) {
                return
            }
            tasks.forEach { task ->
                if (task.status.isRunning()) {
                    val tStartTime = task.startTime?.timestampmilli() ?: 0
                    postTaskData(
                        task = task, startTime = tStartTime, status = BuildStatus.CANCELED, type = task.taskType
                    )
                }
            }
        } catch (ignored: Exception) {
            logger.warn("MK_postCancelData|$buildId|message: ${ignored.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun postTaskData(
        task: PipelineBuildTask,
        startTime: Long,
        status: BuildStatus,
        type: String,
        monitorDataMap: Map<String, Any>?,
        errorType: String?,
        errorCode: Int?,
        errorMsg: String?
    ) {
        try {
            val taskId = task.taskId
            val buildId = task.buildId
            val projectId = task.projectId
            val pipelineId = task.pipelineId
            val name = task.taskName
            val userId = task.starter
            val vmSeqId = task.containerId
            val taskParams = task.taskParams
            val atomCode = task.atomCode ?: taskParams["atomCode"] as String? ?: task.taskType

            val elementMeasureData = ElementMeasureData(
                id = taskId,
                name = name,
                pipelineId = pipelineId,
                projectId = projectId,
                buildId = buildId,
                atomCode = atomCode,
                status = status,
                beginTime = startTime,
                endTime = System.currentTimeMillis(),
                type = type,
                errorCode = errorCode,
                errorType = errorType,
                errorMsg = errorMsg
            )

            val requestBody = ObjectMapper().writeValueAsString(elementMeasureData)

            measureEventDispatcher.dispatch(
                MeasureRequest(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    type = MeasureRequest.MeasureType.TASK,
                    request = requestBody
                )
            )

            pipelineEventDispatcher.dispatch(
                PipelineBuildTaskFinishBroadCastEvent(
                    source = "build-element-$taskId",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    buildId = buildId,
                    taskId = taskId,
                    errorType = errorType,
                    errorCode = errorCode,
                    errorMsg = errorMsg
                )
            )

            if (monitorDataMap == null || !checkAtomMonitorSwitch()) {
                return
            }

            // 上报插件监控数据
            if (getSpecReportAtoms().contains(atomCode)) {
                // 上报开关关闭或者不在指定上报插件范围内则无需上报监控数据
                return
            }

            val monitorDataSize = RamUsageEstimator.sizeOfMap(monitorDataMap)
            if (monitorDataSize > maxMonitorDataSize.toLong()) {
                // 上报的监控对象大小大于规定的值则不上报
                logger.info("[$buildId]|atom=$atomCode|dataSize=$monitorDataSize|maxDataSize=$maxMonitorDataSize")
                return
            }

            val version = taskParams[KEY_VERSION] as String? ?: ""
            val monitorStartTime = monitorDataMap[KEY_START_TIME]?.toString()?.toLong()
                ?: task.startTime?.timestampmilli()
            val monitorEndTime = monitorDataMap[KEY_END_TIME]?.toString()?.toLong()
                ?: task.endTime?.timestampmilli()
            val project = projectCacheService.getProject(projectId)

            val extData: Map<String, Any>? = try {
                monitorDataMap["extData"] as? Map<String, Any>
            } catch (ignored: Exception) {
                null
            }

            val atomMonitorData = AtomMonitorData(
                errorCode = errorCode ?: -1,
                errorMsg = errorMsg,
                errorType = errorType,
                atomCode = atomCode,
                version = version,
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                startTime = monitorStartTime,
                endTime = monitorEndTime,
                elapseTime = (monitorEndTime ?: 0) - (monitorStartTime ?: 0),
                channel = monitorDataMap[KEY_CHANNEL] as? String,
                starter = userId,
                organizationDetailInfo = OrganizationDetailInfo(
                    bgId = project?.bgId?.toInt(),
                    bgName = project?.bgName,
                    centerId = project?.centerId?.toInt(),
                    centerName = project?.centerName,
                    deptId = project?.deptId?.toInt(),
                    deptName = project?.deptName
                ),
                extData = extData
            )
            measureEventDispatcher.dispatch(
                AtomMonitorReportBroadCastEvent(
                    pipelineId = pipelineId,
                    projectId = projectId,
                    buildId = buildId,
                    monitorData = atomMonitorData,
                )
            )
        } catch (ignored: Throwable) { // MK = Monitor Key
            logger.warn("MK_postTaskData|${task.buildId}|message: ${ignored.message}")
        }
    }

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
            if (checkMetricsReportCondition(stageStatus)) {
                return@nextStage
            }
            val containerMetricsDatas = mutableListOf<BuildEndContainerMetricsData>()
            stage.containers.forEach nextContainer@{ container ->
                // 判断container是否执行过,未执行过的container无需上报数据
                val containerStatus = container.status
                if (checkMetricsReportCondition(containerStatus)) {
                    return@nextContainer
                }
                val taskMetricsDatas = mutableListOf<BuildEndTaskMetricsData>()
                val containerAtomCodes = mutableListOf<String>()
                container.elements.forEach nextElement@{ element ->
                    // 判断插件是否执行过,未执行过的插件无需上报数据
                    val elementStatus = element.status
                    if (checkMetricsReportCondition(elementStatus)) {
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

    private fun getSpecReportAtoms(): List<String> = try {
        cache.get("specReportAtoms")?.split(",") ?: emptyList()
    } catch (ignored: Exception) {
        emptyList()
    }

    private fun checkAtomMonitorSwitch(): Boolean {
        return try {
            cache.get("atomMonitorSwitch")?.toBoolean() ?: true
        } catch (ignored: Exception) {
            atomMonitorSwitch.toBoolean()
        }
    }

    private val cache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE)
        .expireAfterWrite(CACHE_TIME, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, String>() {
            override fun load(key: String) = redisOperation.get(key)
        })

    companion object {
        private const val CACHE_SIZE = 10L
        private const val CACHE_TIME = 1L
        private val logger = LoggerFactory.getLogger(MeasureService::class.java)
    }
}
