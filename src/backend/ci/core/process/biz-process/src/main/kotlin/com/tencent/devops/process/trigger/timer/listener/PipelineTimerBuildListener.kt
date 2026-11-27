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

package com.tencent.devops.process.trigger.timer.listener

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.PipelineEventListener
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.TimerNodeType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TIMING_START_EVENT_DESC
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.api.service.ServiceTimerBuildResource
import com.tencent.devops.process.constant.MeasureConstant.NAME_PIPELINE_CRON_EXECUTE_DELAY
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CREATIVE_STREAM_TIMER_TRIGGER_NODE_IS_EMPTY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_TIMER_TRIGGER_FAILED_NOTIFY_BODY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_TIMER_TRIGGER_FAILED_NOTIFY_TITLE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_BRANCH_IS_EMPTY
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_BRANCH_NOT_FOUND
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_BRANCH_NO_CHANGE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PIPELINE_TIMER_YAML_FILE_MISSING
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_YAML_BRANCH_TIME
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_BRANCH_UNKNOWN
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_TIMER_YAML_BRANCH_VERSION
import com.tencent.devops.process.engine.pojo.PipelineTimer
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.trigger.event.PipelineTimerBuildEvent
import com.tencent.devops.process.trigger.timer.PipelineTimerService
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEventBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedErrorCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMsg
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.trigger.timer.PipelineYamlBranchTimerService
import com.tencent.devops.process.trigger.CreateStreamTriggerSupportService
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.trigger.pac.PipelineYamlFacadeService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.PipelineTriggerMeasureService
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
    private val creativeStreamService: CreateStreamTriggerSupportService,
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val redisOperation: RedisOperation,
    private val client: Client
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
            if (!pipelineTimer.yamlBranch.isNullOrBlank()) {
                yamlBranchTimerTrigger(event = event, pipelineTimer = pipelineTimer)
                return
            }
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
        taskId: String,
        version: Int? = null
    ): String? {
        // 代码库定时触发会按分支逐个启动，各分支成败相互独立，去重标记必须按分支分开，
        // 否则 A 分支的失败标记会让 B 分支的同类失败漏记，B 分支成功又会把 A 的标记清掉
        val scope = params[PIPELINE_WEBHOOK_BRANCH]
        return with(event) {
            try {
                when (timerChannelCode) {
                    ChannelCode.CREATIVE_STREAM -> {
                        event.creativeStreamTimer(
                            params = params,
                            taskId = taskId
                        )
                        null
                    }

                    else -> timerTriggerPipeline(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        params = params,
                        channelCode = timerChannelCode,
                        taskId = taskId,
                        version = version
                    )?.also { clearTriggerFailedMark(taskId = taskId, scope = scope) }
                }
            } catch (t: OperationException) {
                logger.info("[$pipelineId]|TimerTrigger no start| msg=${t.message}")
                null
            } catch (e: PermissionForbiddenException) {
                logger.warn("[$pipelineId]|TimerTrigger no permission|event=$this|msg=${e.message}")
                // 无权限异常的完整信息在 defaultMessage 中，直接存储，避免错误码模板{0}无值渲染
                recordTriggerFailed(
                    taskId = taskId,
                    scope = scope,
                    reasonDetail = PipelineTriggerFailedMsg(e.defaultMessage ?: e.message ?: "")
                )
                null
            } catch (ignored: Throwable) {
                logger.warn("[$pipelineId]|TimerTrigger fail event=$this| error=${ignored.message}")
                // 保存触发失败事件
                recordTriggerFailed(
                    taskId = taskId,
                    scope = scope,
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
                null
            }
        }
    }

    /**
     * 把启动异常转成触发记录里的失败原因，口径与 [timerTrigger] 的异常处理一致。
     * 返回 null 表示这类异常不记录：启动流程主动放弃本次触发，不算触发失败。
     */
    private fun Throwable.toTriggerFailedReason(): PipelineTriggerReasonDetail? = when (this) {
        is OperationException -> null
        // 无权限异常的完整信息在 defaultMessage 中，直接存储，避免错误码模板{0}无值渲染
        is PermissionForbiddenException -> PipelineTriggerFailedMsg(defaultMessage ?: message ?: "")
        is ErrorCodeException -> PipelineTriggerFailedErrorCode(
            errorCode = errorCode,
            params = params?.toList() ?: listOf()
        )

        else -> PipelineTriggerFailedMsg(message ?: "")
    }

    /**
     * 定时触发失败后的统一处理：记触发记录 + 给相关人推送消息。
     *
     * 定时触发按分钟级频率重复执行，同一个故障（构建节点不可用、没有节点操作权限等）会一直复现。
     * 若每次都落一条触发记录，列表会被同一条原因刷满，真正有用的其它失败反而被淹没，
     * 所以连续出现同一个失败原因时只记第一条，直到重新触发成功才重置。创作流和普通流水线同样适用。
     * 推送是另一条口径：故障没修好就该持续提醒，但不能变成打扰，按天限一次。
     *
     * [scope] 是去重的细分维度：一次定时触发会启动多次构建时（创作流按运行节点、代码库定时按分支），
     * 各自的成败相互独立，必须分开计标记，否则会互相覆盖。只启动一次的场景传 null。
     */
    private fun PipelineTimerBuildEvent.recordTriggerFailed(
        taskId: String,
        scope: String?,
        reasonDetail: PipelineTriggerReasonDetail
    ) {
        // 失败原因作为去重标记的值：原因变了说明是新的故障，要重新记一条
        val signature = JsonUtil.toJson(reasonDetail)
        val failedKey = triggerFailedKey(taskId, scope)
        if (redisOperation.get(failedKey) != signature) {
            redisOperation.set(key = failedKey, value = signature, expiredInSecond = FAILED_MARK_EXPIRED_SECOND)
            saveTriggerEvent(
                projectId = projectId,
                userId = userId,
                pipelineId = pipelineId,
                reasonDetail = reasonDetail
            )
        }
        notifyTriggerFailed(taskId = taskId, scope = scope, reasonDetail = reasonDetail)
    }

    /**
     * 触发成功后清掉失败标记，下次再失败重新记一条，避免故障修好又复发时漏记。
     */
    private fun PipelineTimerBuildEvent.clearTriggerFailedMark(taskId: String, scope: String?) {
        redisOperation.delete(triggerFailedKey(taskId, scope))
    }

    /**
     * 把失败原因推送给流水线授权人和触发人。按「流水线 + 触发器 + 去重维度」限频，同一个故障每天最多推一次。
     * 通知失败不能影响触发流程本身，异常只记日志。
     */
    private fun PipelineTimerBuildEvent.notifyTriggerFailed(
        taskId: String,
        scope: String?,
        reasonDetail: PipelineTriggerReasonDetail
    ) {
        val notifyKey = triggerNotifyKey(taskId, scope)
        try {
            if (!redisOperation.setIfAbsent(
                    key = notifyKey,
                    value = LocalDateTime.now().toString(),
                    expiredInSecond = NOTIFY_INTERVAL_SECOND
                )
            ) {
                return
            }
            // 这次没有发出去时要放掉限频标记，否则这一天都不会再提醒
            val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: run {
                redisOperation.delete(notifyKey)
                return
            }
            val receivers = setOfNotNull(
                creativeStreamService.getPipelineOAuthUser(projectId, pipelineId)?.takeIf(String::isNotBlank),
                userId.takeIf(String::isNotBlank)
            )
            if (receivers.isEmpty()) {
                redisOperation.delete(notifyKey)
                return
            }
            val reason = reasonDetail.getReasonDetailList()?.joinToString("；").orEmpty()
            // 文案只按流水线写，创作流渠道由 MessageUtil 统一替换称谓。
            // MQ 消费没有 ChannelContext，渠道必须显式传，否则创作流收到的是「流水线」
            val channel = timerChannelCode.name
            val message = RtxNotifyMessage().apply {
                addAllReceivers(receivers)
                title = I18nUtil.getCodeLanMessage(
                    messageCode = BK_TIMER_TRIGGER_FAILED_NOTIFY_TITLE,
                    params = arrayOf(pipeline.pipelineName),
                    defaultMessage = "定时触发失败：${pipeline.pipelineName}",
                    channel = channel
                )
                body = I18nUtil.getCodeLanMessage(
                    messageCode = BK_TIMER_TRIGGER_FAILED_NOTIFY_BODY,
                    params = arrayOf(pipeline.pipelineName, reason),
                    defaultMessage = "流水线[${pipeline.pipelineName}]的定时触发未能执行，原因：$reason。" +
                            "处理完成后将自动恢复触发。",
                    channel = channel
                )
            }
            client.get(ServiceNotifyResource::class).sendRtxNotify(message)
        } catch (ignored: Throwable) {
            redisOperation.delete(notifyKey)
            logger.warn("[$pipelineId]|notify timer trigger failed error|taskId=$taskId", ignored)
        }
    }

    private fun PipelineTimerBuildEvent.triggerFailedKey(taskId: String, scope: String?) =
        "$TIMER_TRIGGER_FAILED_KEY_PREFIX$projectId:$pipelineId:$taskId:${scope.orEmpty()}"

    private fun PipelineTimerBuildEvent.triggerNotifyKey(taskId: String, scope: String?) =
        "$TIMER_TRIGGER_NOTIFY_KEY_PREFIX$projectId:$pipelineId:$taskId:${scope.orEmpty()}"

    /**
     * 创作流定时触发。
     *
     * 运行资源决定到点启动几次构建：绑定单台机器时启动一次，由启动流程解析出那台机器；
     * 绑定环境时环境下每个节点各启动一次，触发器里勾选了节点就只跑勾中的那些。
     */
    private fun PipelineTimerBuildEvent.creativeStreamTimer(
        params: Map<String, String> = emptyMap(),
        taskId: String
    ) {
        val triggerElement = checkTriggerExist(
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = taskId
        ) as? TimerTriggerElement ?: return
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val setting = pipelineRepositoryService.getSetting(projectId, pipelineId) ?: return
        val resource = setting.runtimeResource()
        // 绑定单台机器：不存在「选节点」的概念，到点按当前运行资源启动一次。
        // 类型本身已经是单体时就要走这条，不能只看 runtimeResource()：标识没还原出来时它是 null，
        // 掉进下面的环境分支会因为没有环境节点而直接返回，定时触发看起来像没生效。
        // 机器已释放等解析不出来的情况由启动流程报错，这里不提前拦
        if (resource?.group == false || setting.runtimeResourceType?.group == false) {
            try {
                val buildId = timerTriggerPipeline(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    params = params,
                    channelCode = timerChannelCode,
                    taskId = taskId
                )
                if (!buildId.isNullOrBlank()) {
                    clearTriggerFailedMark(taskId = taskId, scope = null)
                }
            } catch (ignored: Throwable) {
                logger.warn("[$pipelineId]|TimerTrigger single resource failed", ignored)
                ignored.toTriggerFailedReason()?.let {
                    recordTriggerFailed(taskId = taskId, scope = null, reasonDetail = it)
                }
            }
            return
        }
        val configuredNodes = triggerElement.nodes?.takeIf {
            triggerElement.nodeType == TimerNodeType.NODE_LIST
        }
        if (triggerElement.nodeType == TimerNodeType.NODE_LIST && configuredNodes.isNullOrEmpty()) {
            recordTriggerFailed(
                taskId = taskId,
                scope = null,
                reasonDetail = PipelineTriggerFailedErrorCode(
                    BK_CREATIVE_STREAM_TIMER_TRIGGER_NODE_IS_EMPTY
                )
            )
            return
        }
        // 勾选了节点就按勾选的逐个触发，这里不预先判断节点还能不能用：环境节点列表是按触发人权限过滤出来的，
        // 用它反推只能得出「不在列表里」，区分不了节点被移出环境、还是触发人没了该节点的操作权限。
        // 两种情况启动流程都会以各自的错误码拒绝，失败原因由它给出才准确。
        // 没勾选节点时才需要环境列表来决定触发几次，环境为空就静默跳过，沿用原有处理
        val targetNodes = configuredNodes ?: creativeStreamService.getRuntimeResourceNodeList(
            pipelineInfo = pipelineInfo,
            pipelineSetting = setting,
            userId = userId
        ).orEmpty()
        if (targetNodes.isEmpty()) {
            logger.info("[$pipelineId]|no runnable node in runtime resource|nodeType=${triggerElement.nodeType}")
            return
        }
        targetNodes.forEach { agentHashId ->
            // 单个节点启动失败不能牵连其它节点：节点可能已被移出环境、已被删除或触发人已无操作权限，
            // 这些都是单节点的问题，兜住异常记一条失败后继续触发剩下的节点
            try {
                val creativeStreamParams = creativeStreamService.creativeStreamParams(
                    projectId = projectId,
                    agentHashId = agentHashId,
                    userId = userId
                )
                timerTriggerPipeline(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    params = params.plus(creativeStreamParams),
                    channelCode = timerChannelCode,
                    taskId = taskId
                )
                clearTriggerFailedMark(taskId = taskId, scope = agentHashId)
            } catch (ignored: Throwable) {
                logger.warn("[$pipelineId]|TimerTrigger node failed|agentHashId=$agentHashId", ignored)
                ignored.toTriggerFailedReason()?.let {
                    recordTriggerFailed(taskId = taskId, scope = agentHashId, reasonDetail = it)
                }
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
        taskId: String,
        version: Int? = null
    ): String? {
        val buildResult = serviceTimerBuildResource.timerTrigger(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            params = params,
            channelCode = channelCode,
            channelCodeHeader = channelCode,
            version = version
        )
        // 如果是不存在的流水线，则直接删除定时任务，相当于给异常创建失败的定时流水线做清理
        if (buildResult.data.isNullOrBlank()) {
            pipelineTimerService.deleteTimer(projectId, pipelineId, userId, taskId)
            logger.warn("[$pipelineId]|pipeline not exist!${buildResult.message}")
            return null
        }
        logger.info("[$pipelineId]|TimerTrigger start| buildId=${buildResult.data}")
        return buildResult.data
    }

    /**
     * 按非默认分支 YAML 执行。分支或文件不存在时记失败，不改跑发布版本。
     */
    private fun yamlBranchTimerTrigger(event: PipelineTimerBuildEvent, pipelineTimer: PipelineTimer) {
        val yamlBranch = pipelineTimer.yamlBranch ?: return
        val elementId = pipelineTimer.startParam?.get(PipelineYamlBranchTimerService.ELEMENT_ID_PARAM)
        val branchResource = pipelineRepositoryService.getBranchVersionResource(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            branchName = yamlBranch
        )
        val elementExists = branchResource?.model?.getTriggerContainer()?.elements?.any {
            it is TimerTriggerElement && it.id == elementId && it.yamlBranch == yamlBranch
        } == true
        if (!elementExists || elementId.isNullOrBlank()) {
            saveTriggerEvent(
                projectId = event.projectId,
                userId = event.userId,
                pipelineId = event.pipelineId,
                reasonDetail = PipelineTriggerFailedMsg(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_PIPELINE_TIMER_YAML_FILE_MISSING,
                        params = arrayOf(yamlBranch),
                        defaultMessage = "分支 $yamlBranch 或该分支上的流水线文件不存在，本次不执行"
                    )
                )
            )
            pipelineTimerService.deleteTimer(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                userId = event.userId,
                taskId = pipelineTimer.taskId
            )
            pipelineTimerService.deleteTimerBranch(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                repoHashId = null,
                branch = null,
                taskId = pipelineTimer.taskId
            )
            return
        }
        val version = try {
            pipelineYamlFacadeService.getPipelineYamlVersion(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                branch = yamlBranch
            )
        } catch (ignored: Exception) {
            logger.warn(
                "yaml-branch timer file missing|${event.projectId}|${event.pipelineId}|$yamlBranch",
                ignored
            )
            saveTriggerEvent(
                projectId = event.projectId,
                userId = event.userId,
                pipelineId = event.pipelineId,
                reasonDetail = when (ignored) {
                    is ErrorCodeException -> PipelineTriggerFailedErrorCode(
                        errorCode = ignored.errorCode,
                        params = ignored.params?.toList() ?: listOf(yamlBranch)
                    )
                    else -> PipelineTriggerFailedMsg(
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_PIPELINE_TIMER_YAML_FILE_MISSING,
                            params = arrayOf(yamlBranch),
                            defaultMessage = "分支 $yamlBranch 或该分支上的流水线文件不存在，本次不执行"
                        )
                    )
                }
            )
            return
        }
        if (version == null) {
            saveTriggerEvent(
                projectId = event.projectId,
                userId = event.userId,
                pipelineId = event.pipelineId,
                reasonDetail = PipelineTriggerFailedMsg(
                    I18nUtil.getCodeLanMessage(
                        messageCode = ERROR_PIPELINE_TIMER_YAML_BRANCH_VERSION,
                        params = arrayOf(yamlBranch),
                        defaultMessage = "分支 $yamlBranch 的定时触发未找到可执行的分支版本，本次不执行"
                    )
                )
            )
            return
        }
        val startParams = mutableMapOf<String, String>()
        pipelineTimer.startParam?.filterKeys {
            it != PipelineYamlBranchTimerService.ELEMENT_ID_PARAM
        }?.let { startParams.putAll(it) }
        startParams[PIPELINE_BUILD_MSG] = I18nUtil.getCodeLanMessage(
            messageCode = BUILD_MSG_YAML_BRANCH_TIME,
            params = arrayOf(yamlBranch),
            defaultMessage = "$yamlBranch 分支上的定时触发"
        )
        startParams[PIPELINE_WEBHOOK_BRANCH] = yamlBranch
        startParams[BK_REPO_GIT_WEBHOOK_BRANCH] = yamlBranch
        pipelineTimer.repoHashId?.let { startParams[BK_REPO_WEBHOOK_HASH_ID] = it }
        if (pipelineTimer.noScm == true) {
            val branchMessages = mutableMapOf<String, MutableSet<String>>()
            branchTimerTrigger(
                event = event,
                repoHashId = pipelineTimer.repoHashId ?: return,
                branch = yamlBranch,
                branchMessages = branchMessages,
                startParams = startParams,
                taskId = pipelineTimer.taskId,
                version = version
            )
            if (branchMessages.isNotEmpty()) {
                saveTriggerEvent(
                    projectId = event.projectId,
                    userId = event.userId,
                    pipelineId = event.pipelineId,
                    reasonDetail = PipelineTriggerFailedMsg(
                        JsonUtil.toJson(
                            branchMessages.map { (messageCode, branches) ->
                                I18nUtil.getCodeLanMessage(
                                    messageCode = messageCode,
                                    params = arrayOf(branches.joinToString(","))
                                )
                            }
                        )
                    )
                )
            }
        } else {
            timerTrigger(
                event = event,
                params = startParams,
                taskId = pipelineTimer.taskId,
                version = version
            )
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
        taskId: String,
        version: Int? = null
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
                        taskId = taskId,
                        version = version
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
    ) = pipelineRepositoryService.getModel(
        projectId = projectId,
        pipelineId = pipelineId
    )?.getTriggerContainer()?.elements?.find {
        it is TimerTriggerElement && it.id == taskId
    }

    /**
     * 清理无效的定时任务
     */
    private fun PipelineTimerBuildEvent.cleanInvalidTimerTask(): Boolean {
        return taskId?.let {
            val triggerExist = checkTriggerExist(
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = it
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

    companion object {
        private const val TIMER_TRIGGER_FAILED_KEY_PREFIX = "process:timer:trigger:failed:"
        private const val TIMER_TRIGGER_NOTIFY_KEY_PREFIX = "process:timer:trigger:notify:"

        /** 失败标记只用于去重，留够排查周期即可，过期后至多多记一条 */
        private const val FAILED_MARK_EXPIRED_SECOND = 7 * 24 * 60 * 60L

        /** 同一个故障的推送间隔：故障不修就每天提醒一次 */
        private const val NOTIFY_INTERVAL_SECOND = 24 * 60 * 60L
    }
}
