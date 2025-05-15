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

package com.tencent.devops.process.plugin.trigger.timer.listener

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.PipelineEventListener
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TIMING_START_EVENT_DESC
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.api.service.ServiceTimerBuildResource
import com.tencent.devops.process.constant.MeasureConstant.NAME_PIPELINE_CRON_EXECUTE_DELAY
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_BRANCH_IS_EMPTY
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_BRANCH_NOT_FOUND
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_BRANCH_NO_CHANGE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_BRANCH_UNKNOWN
import com.tencent.devops.process.engine.pojo.PipelineTimer
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerBuildEvent
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEventBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMsg
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.service.TimerScheduleMeasureService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 *  MQ实现的流水线原子任务执行事件
 *
 * @version 1.0
 */
@Component
class PipelineTimerBuildListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val serviceTimerBuildResource: ServiceTimerBuildResource,
    private val pipelineTimerService: PipelineTimerService,
    private val scmProxyService: ScmProxyService,
    private val triggerEventService: PipelineTriggerEventService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val timerScheduleMeasureService: TimerScheduleMeasureService
) : PipelineEventListener<PipelineTimerBuildEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineTimerBuildEvent) {
        logger.info("Receive PipelineTimerBuildEvent from MQ|[$event]")
        try {
            val pipelineTimer =
                pipelineTimerService.get(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    taskId = event.taskId
                ) ?: return
            with(pipelineTimer) {
                when {
                    repoHashId.isNullOrBlank() ->
                        timerTrigger(
                            event = event,
                            params = event.startParam ?: mapOf(),
                            taskId = pipelineTimer.taskId
                        )

                    else ->
                        repoTimerTrigger(
                            event = event,
                            pipelineTimer = pipelineTimer
                        )
                }
            }
        } catch (ignored: Exception) {
            logger.warn("fail to trigger pipeline|event=$event", ignored)
        } finally {
            timerScheduleMeasureService.recordActualExecutionTime(
                name = NAME_PIPELINE_CRON_EXECUTE_DELAY,
                event = event
            )
        }
    }

    private fun timerTrigger(
        event: PipelineTimerBuildEvent,
        params: Map<String, String> = emptyMap(),
        taskId: String
    ): String? {
        with(event) {
            try {
                val buildResult = serviceTimerBuildResource.timerTrigger(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    params = params,
                    channelCode = channelCode
                )

                // 如果是不存在的流水线，则直接删除定时任务，相当于给异常创建失败的定时流水线做清理
                if (buildResult.data.isNullOrBlank()) {
                    pipelineTimerService.deleteTimer(projectId, pipelineId, userId, taskId)
                    logger.warn("[$pipelineId]|pipeline not exist!${buildResult.message}")
                } else {
                    logger.info("[$pipelineId]|TimerTrigger start| buildId=${buildResult.data}")
                }
                return buildResult.data
            } catch (t: OperationException) {
                logger.info("[$pipelineId]|TimerTrigger no start| msg=${t.message}")
            } catch (ignored: Throwable) {
                logger.warn("[$pipelineId]|TimerTrigger fail event=$this| error=${ignored.message}")
            }
            return null
        }
    }

    @Suppress("NestedBlockDepth")
    private fun repoTimerTrigger(event: PipelineTimerBuildEvent, pipelineTimer: PipelineTimer) {
        val messages = mutableSetOf<String>()
        val branchMessages = mutableMapOf<String/*messageCode*/, MutableSet<String>/*branch*/>()
        with(pipelineTimer) {
            try {
                val finalBranchs = if (branchs.isNullOrEmpty()) {
                    val repositoryConfig = RepositoryConfig(
                        repositoryHashId = repoHashId!!,
                        repositoryName = null,
                        repositoryType = RepositoryType.ID
                    )
                    scmProxyService.getDefaultBranch(
                        projectId = projectId,
                        repositoryConfig = repositoryConfig
                    )?.let {
                        listOf(it)
                    }
                } else {
                    branchs
                }
                if (finalBranchs.isNullOrEmpty()) {
                    logger.info("time scheduled branch not found|$projectId|$pipelineId")
                    messages.add(I18nUtil.getCodeLanMessage(ERROR_PIPELINE_TIMER_BRANCH_IS_EMPTY))
                    return
                }
                // 填充触发器启动参数
                val startParams = mutableMapOf<String, String>()
                event.startParam?.let {
                    startParams.putAll(it)
                }
                finalBranchs.forEach { branch ->
                    if (noScm == true) {
                        branchTimerTrigger(
                            event = event,
                            repoHashId = repoHashId!!,
                            branch = branch,
                            branchMessages = branchMessages,
                            startParams = startParams,
                            taskId = pipelineTimer.taskId
                        )
                    } else {
                        startParams.putAll(
                            mapOf(
                                BK_REPO_WEBHOOK_HASH_ID to repoHashId!!,
                                PIPELINE_WEBHOOK_BRANCH to branch
                            )
                        )
                        timerTrigger(
                            event = event,
                            params = startParams,
                            taskId = pipelineTimer.taskId
                        )
                    }
                }
            } catch (ignored: Exception) {
                logger.warn("repo scheduled trigger fail|$projectId|$pipelineId|$repoHashId|$branchs")
                messages.add(ignored.message ?: "scheduled trigger failed")
            }
            messages.addAll(
                branchMessages.map { (messageCode, branchs) ->
                    I18nUtil.getCodeLanMessage(
                        messageCode = messageCode,
                        params = arrayOf(branchs.joinToString(","))
                    )
                }
            )
            if (messages.isNotEmpty()) {
                saveTriggerEvent(
                    projectId = projectId,
                    userId = event.userId,
                    pipelineId = pipelineId,
                    reasonDetail = PipelineTriggerFailedMsg(JsonUtil.toJson(messages))
                )
            }
        }
    }

    private fun branchTimerTrigger(
        event: PipelineTimerBuildEvent,
        repoHashId: String,
        branch: String,
        branchMessages: MutableMap<String, MutableSet<String>>,
        startParams: MutableMap<String, String>,
        taskId: String
    ) {
        val repositoryConfig = RepositoryConfig(
            repositoryHashId = repoHashId,
            repositoryName = null,
            repositoryType = RepositoryType.ID
        )
        with(event) {
            logger.info("start to build by time trigger|$projectId|$pipelineId|$repoHashId|$branch")
            try {
                val revision = scmProxyService.recursiveFetchLatestRevision(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repositoryConfig = repositoryConfig,
                    branchName = branch,
                    variables = emptyMap()
                ).data?.revision ?: run {
                    branchMessages.computeIfAbsent(ERROR_PIPELINE_TIMER_BRANCH_NOT_FOUND) {
                        mutableSetOf()
                    }.add(branch)
                    return
                }
                val timerBranch = pipelineTimerService.getTimerBranch(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = taskId,
                    repoHashId = repoHashId,
                    branch = branch
                )
                if (timerBranch == null || timerBranch.revision != revision) {
                    startParams.putAll(
                        mapOf(
                            BK_REPO_WEBHOOK_HASH_ID to repoHashId,
                            PIPELINE_WEBHOOK_BRANCH to branch
                        )
                    )
                    val buildId = timerTrigger(
                        event = event,
                        params = startParams,
                        taskId = taskId
                    ) ?: return
                    logger.info("success to build by time trigger|$projectId|$pipelineId|$repoHashId|$branch|$buildId")
                    pipelineTimerService.saveTimerBranch(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        taskId = taskId,
                        repoHashId = repoHashId,
                        branch = branch,
                        revision = revision
                    )
                } else {
                    logger.info("branch scheduled trigger fail,revision not change|$pipelineId|$repoHashId|$branch")
                    branchMessages.computeIfAbsent(ERROR_PIPELINE_TIMER_BRANCH_NO_CHANGE) {
                        mutableSetOf()
                    }.add(branch)
                }
            } catch (ignored: Exception) {
                logger.warn("branch scheduled trigger fail|$projectId|$pipelineId|$repoHashId|$branch", ignored)
                branchMessages.computeIfAbsent(ERROR_PIPELINE_TIMER_BRANCH_UNKNOWN) {
                    mutableSetOf()
                }.add(branch)
            }
        }
    }

    private fun saveTriggerEvent(
        projectId: String,
        userId: String,
        pipelineId: String,
        reasonDetail: PipelineTriggerReasonDetail
    ) {
        val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId = pipelineId) ?: run {
            logger.warn("time trigger pipeline not found|$projectId|$pipelineId")
            return
        }
        val requestId = MDC.get(TraceTag.BIZID)
        val eventId = triggerEventService.getEventId()
        val triggerEventBuilder = PipelineTriggerEventBuilder()
        triggerEventBuilder.requestId(requestId)
        triggerEventBuilder.projectId(projectId)
        triggerEventBuilder.eventId(eventId)
        triggerEventBuilder.triggerUser(userId)
        triggerEventBuilder.createTime(LocalDateTime.now())
        triggerEventBuilder.triggerType(PipelineTriggerType.TIME_TRIGGER.name)
        triggerEventBuilder.eventSource(userId)
        triggerEventBuilder.eventType(PipelineTriggerType.TIME_TRIGGER.name)
        triggerEventBuilder.eventDesc(
            I18Variable(
                code = TIMING_START_EVENT_DESC,
                params = listOf(userId)
            ).toJsonStr()
        )

        val triggerDetailBuilder = PipelineTriggerDetailBuilder()
        triggerDetailBuilder.eventId(eventId)
        triggerDetailBuilder.projectId(projectId)
        triggerDetailBuilder.pipelineId(pipelineId = pipelineId)
        triggerDetailBuilder.pipelineName(pipeline.pipelineName)
        triggerDetailBuilder.detailId(triggerEventService.getDetailId())
        triggerDetailBuilder.status(PipelineTriggerStatus.FAILED.name)
        triggerDetailBuilder.reason(PipelineTriggerReason.TRIGGER_FAILED.name)
        triggerDetailBuilder.reasonDetail(reasonDetail)

        triggerEventService.saveEvent(
            triggerEvent = triggerEventBuilder.build(),
            triggerDetail = triggerDetailBuilder.build()
        )
    }
}
