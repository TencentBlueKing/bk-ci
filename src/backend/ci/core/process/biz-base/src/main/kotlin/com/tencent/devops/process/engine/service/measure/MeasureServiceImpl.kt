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

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.constant.KEY_CHANNEL
import com.tencent.devops.common.api.constant.KEY_END_TIME
import com.tencent.devops.common.api.constant.KEY_START_TIME
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.pojo.AtomMonitorData
import com.tencent.devops.common.api.pojo.OrganizationDetailInfo
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.measure.AtomMonitorReportBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildTaskFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.ProjectCacheService
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import com.tencent.devops.process.template.service.TemplateService
import org.apache.lucene.util.RamUsageEstimator
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Suppress("ALL", "UNUSED")
@Service
class MeasureServiceImpl : MeasureService {

    @Autowired
    lateinit var projectCacheService: ProjectCacheService
    @Autowired
    lateinit var pipelineTaskService: PipelineTaskService
    @Autowired
    lateinit var buildVariableService: BuildVariableService
    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var templateService: TemplateService
    @Autowired
    lateinit var pipelineInfoService: PipelineInfoService
    @Autowired
    lateinit var redisOperation: RedisOperation
    @Autowired
    lateinit var pipelineEventDispatcher: PipelineEventDispatcher
    @Autowired
    lateinit var measureEventDispatcher: MeasureEventDispatcher

    @Value("\${build.atomMonitorData.report.switch:false}")
    private val atomMonitorSwitch: String = "false"

    @Value("\${build.atomMonitorData.report.maxMonitorDataSize:1677216}")
    private val maxMonitorDataSize: String = "1677216"

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
            val userId = task.starter
            val vmSeqId = task.containerId
            val taskParams = task.taskParams
            val atomCode = task.atomCode ?: taskParams["atomCode"] as String? ?: task.taskType
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
                    monitorData = atomMonitorData
                )
            )
        } catch (ignored: Throwable) { // MK = Monitor Key
            logger.warn("MK_postTaskData|${task.buildId}|message: ${ignored.message}")
        }
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
