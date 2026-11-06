package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.PIPELINE_PAC_REPO_HASH_ID
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.webhook.service.code.loader.WebhookElementParamsRegistrar
import com.tencent.devops.common.webhook.service.code.loader.WebhookStartParamsRegistrar
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceScmWebhookResource
import com.tencent.devops.process.constant.MeasureConstant
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailBuilder
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedErrorCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatch
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMsg
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import com.tencent.devops.process.trigger.CreateStreamTriggerSupportService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.PipelineTriggerMeasureService
import com.tencent.devops.process.trigger.TriggerProcessClient
import com.tencent.devops.process.trigger.yaml.PipelineYamlService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import io.micrometer.core.instrument.Tags
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * SCM 老链路：单条流水线的匹配、启动和触发记录
 */
@Suppress("ALL")
@Service
class PipelineBuildWebhookTriggerService @Autowired constructor(
    private val client: Client,
    private val triggerProcessClient: TriggerProcessClient,
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineBuildCommitService: PipelineBuildCommitService,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineTriggerMeasureService: PipelineTriggerMeasureService,
    private val creativeStreamTriggerSupportService: CreateStreamTriggerSupportService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildWebhookTriggerService::class.java)
    }

    /**
     * @return 触发成功的构建ID，未触发返回 null
     */
    fun triggerSinglePipeline(
        subscriber: WebhookTriggerPipeline,
        matcher: ScmWebhookMatcher,
        triggerEvent: PipelineTriggerEvent,
        repoEventIdMap: ConcurrentHashMap<String, Long>
    ): String? {
        val pipelineWatcher = Watcher(
            "old WebhookTrigger|${subscriber.projectId}|${subscriber.pipelineId}"
        )
        var status = PipelineTriggerReason.TRIGGER_SUCCESS
        val projectId = subscriber.projectId
        val pipelineId = subscriber.pipelineId
        var buildId: String? = null
        try {
            try {
                logger.info("pipelineId is $pipelineId")
                val builder = PipelineTriggerDetailBuilder()
                    .projectId(projectId)
                    .pipelineId(pipelineId)

                pipelineWatcher.start("trigger pipeline build")
                val triggerResult = webhookTriggerPipelineBuild(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    matcher = matcher,
                    builder = builder
                )
                pipelineWatcher.stop()
                if (!triggerResult) {
                    status = PipelineTriggerReason.TRIGGER_NOT_MATCH
                }
                buildId = builder.getBuildId()
                pipelineWatcher.start("save trigger event")
                saveTriggerEvent(
                    projectId = projectId,
                    builder = builder,
                    triggerEvent = triggerEvent,
                    repoEventIdMap = repoEventIdMap
                )
                pipelineWatcher.stop()
            } catch (e: Throwable) {
                status = PipelineTriggerReason.TRIGGER_FAILED
                logger.warn("[$pipelineId]|webhookTriggerPipelineBuild fail: $e", e)
            }
            val totalCostMills = System.currentTimeMillis() - triggerEvent.createTime.timestampmilli()
            if (totalCostMills >= 60 * 1000) {
                logger.warn(
                    "old Webhook trigger execution time exceeds threshold|" +
                        "${matcher.getRepoName()}|$projectId|$pipelineId|$totalCostMills"
                )
            }
            pipelineTriggerMeasureService.recordTaskExecutionTime(
                name = MeasureConstant.PIPELINE_SCM_WEBHOOK_EXECUTE_TIME,
                tags = Tags.of(MeasureConstant.TAG_SCM_WEBHOOK_TRIGGER_STATUS, status.name)
                    .and(MeasureConstant.TAG_SCM_WEBHOOK_TRIGGER_YAML, "false")
                    .and(MeasureConstant.TAG_SCM_WEBHOOK_TRIGGER_OLD, "true")
                    .and(MeasureConstant.TAG_SCM_WEBHOOK_SCM_CODE, triggerEvent.triggerType)
                    .toList(),
                timeConsumingMills = totalCostMills
            )
        } finally {
            LogUtils.printCostTimeWE(pipelineWatcher)
        }
        return buildId
    }

    private fun saveTriggerEvent(
        projectId: String,
        builder: PipelineTriggerDetailBuilder,
        triggerEvent: PipelineTriggerEvent,
        repoEventIdMap: ConcurrentHashMap<String, Long>
    ) {
        if (!builder.getEventSource().isNullOrBlank()) {
            val eventSource = builder.getEventSource()!!
            val eventType = triggerEvent.eventType
            val eventId = repoEventIdMap.computeIfAbsent(eventSource) {
                pipelineTriggerEventService.getEventId(
                    projectId = projectId,
                    requestId = triggerEvent.requestId,
                    eventSource = eventSource
                )
            }
            builder.eventId(eventId)
            builder.detailId(pipelineTriggerEventService.getDetailId())
            val triggerDetail = builder.build()
            val eventToSave = triggerEvent.copy(
                projectId = projectId,
                eventSource = eventSource,
                eventId = eventId
            )
            pipelineTriggerEventService.saveEvent(
                triggerEvent = eventToSave,
                triggerDetail = triggerDetail
            )
            // 判断刷新的eventType和repository_hash_id字段的准确性,为后期优化做准备
            pipelineWebhookService.get(
                projectId = projectId,
                pipelineId = triggerDetail.pipelineId!!,
                repositoryHashId = eventSource,
                eventType = eventType
            ) ?: run {
                logger.warn(
                    "Failed to match pipeline webhook|$projectId|${triggerDetail.pipelineId}|$eventSource|$eventType"
                )
            }
        }
    }

    @BkTimed
    private fun webhookTriggerPipelineBuild(
        projectId: String,
        pipelineId: String,
        matcher: ScmWebhookMatcher,
        builder: PipelineTriggerDetailBuilder
    ): Boolean {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: return false

        val model = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId)?.model
        if (model == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return false
        }
        // 触发事件保存流水线名称
        builder.pipelineName(pipelineInfo.pipelineName)
        // 获取授权人
        val userId = pipelineRepositoryService.getPipelineOauthUser(projectId, pipelineId)
            ?: pipelineInfo.lastModifyUser
        val container = model.getTriggerContainer()
        // 解析变量，兼容代码库分支等级联参数
        val variables = pipelineRepositoryService.getTriggerParams(container).toMutableMap()
        // 补充yaml流水线代码库信息
        pipelineYamlService.getPipelineYamlInfo(projectId = projectId, pipelineId = pipelineId)?.let {
            variables[PIPELINE_PAC_REPO_HASH_ID] = it.repoHashId
        }

        val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
        // 寻找代码触发原子
        container.elements.forEach elements@{ element ->
            if (!element.elementEnabled() || element !is WebHookTriggerElement) {
                logger.info("Trigger element is disable, can not start pipeline")
                return@elements
            }
            val webHookParams = WebhookElementParamsRegistrar.getService(element)
                .getWebhookElementParams(element, variables) ?: return@elements
            val repositoryConfig = webHookParams.repositoryConfig
            if (repositoryConfig.getRepositoryId().isBlank()) {
                logger.info("repositoryHashId is blank for code trigger pipeline $pipelineId ")
                return@elements
            }

            logger.info("$pipelineId|${element.name}|Get the code trigger pipeline")
            // #2958 如果仓库找不到,会抛出404异常,就不会继续往下遍历
            val repo = try {
                client.get(ServiceRepositoryResource::class).get(
                    projectId,
                    repositoryConfig.getURLEncodeRepositoryId(),
                    repositoryConfig.repositoryType
                ).data
            } catch (e: Exception) {
                null
            }
            if (repo == null) {
                logger.warn("$pipelineId|repo[$repositoryConfig] does not exist")
                return@elements
            }

            val matchResult = matcher.isMatch(projectId, pipelineId, repo, webHookParams)
            if (matchResult.isMatch) {
                try {
                    // 创作流相关参数，触发成功时才去校验创作流运行环境，避免重复查询创作流环境信息
                    val externalStartParams = creativeStreamTriggerSupportService.externalWebhookStartParams(
                        pipelineInfo = pipelineInfo,
                        userId = userId
                    )
                    val webhookCommit = WebhookCommit(
                        userId = userId,
                        pipelineId = pipelineId,
                        version = null,
                        params = WebhookStartParamsRegistrar.getService(element).getStartParams(
                            projectId = projectId,
                            element = element,
                            repo = repo,
                            matcher = matcher,
                            variables = variables,
                            params = webHookParams,
                            matchResult = matchResult
                        ).plus(externalStartParams),
                        repositoryConfig = repositoryConfig,
                        repoName = matcher.getRepoName(),
                        commitId = matcher.getRevision(),
                        block = webHookParams.block,
                        eventType = matcher.getEventType(),
                        codeType = webHookParams.codeType
                    )
                    // 执行权限在 process 启动构建时校验
                    val buildId = triggerProcessClient.call(projectId, ServiceScmWebhookResource::class) {
                        it.webhookCommit(projectId, webhookCommit).data
                    }
                    logger.info("$pipelineId|$buildId|webhook trigger|(${element.name}|repo(${matcher.getRepoName()})")
                    if (!buildId.isNullOrEmpty()) {
                        pipelineBuildCommitService.create(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            matcher = matcher,
                            repo = repo,
                            channelCode = pipelineInfo.channelCode
                        )
                        val buildDetail = triggerProcessClient.call(projectId, ServiceBuildResource::class) {
                            it.getBuildDetail(
                                userId = userId,
                                buildId = buildId,
                                pipelineId = pipelineId,
                                projectId = projectId,
                                channelCode = pipelineInfo.channelCode
                            ).data
                        }
                        builder.buildId(buildId)
                            .status(PipelineTriggerStatus.SUCCEED.name)
                            .eventSource(eventSource = repo.repoHashId!!)
                            .reason(PipelineTriggerReason.TRIGGER_SUCCESS.name)
                            .buildNum(buildDetail?.buildNum.toString())
                    }
                } catch (ignore: Exception) {
                    if (isPermissionForbidden(ignore)) {
                        logger.warn("check permission failed", ignore)
                        builder.eventSource(repo.repoHashId!!)
                            .status(PipelineTriggerStatus.FAILED.name)
                            .reason(PipelineTriggerReason.TRIGGER_FAILED.name)
                            .reasonDetail(
                                PipelineTriggerFailedErrorCode(
                                    errorCode = ProcessMessageCode.BK_AUTHOR_NOT_PIPELINE_EXECUTE_PERMISSION,
                                    params = listOf(userId)
                                )
                            )
                        // 当前流水线没有权限触发
                        return false
                    }
                    logger.warn("$pipelineId|webhook trigger|(${element.name})|repo(${matcher.getRepoName()})", ignore)
                    builder.eventSource(eventSource = repo.repoHashId!!)
                    builder.status(PipelineTriggerStatus.FAILED.name)
                        .reason(PipelineTriggerReason.TRIGGER_FAILED.name)
                        .reasonDetail(PipelineTriggerFailedMsg(ignore.message ?: ""))
                }
                return true
            } else {
                logger.info(
                    "$pipelineId|webhook trigger match unsuccess|(${element.name})|repo(${matcher.getRepoName()})"
                )
                if (!matchResult.reason.isNullOrBlank()) {
                    builder.eventSource(eventSource = repo.repoHashId!!)
                    failedMatchElements.add(
                        PipelineTriggerFailedMatchElement(
                            elementId = element.id,
                            elementName = element.name,
                            elementAtomCode = element.getAtomCode(),
                            reasonMsg = matchResult.reason!!
                        )
                    )
                }
            }
        }

        // 历史原因,webhook表没有记录eventType,所以查找出来的订阅者可能因为事件类型不匹配,事件不需要记录
        if (!builder.getEventSource().isNullOrBlank()) {
            builder.status(PipelineTriggerStatus.FAILED.name)
                .reason(PipelineTriggerReason.TRIGGER_NOT_MATCH.name)
                .reasonDetail(PipelineTriggerFailedMatch(failedMatchElements))
        }
        return false
    }

    /**
     * 同进程调用抛出 PermissionForbiddenException，走 Feign 或网关时被解码为 403 的 RemoteServiceException
     */
    private fun isPermissionForbidden(e: Exception): Boolean {
        return e is PermissionForbiddenException ||
            (e is RemoteServiceException && e.httpStatus == Response.Status.FORBIDDEN.statusCode)
    }
}
