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

package com.tencent.devops.process.engine.service.measure

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.measure.MeasureRequest
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildTaskFinishBroadCastEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.measure.pojo.ElementMeasureData
import com.tencent.devops.measure.pojo.PipelineBuildData
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.template.TemplateService
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Suppress("UNUSED")
class MeasureServiceImpl constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildVarDao: PipelineBuildVarDao,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val templateService: TemplateService,
    private val measureEventDispatcher: MeasureEventDispatcher,
    private val pipelineEventDispatcher: PipelineEventDispatcher
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
                logger.warn("The pipeline.json is not exist of pipeline($pipelineId)")
                return
            }

            val json = JsonUtil.getObjectMapper().writeValueAsString(model)

            val variable = pipelineBuildVarDao.getVars(dslContext, buildId)
            val metaInfo = mapOf(
                "parentPipelineId" to (variable[PIPELINE_START_PARENT_PIPELINE_ID] ?: ""),
                "parentBuildId" to (variable[PIPELINE_START_PARENT_BUILD_ID] ?: "")
            )

            val data = PipelineBuildData(
                projectId = projectId,
                pipelineId = pipelineId,
                templateId = templateService.listPipelineTemplate(setOf(pipelineId))?.firstOrNull()?.templateId ?: "",
                buildId = buildId,
                beginTime = startTime,
                endTime = System.currentTimeMillis(),
                startType = StartType.toStartType(startType),
                buildUser = username,
                isParallel = false,
                buildResult = buildStatus,
                pipeline = json,
                buildNum = buildNum,
                metaInfo = metaInfo,
                errorInfoList = errorInfoList
            )

            val requestBody = objectMapper.writeValueAsString(data)
            measureEventDispatcher.dispatch(
                MeasureRequest(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    type = MeasureRequest.MeasureType.PIPELINE,
                    request = requestBody
                )
            )
        } catch (t: Throwable) {
            logger.warn("Fail to post the pipeline measure data of build($buildId)", t)
        }
    }

    override fun postCancelData(projectId: String, pipelineId: String, buildId: String, userId: String) {
        try {
            val tasks = pipelineRuntimeService.getAllBuildTask(buildId)
            if (tasks.isEmpty()) {
                return
            }
            tasks.forEach { task ->
                with(task) {
                    if (BuildStatus.isRunning(status)) {
                        val tStartTime = startTime?.timestampmilli() ?: 0
                        postTaskData(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            taskId = taskId,
                            atomCode = atomCode ?: taskParams["atomCode"] as String? ?: taskType,
                            name = taskName,
                            buildId = buildId,
                            startTime = tStartTime,
                            status = BuildStatus.CANCELED,
                            type = taskType,
                            executeCount = executeCount,
                            userId = userId
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("[$buildId]| Fail to post the cancel measure event", e)
        }
    }

    override fun postTaskData(
        projectId: String,
        pipelineId: String,
        taskId: String,
        atomCode: String,
        name: String,
        buildId: String,
        startTime: Long,
        status: BuildStatus,
        type: String,
        executeCount: Int?,
        extraInfo: Map<String, Any>?,
        errorType: String?,
        errorCode: Int?,
        errorMsg: String?,
        userId: String
    ) {
        try {

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
                extraInfo = if (extraInfo != null && extraInfo.isNotEmpty()) {
                    val extraInfoStr = ObjectMapper().writeValueAsString(extraInfo)
                    extraInfoStr
                } else null,
                errorCode = errorCode,
                errorType = errorType,
                errorMsg = errorMsg
            )

            val requestBody = ObjectMapper().writeValueAsString(elementMeasureData)
            logger.info("[$buildId]| add the element data, request data: $elementMeasureData")
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
        } catch (e: Throwable) {
            logger.error("Fail to add the element data, $e")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MeasureService::class.java)
    }
}
