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

package com.tencent.devops.process.plugin.trigger.timer.listener

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.PipelineEventListener
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.market.MarketEventAtomElement
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TIMING_START_EVENT_DESC
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.api.service.ServiceTimerBuildResource
import com.tencent.devops.process.constant.MeasureConstant.NAME_PIPELINE_CRON_EXECUTE_DELAY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CREATIVE_STREAM_START_TASK_IS_EMPTY
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
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedErrorCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMsg
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.service.CreativeStreamService
import com.tencent.devops.process.trigger.PipelineTriggerMeasureService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.store.pojo.common.KEY_CREATIVE_TASK_LIST
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_START_TASK_TYPE
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
    private val pipelineTriggerMeasureService: PipelineTriggerMeasureService,
    private val creativeStreamService: CreativeStreamService
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
            // 若存在无效的定时任务，则将其清理，并跳过后续执行
            if (event.cleanInvalidTimerTask()) {
                return
            }
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
            pipelineTriggerMeasureService.recordActualExecutionTime(
                name = NAME_PIPELINE_CRON_EXECUTE_DELAY,
                event = event
            )
        }
    }

    @SuppressWarnings("NestedBlockDepth")
    private fun timerTrigger(
        event: PipelineTimerBuildEvent,
        params: Map<String, String> = emptyMap(),
        taskId: String
    ) {
        with(event) {
            try {
                when (channelCode) {
                    ChannelCode.CREATIVE_STREAM -> {
                        event.creativeStreamTimer(
                            params = params,
                            taskId = taskId
                        )
                    }

                    else -> {
                        timerTriggerPipeline(
                            userId = userId,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            params = params,
                            channelCode = channelCode,
                            taskId = taskId
                        )
                    }
                }
            } catch (t: OperationException) {
                logger.info("[$pipelineId]|TimerTrigger no start| msg=${t.message}")
            } catch (ignored: Throwable) {
                logger.warn("[$pipelineId]|TimerTrigger fail event=$this| error=${ignored.message}")
                // 保存触发失败事件
                saveTriggerEvent(
                    projectId = projectId,
                    userId = userId,
                    pipelineId = pipelineId,
                    reasonDetail = when {
                        ignored is ErrorCodeException -> {
                            PipelineTriggerFailedErrorCode(
                                errorCode = ignored.errorCode,
                                params = ignored.params?.toList() ?: listOf()
                            )
                        }
                        else -> {
                            PipelineTriggerFailedMsg(
                                ignored.message ?: ""
                            )
                        }
                    }
                )
            }
        }
    }

    /**
     * 创作流定时触发
     */
    private fun PipelineTimerBuildEvent.creativeStreamTimer(
        params: Map<String, String> = emptyMap(),
        taskId: String
    ) {
        // 查找触发器
        val triggerElement = checkTriggerExist(
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            taskId = taskId
        ) as? MarketEventAtomElement ?: return
        val input = triggerElement.data[KEY_INPUT] as Map<String, Any>? ?: mapOf()
        val startType = input[KEY_START_TASK_TYPE]
        if (startType == "CREATIVE_TASK") {
            val creativeTaskList = input[KEY_CREATIVE_TASK_LIST] as List<String>? ?: listOf()
            if (creativeTaskList.isEmpty()) {
                // 启动节点为空
                saveTriggerEvent(
                    projectId = projectId,
                    userId = userId,
                    pipelineId = pipelineId,
                    reasonDetail = PipelineTriggerFailedErrorCode(
                        BK_CREATIVE_STREAM_START_TASK_IS_EMPTY
                    )
                )
            } else {
                // 对创作环境下选中的创作节点，逐一启动
                creativeTaskList.forEach {
                    val creativeStreamParams = creativeStreamService.creativeStreamParams(
                        projectId = projectId,
                        agentHashId = it,
                        userId = userId
                    )
                    timerTriggerPipeline(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        params = params.plus(creativeStreamParams),
                        channelCode = channelCode,
                        taskId = taskId
                    )
                }
            }
        } else {
            // 对创作环境下所有的创作节点，逐一启动
            val envHashId = pipelineRepositoryService.getSetting(
                projectId = projectId,
                pipelineId = pipelineId
            )?.envHashId ?: ""
            creativeStreamService.getEnvNodeList(
                projectId = projectId,
                envHashId = envHashId,
                userId = userId
            ).forEach {
                val creativeStreamParams = creativeStreamService.creativeStreamParams(
                    projectId = projectId,
                    agentHashId = it,
                    userId = userId
                )
                timerTriggerPipeline(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    params = params.plus(creativeStreamParams),
                    channelCode = channelCode,
                    taskId = taskId
                )
            }
        }
    }

    /**
     * 触发目标流水线
     */
    private fun timerTriggerPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        params: Map<String, String>,
        channelCode: ChannelCode,
        taskId: String
    ) {
        val buildResult = serviceTimerBuildResource.timerTrigger(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            params = params,
            channelCode = channelCode,
            channelCodeHeader = channelCode
        )
        // 如果是不存在的流水线，则直接删除定时任务，相当于给异常创建失败的定时流水线做清理
        if (buildResult.data.isNullOrBlank()) {
            pipelineTimerService.deleteTimer(projectId, pipelineId, userId, taskId)
            logger.warn("[$pipelineId]|pipeline not exist!${buildResult.message}")
        } else {
            logger.info("[$pipelineId]|TimerTrigger start| buildId=${buildResult.data}")
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

    /**
     * 检查触发器是否存在
     */
    private fun checkTriggerExist(
        projectId: String,
        pipelineId: String,
        taskId: String,
        channelCode: ChannelCode
    ) = pipelineRepositoryService.getModel(
        projectId = projectId,
        pipelineId = pipelineId
    )?.getTriggerContainer()?.elements?.find {
        if (channelCode == ChannelCode.CREATIVE_STREAM) {
            it is MarketEventAtomElement
        } else {
            it is TimerTriggerElement
        } && it.id == taskId
    }

    /**
     * 清理无效的定时任务
     */
    private fun PipelineTimerBuildEvent.cleanInvalidTimerTask(): Boolean {
        return taskId?.let {
            val triggerExist = checkTriggerExist(
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = it,
                channelCode = channelCode
            )
            if (triggerExist == null) {
                // 存在异常定时任务，尝试删除定时任务
                val result = pipelineTimerService.deleteTimer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = it,
                    userId = userId
                )
                val timerBranch = pipelineTimerService.deleteTimerBranch(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repoHashId = null,
                    branch = null,
                    taskId = it
                )
                logger.warn(
                    "[$projectId|$pipelineId|$it]|" +
                            "abnormal scheduled task exists, attempting to delete task($result) and " +
                            "timerBranch($timerBranch)"
                )
                true
            } else {
                false
            }
        } ?: false
    }
}
