/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.engine.control

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.archive.util.closeQuietly
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ProjectPipelineCallbackStatus
import com.tencent.devops.common.pipeline.event.BuildEvent
import com.tencent.devops.common.pipeline.event.CallBackData
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallbackConstants.DEVOPS_ALL_PROJECT
import com.tencent.devops.common.pipeline.event.PipelineEvent
import com.tencent.devops.common.pipeline.event.ProjectCallbackEvent
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.common.pipeline.event.SimpleJob
import com.tencent.devops.common.pipeline.event.SimpleModel
import com.tencent.devops.common.pipeline.event.SimpleStage
import com.tencent.devops.common.pipeline.event.SimpleTask
import com.tencent.devops.common.pipeline.event.StreamEnabledEvent
import com.tencent.devops.common.pipeline.utils.EventUtils.toEventType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.process.engine.pojo.event.PipelineStreamEnabledEvent
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackUrlGenerator
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.pojo.CallBackHeader
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 *  步骤控制器
 * @version 1.0
 */
@Suppress("ALL")
@Service
class CallBackControl @Autowired constructor(
    private val pipelineBuildRecordService: PipelineBuildRecordService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val projectPipelineCallBackService: ProjectPipelineCallBackService,
    private val client: Client,
    private val callbackCircuitBreakerRegistry: CircuitBreakerRegistry,
    private val meterRegistry: MeterRegistry,
    private val projectPipelineCallBackUrlGenerator: ProjectPipelineCallBackUrlGenerator
) {

    @Value("\${callback.failureDisableTimePeriod:#{43200000}}")
    private val failureDisableTimePeriod: Long = DEFAULT_FAILURE_DISABLE_TIME_PERIOD

    fun pipelineCreateEvent(projectId: String, pipelineId: String) {
        callBackPipelineEvent(projectId, pipelineId, CallBackEvent.CREATE_PIPELINE)
    }

    fun pipelineDeleteEvent(projectId: String, pipelineId: String) {
        callBackPipelineEvent(projectId, pipelineId, CallBackEvent.DELETE_PIPELINE)
    }

    fun pipelineUpdateEvent(projectId: String, pipelineId: String) {
        callBackPipelineEvent(projectId, pipelineId, CallBackEvent.UPDATE_PIPELINE)
    }

    fun pipelineRestoreEvent(projectId: String, pipelineId: String) {
        callBackPipelineEvent(projectId, pipelineId, CallBackEvent.RESTORE_PIPELINE)
    }

    fun projectCreate(projectId: String, projectName: String, userId: String) {
        callBackProjectEvent(projectId, projectName, userId, true, CallBackEvent.PROJECT_CREATE)
    }

    fun projectUpdate(projectId: String, projectName: String, userId: String) {
        callBackProjectEvent(projectId, projectName, userId, true, CallBackEvent.PROJECT_UPDATE)
    }

    fun projectEnable(projectId: String, projectName: String, userId: String) {
        callBackProjectEvent(projectId, projectName, userId, true, CallBackEvent.PROJECT_ENABLE)
    }

    fun projectDisable(projectId: String, projectName: String, userId: String) {
        callBackProjectEvent(projectId, projectName, userId, false, CallBackEvent.PROJECT_DISABLE)
    }

    fun pipelineStreamEnabledEvent(event: PipelineStreamEnabledEvent) {
        with(event) {
            logger.info("$projectId|STREAM_ENABLED|callback stream enable event")
            val list = projectPipelineCallBackService.listProjectCallBack(
                projectId = projectId,
                events = CallBackEvent.STREAM_ENABLED.name
            )
            val streamEnabledEvent = StreamEnabledEvent(
                gitProjectId = gitProjectId,
                gitProjectUrl = gitProjectUrl,
                userId = userId,
                enable = enable
            )
            sendToCallBack(CallBackData(event = CallBackEvent.STREAM_ENABLED, data = streamEnabledEvent), list)
        }
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
            projectId = projectId,
            pipelineId = pipelineId,
            delete = null
        ) ?: return

        val pipelineEvent = PipelineEvent(
            pipelineId = pipelineInfo.pipelineId,
            pipelineName = pipelineInfo.pipelineName,
            userId = pipelineInfo.lastModifyUser,
            updateTime = pipelineInfo.updateTime,
            projectId = pipelineInfo.projectId
        )

        sendToCallBack(CallBackData(event = callBackEvent, data = pipelineEvent), list)
    }

    private fun callBackProjectEvent(
        projectId: String,
        projectName: String,
        userId: String,
        enable: Boolean,
        callBackEvent: CallBackEvent
    ) {
        logger.info("$projectId|$projectName|$callBackEvent|callback project event")
        val list = projectPipelineCallBackService.listProjectCallBack(
            projectId = DEVOPS_ALL_PROJECT,
            events = callBackEvent.name
        )
        if (list.isEmpty()) {
            logger.info("no [$callBackEvent] project callback")
            return
        }

        val projectEvent = ProjectCallbackEvent(
            projectId = projectId,
            projectName = projectName,
            enable = enable,
            userId = userId
        )

        sendToCallBack(CallBackData(event = callBackEvent, data = projectEvent), list)
    }

    fun callBackBuildEvent(event: PipelineBuildStatusBroadCastEvent) {
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        val buildId = event.buildId
        if (event.atomCode != null && VmOperateTaskGenerator.isVmAtom(event.atomCode!!)) {
            return
        }
        val callBackEvent = event.toEventType() ?: return

        logger.info("$projectId|$pipelineId|$buildId|${callBackEvent.name}|${event.stageId}|${event.taskId}|callback")
        val list = mutableListOf<ProjectPipelineCallBack>()
        list.addAll(
            projectPipelineCallBackService.listProjectCallBack(
                projectId = projectId,
                events = callBackEvent.name
            )
        )
        // 流水线级别回调，旧数据存在model中，新数据存在数据库中
        val pipelineCallback = projectPipelineCallBackService.getPipelineCallback(
            projectId = projectId,
            pipelineId = pipelineId,
            event = callBackEvent.name
        ).let {
            if (it.isEmpty()) {
                pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId
                )?.model?.getPipelineCallBack(
                    projectId = projectId,
                    callbackEvent = callBackEvent
                ) ?: emptyList()
            } else {
                it
            }
        }
        if (pipelineCallback.isNotEmpty()) {
            list.addAll(pipelineCallback)
        }

        if (list.isEmpty()) {
            return
        }

        val modelDetail = pipelineBuildRecordService.getBuildRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            refreshStatus = false
        ) ?: return

        val buildEvent = BuildEvent(
            buildId = event.buildId,
            pipelineId = modelDetail.pipelineId,
            pipelineName = modelDetail.pipelineName,
            userId = modelDetail.userId,
            triggerUser = modelDetail.triggerUser,
            cancelUserId = modelDetail.cancelUserId,
            status = modelDetail.status,
            startTime = modelDetail.startTime ?: 0,
            endTime = modelDetail.endTime ?: 0,
            model = SimpleModel(parseModel(modelDetail.model)),
            projectId = event.projectId,
            trigger = modelDetail.trigger,
            stageId = event.stageId,
            taskId = event.taskId,
            buildNo = modelDetail.buildNum,
            debug = modelDetail.debug
        )
        sendToCallBack(CallBackData(event = callBackEvent, data = buildEvent), list)
    }

    private fun <T> sendToCallBack(callBackData: CallBackData<T>, list: List<ProjectPipelineCallBack>) {
        val requestBody = ObjectMapper().writeValueAsString(callBackData)

        list.forEach {
            val uniqueId = when (val data = callBackData.data) {
                is PipelineEvent -> {
                    data.pipelineId
                }

                is BuildEvent -> {
                    data.buildId
                }

                else -> ""
            }
            val watcher = Watcher(id = "${it.projectId}|${it.callBackUrl}|${it.events}|$uniqueId")
            try {
                logger.info("${it.projectId}|${it.callBackUrl}|$uniqueId|${it.events}|send to callback")
                if (it.callBackUrl.isBlank()) {
                    logger.warn("[${it.projectId}]| call back url is empty!")
                    return@forEach
                }
                send(uniqueId = uniqueId, callBack = it, requestBody = requestBody)
            } catch (e: Exception) {
                logger.error("BKSystemErrorMonitor|${it.projectId}|${it.callBackUrl}|${it.events}|${e.message}", e)
            } finally {
                watcher.stop()
                LogUtils.printCostTimeWE(watcher, warnThreshold = 2000)
            }
        }
    }

    private fun send(uniqueId: String, callBack: ProjectPipelineCallBack, requestBody: String) {

        val startTime = System.currentTimeMillis()
        val request = Request.Builder()
            .url(callBack.callBackUrl)
            .header("X-DEVOPS-WEBHOOK-TOKEN", callBack.secretToken ?: "NONE")
            .header(TraceTag.TRACE_HEADER_DEVOPS_BIZID, TraceTag.buildBiz())
            .post(RequestBody.create(JSON, requestBody))
            .let { builder ->
                // 回调填自定义header
                callBack.secretParam?.run {
                    secret(builder)
                }
                builder
            }
            .build()

        var errorMsg: String? = null
        var status = ProjectPipelineCallbackStatus.SUCCESS
        // 熔断处理
        val breaker = callbackCircuitBreakerRegistry.circuitBreaker(callBack.callBackUrl)
        try {
            breaker.executeCallable {
                HttpRetryUtils.retry(MAX_RETRY_COUNT) {
                    callbackClient.newCall(request).execute().closeQuietly()
                }
                if (callBack.failureTime != null) {
                    projectPipelineCallBackService.updateFailureTime(
                        projectId = callBack.projectId,
                        id = callBack.id!!,
                        failureTime = null
                    )
                }
            }
        } catch (e: CallNotPermittedException) {
            logger.warn(
                "[${callBack.projectId}]|CALL_BACK|url=${callBack.callBackUrl}|${callBack.events}|" +
                    "failureRate=${breaker.metrics.failureRate}|${e.message}"
            )
            checkIfNeedDisable(breaker, callBack)
            errorMsg = e.message
            status = ProjectPipelineCallbackStatus.FAILED
        } catch (e: Exception) {
            logger.warn(
                "BKSystemErrorMonitor|[${callBack.projectId}]|CALL_BACK|" +
                    "url=${callBack.callBackUrl}|${callBack.events}",
                e
            )
            errorMsg = e.message
            status = ProjectPipelineCallbackStatus.FAILED
        } finally {
            // 去掉代理url后,真实的url地址
            val realUrl = projectPipelineCallBackUrlGenerator.decodeCallbackUrl(
                request.url.toString()
            ).substringBefore("?")
            Counter.builder(PIPELINE_CALLBACK_COUNT)
                .tags(
                    Tags.of("status", status.name)
                        .and("event", callBack.events)
                )
                .register(meterRegistry)
                .increment()
            saveHistory(
                callBack = callBack,
                requestHeaders = listOf(CallBackHeader(name = "X-DEVOPS-WEBHOOK-UNIQUE-ID", value = uniqueId)),
                status = status.name,
                errorMsg = errorMsg,
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * 判断是否需要禁用
     */
    private fun checkIfNeedDisable(
        breaker: CircuitBreaker,
        callBack: ProjectPipelineCallBack
    ) {
        if (breaker.metrics.failureRate == 100.0F) {
            // 如果请求已经连续12个小时100%失败，则说明回调地址已经失效，禁用
            val duration = if (callBack.failureTime != null) {
                System.currentTimeMillis() - callBack.failureTime!!.timestampmilli()
            } else {
                // 记录第一次100%失败的时间
                projectPipelineCallBackService.updateFailureTime(
                    projectId = callBack.projectId,
                    id = callBack.id!!,
                    failureTime = LocalDateTime.now()
                )
                0
            }
            if (duration >= failureDisableTimePeriod) {
                logger.warn(
                    "disable callbacks because of 100% failure rate|" +
                            "[${callBack.projectId}]|CALL_BACK|url=${callBack.callBackUrl}|${callBack.events}|" +
                            "duration=$duration"
                )
                projectPipelineCallBackService.disable(projectId = callBack.projectId, id = callBack.id!!)
            }
        }
    }

    private fun saveHistory(
        callBack: ProjectPipelineCallBack,
        requestHeaders: List<CallBackHeader>,
        status: String,
        errorMsg: String?,
        startTime: Long,
        endTime: Long
    ) {
        try {
            projectPipelineCallBackService.createHistory(
                ProjectPipelineCallBackHistory(
                    projectId = callBack.projectId,
                    callBackUrl = callBack.callBackUrl,
                    events = callBack.events,
                    status = status,
                    errorMsg = errorMsg,
                    requestHeaders = requestHeaders,
                    requestBody = "",
                    responseCode = 0,
                    responseBody = "",
                    startTime = startTime,
                    endTime = endTime,
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("PROJECT_PIPELINE_CALLBACK_HISTORY").data
                )
            )
        } catch (e: Throwable) {
            logger.error("[${callBack.projectId}]|[${callBack.callBackUrl}]|[${callBack.events}]|save fail", e)
        }
    }

    internal fun parseModel(model: Model): List<SimpleStage> {
        val stages = mutableListOf<SimpleStage>()
        model.stages.forEachIndexed { pos, s ->
            val jobs = mutableListOf<SimpleJob>()
            val stage = SimpleStage(
                stageName = "Stage-${pos + 1}",
                name = s.name ?: "",
                status = "",
                jobs = jobs
            )
            logger.info("parseModel ${model.name}|${stage.stageName}|${stage.name}|")
            stage.startTime = s.startEpoch ?: 0
            stages.add(stage)

            val triple = parseJob(s, jobs)
            val stageStartTimeMills = triple.first
            val stageEndTimeMills = triple.second
            val stageStatus = triple.third

            if (stage.startTime == 0L && stageStartTimeMills > 0) {
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
        var stageStartTimeMills = Long.MAX_VALUE
        var stageEndTimeMills = 1L
        var stageStatus = BuildStatus.QUEUE
        s.containers.forEach { c ->
            val jobStatus = BuildStatus.parse(c.status)
            val jobStartTimeMills = c.startEpoch ?: 0L
            val jobEndTimeMills = if (jobStatus.isFinish()) {
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
            } else if (stageEndTimeMills in 1 until jobEndTimeMills) {
                stageEndTimeMills = jobEndTimeMills
                if (jobStatus.isFailure()) {
                    stageStatus = jobStatus
                }
            }

            jobs.add(
                SimpleJob(
                    jobName = c.name,
                    status = jobStatus.name,
                    startTime = jobStartTimeMills,
                    endTime = jobEndTimeMills,
                    tasks = parseTask(c)
                )
            )
        }

        if (stageEndTimeMills > 0 && !stageStatus.isFinish()) {
            stageStatus = BuildStatus.SUCCEED
        }
        return Triple(stageStartTimeMills, stageEndTimeMills, stageStatus)
    }

    private fun parseTask(c: Container): MutableList<SimpleTask> {
        val tasks = mutableListOf<SimpleTask>()
        c.elements.forEach { e ->
            val taskStartTimeMills = e.startEpoch ?: 0
            val taskStatus = BuildStatus.parse(e.status)
            val taskEndTimeMills = if (taskStatus.isFinish()) {
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
        return tasks
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CallBackControl::class.java)
        private val JSON = "application/json;charset=utf-8".toMediaTypeOrNull()
        const val MAX_RETRY_COUNT = 3
        // 默认连续失败12小时,则禁用回调地址
        private const val DEFAULT_FAILURE_DISABLE_TIME_PERIOD = 12 * 60 * 60 * 1000L
        private const val PIPELINE_CALLBACK_COUNT = "pipeline_callback_count"

        private const val connectTimeout = 3L
        private const val readTimeout = 3L
        private const val writeTimeout = 3L

        private fun anySslSocketFactory(): SSLSocketFactory {
            try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAnyCerts, java.security.SecureRandom())
                return sslContext.socketFactory
            } catch (ignored: Exception) {
                throw RemoteServiceException(ignored.message!!)
            }
        }

        private val trustAnyCerts = arrayOf<TrustManager>(object : X509TrustManager {

            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        private val callbackClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .sslSocketFactory(anySslSocketFactory(), trustAnyCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}
