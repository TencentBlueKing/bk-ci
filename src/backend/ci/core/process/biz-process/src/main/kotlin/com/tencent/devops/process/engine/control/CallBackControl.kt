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

package com.tencent.devops.process.engine.control

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ProjectPipelineCallbackStatus
import com.tencent.devops.common.pipeline.event.BuildEvent
import com.tencent.devops.common.pipeline.event.CallBackData
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.PipelineEvent
import com.tencent.devops.common.pipeline.event.SimpleJob
import com.tencent.devops.common.pipeline.event.SimpleModel
import com.tencent.devops.common.pipeline.event.SimpleStage
import com.tencent.devops.common.pipeline.event.SimpleTask
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import com.tencent.devops.process.pojo.CallBackHeader
import com.tencent.devops.process.pojo.ProjectPipelineCallBack
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

/**
 *  步骤控制器
 * @version 1.0
 */
@Service
class CallBackControl @Autowired constructor(
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val projectPipelineCallBackService: ProjectPipelineCallBackService
) {

    fun pipelineCreateEvent(projectId: String, pipelineId: String) {
        callBackPipelineEvent(projectId, pipelineId, CallBackEvent.CREATE_PIPELINE)
    }

    fun pipelineDeleteEvent(projectId: String, pipelineId: String) {
        callBackPipelineEvent(projectId, pipelineId, CallBackEvent.DELETE_PIPELINE)
    }

    fun pipelineUpdateEvent(projectId: String, pipelineId: String) {
        callBackPipelineEvent(projectId, pipelineId, CallBackEvent.UPDATE_PIPELINE)
    }

    private fun callBackPipelineEvent(projectId: String, pipelineId: String, callBackEvent: CallBackEvent) {
        logger.info("$projectId|$pipelineId|$callBackEvent|callback pipeline event")
        val list = projectPipelineCallBackService.listProjectCallBack(
            projectId = projectId,
            events = callBackEvent.name
        )
        if (list.isEmpty()) {
            logger.info("[$pipelineId]|[$callBackEvent]| no callback")
            return
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            pipelineId = pipelineId,
            delete = null
        ) ?: return

        val pipelineEvent = PipelineEvent(
            pipelineId = pipelineInfo.pipelineId,
            pipelineName = pipelineInfo.pipelineName,
            userId = pipelineInfo.lastModifyUser,
            updateTime = pipelineInfo.updateTime
        )

        sendToCallBack(CallBackData(event = callBackEvent, data = pipelineEvent), list)
    }

    fun callBackBuildEvent(event: PipelineBuildStatusBroadCastEvent) {
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        val buildId = event.buildId

        val callBackEvent =
            if (event.taskId.isNullOrBlank()) {
                if (event.actionType == ActionType.START) {
                    CallBackEvent.BUILD_START
                } else {
                    CallBackEvent.BUILD_END
                }
            } else {
                if (event.actionType == ActionType.START) {
                    CallBackEvent.BUILD_TASK_START
                } else {
                    CallBackEvent.BUILD_TASK_END
                }
            }

        logger.info("$projectId|$pipelineId|$buildId|${callBackEvent.name}|callback build event")
        val list = projectPipelineCallBackService.listProjectCallBack(
            projectId = projectId,
            events = callBackEvent.name
        )
        if (list.isEmpty()) {
            logger.info("[$buildId]|[$pipelineId]|[$callBackEvent]| no callback")
            return
        }
        val modelDetail = pipelineBuildDetailService.get(buildId = buildId, refreshStatus = false) ?: return

        val stages = parseModel(modelDetail.model)

        val buildEvent = BuildEvent(
            buildId = buildId,
            pipelineId = modelDetail.pipelineId,
            pipelineName = modelDetail.pipelineName,
            userId = modelDetail.userId,
            status = modelDetail.status,
            startTime = modelDetail.startTime,
            endTime = modelDetail.endTime ?: 0,
            model = SimpleModel(stages),
            projectId = projectId,
            trigger = modelDetail.trigger
        )

        sendToCallBack(CallBackData(event = callBackEvent, data = buildEvent), list)
    }

    private fun <T> sendToCallBack(callBackData: CallBackData<T>, list: List<ProjectPipelineCallBack>) {

        val requestBody = ObjectMapper().writeValueAsString(callBackData)
        executors.submit {
            list.forEach {
                try {
                    logger.info("${it.projectId}|${it.callBackUrl}|${it.events}|send to callback")
                    if (it.callBackUrl.isBlank()) {
                        logger.warn("[${it.projectId}]| call back url is empty!")
                        return@forEach
                    }
                    send(callBack = it, requestBody = requestBody, executeCount = 1)
                } catch (e: Exception) {
                    logger.error("${it.projectId}|${it.callBackUrl}|${it.events}|send to callback error", e)
                }
            }
        }
    }

    private fun send(callBack: ProjectPipelineCallBack, requestBody: String, executeCount: Int = 1) {
        if (executeCount > 3) {
            logger.warn("[${callBack.projectId}]|CALL_BACK|url=${callBack.callBackUrl}| retry fail!")
            return
        }
        val request = Request.Builder()
            .url(callBack.callBackUrl)
            .header("X-DEVOPS-WEBHOOK-TOKEN", callBack.secretToken ?: "NONE")
            .header(TraceTag.TRACE_HEADER_DEVOPS_BIZID, TraceTag.buildBiz())
            .post(RequestBody.create(JSON, requestBody))
            .build()

        val startTime = System.currentTimeMillis()
        var responseCode: Int? = null
        var responseBody: String? = null
        var errorMsg: String? = null
        var status = ProjectPipelineCallbackStatus.SUCCESS
        try {
            OkhttpUtils.doHttp(request).use { response ->
                if (response.code() != 200) {
                    logger.warn("[${callBack.projectId}]|CALL_BACK|url=${callBack.callBackUrl}| code=${response.code()}")

                    Thread.sleep(executeCount * executeCount * 1000L)
                    send(callBack, requestBody, executeCount + 1)
                } else {
                    logger.info("[${callBack.projectId}]|CALL_BACK|url=${callBack.callBackUrl}| code=${response.code()}")
                }
                responseCode = response.code()
                responseBody = response.body()?.string()
                errorMsg = response.message()
            }
        } catch (e: Exception) {
            logger.error("[${callBack.projectId}]|CALL_BACK|url=${callBack.callBackUrl}|${callBack.events}", e)
            errorMsg = e.message
            status = ProjectPipelineCallbackStatus.FAILED
        } finally {
            saveHistory(
                callBack = callBack,
                requestHeaders = request.headers().names().map { CallBackHeader(name = it, value = request.header(it) ?: "") },
                requestBody = requestBody,
                responseCode = responseCode,
                responseBody = responseBody,
                status = status.name,
                errorMsg = errorMsg,
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )
        }
    }

    private fun saveHistory(
        callBack: ProjectPipelineCallBack,
        requestHeaders: List<CallBackHeader>,
        requestBody: String,
        responseCode: Int?,
        responseBody: String?,
        status: String,
        errorMsg: String?,
        startTime: Long,
        endTime: Long
    ) {
        try {
            projectPipelineCallBackService.createHistory(ProjectPipelineCallBackHistory(
                projectId = callBack.projectId,
                callBackUrl = callBack.callBackUrl,
                events = callBack.events,
                status = status,
                errorMsg = errorMsg,
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseCode = responseCode,
                responseBody = responseBody,
                startTime = startTime,
                endTime = endTime
            ))
        } catch (e: Throwable) {
            logger.error("[${callBack.projectId}]|[${callBack.callBackUrl}]|[${callBack.events}]|save callback history fail", e)
        }
    }

    private fun parseModel(model: Model): List<SimpleStage> {
        val stages = mutableListOf<SimpleStage>()
        model.stages.forEachIndexed { pos, s ->
            val jobs = mutableListOf<SimpleJob>()
            val stage = SimpleStage(stageName = "Stage-${pos + 1}", status = "", jobs = jobs)
            stages.add(stage)

            val triple = parseJob(s, jobs)
            val stageStartTimeMills = triple.first
            val stageEndTimeMills = triple.second
            val stageStatus = triple.third

            if (stageStartTimeMills > 0) {
                stage.startTime = stageStartTimeMills
            }

            if (stageEndTimeMills > 0) {
                stage.endTime = stageEndTimeMills
            }

            stage.status = stageStatus.name
        }
        return stages
    }

    private fun parseJob(s: Stage, jobs: MutableList<SimpleJob>): Triple<Long, Long, BuildStatus> {
        var stageStartTimeMills = 0L
        var stageEndTimeMills = -1L
        var stageStatus = BuildStatus.QUEUE
        s.containers.forEach { c ->
            val jobStatus = BuildStatus.parse(c.status)
            val tasks = mutableListOf<SimpleTask>()
            val jobStartTimeMills = c.startEpoch ?: 0L
            val jobEndTimeMills = if (BuildStatus.isFinish(jobStatus)) {
                jobStartTimeMills + (c.elementElapsed ?: 0) + (c.systemElapsed ?: 0)
            } else {
                0
            }

            if (stageStartTimeMills > jobStartTimeMills) {
                stageStartTimeMills = jobStartTimeMills
            }

            if (jobEndTimeMills == 0L) {
                stageStatus = jobStatus
                stageEndTimeMills = jobEndTimeMills
            } else if (stageEndTimeMills > 0 && stageEndTimeMills < jobEndTimeMills) {
                stageEndTimeMills = jobEndTimeMills
                if (BuildStatus.isFailure(jobStatus)) {
                    stageStatus = jobStatus
                }
            }

            jobs.add(
                SimpleJob(
                    jobName = c.name,
                    status = jobStatus.name,
                    startTime = jobStartTimeMills,
                    endTime = jobEndTimeMills,
                    tasks = tasks
                )
            )

            parseTask(c, tasks)
        }

        if (stageEndTimeMills > 0 && !BuildStatus.isFinish(stageStatus)) {
            stageStatus = BuildStatus.SUCCEED
        }
        return Triple(stageStartTimeMills, stageEndTimeMills, stageStatus)
    }

    private fun parseTask(c: Container, tasks: MutableList<SimpleTask>) {
        c.elements.forEach { e ->
            val taskStartTimeMills = e.startEpoch ?: 0
            val taskStatus = BuildStatus.parse(e.status)
            val taskEndTimeMills = if (BuildStatus.isFinish(taskStatus)) {
                taskStartTimeMills + (e.elapsed ?: 0)
            } else {
                0
            }
            tasks.add(
                SimpleTask(
                    taskId = e.id!!,
                    taskName = e.name,
                    atomCode = e.getAtomCode(),
                    status = taskStatus.name,
                    startTime = taskStartTimeMills,
                    endTime = taskEndTimeMills
                )
            )
        }
    }

    private val executors = Executors.newFixedThreadPool(8)

    companion object {
        private val logger = LoggerFactory.getLogger(CallBackControl::class.java)
        private val JSON = MediaType.parse("application/json;charset=utf-8")
    }
}
