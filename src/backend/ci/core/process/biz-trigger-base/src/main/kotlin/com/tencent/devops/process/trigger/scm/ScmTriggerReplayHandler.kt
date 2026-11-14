package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.util.EventCacheUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineTriggerEventDao
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.pojo.trigger.ScmWebhookEventBody
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookTriggerService
import com.tencent.devops.process.trigger.replay.TriggerReplayHandler
import com.tencent.devops.process.webhook.CodeWebhookEventDispatcher
import com.tencent.devops.process.webhook.WebhookEventFactory
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import com.tencent.devops.repository.api.ServiceRepositoryPermissionResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.ServiceRepositoryWebhookResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryWebhookRequest
import com.tencent.devops.repository.pojo.webhook.WebhookParseRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 代码库触发事件重放
 *
 * 重放依赖 yaml 同步、批量匹配等 trigger 侧逻辑，投递 ReplayWebhookEvent 由 trigger 消费；
 * 重新构建只针对已有流水线，在当前线程按最新版本直接匹配触发
 */
@Service
class ScmTriggerReplayHandler(
    private val client: Client,
    private val dslContext: DSLContext,
    private val streamBridge: StreamBridge,
    private val pipelineTriggerEventDao: PipelineTriggerEventDao,
    private val grayService: WebhookGrayService,
    private val webhookEventFactory: WebhookEventFactory,
    private val scmWebhookTriggerBuildService: ScmWebhookTriggerBuildService,
    private val pipelineBuildWebhookTriggerService: PipelineBuildWebhookTriggerService
) : TriggerReplayHandler {

    override fun support(triggerType: PipelineTriggerType) = triggerType.toScmType() != null

    override fun replay(replayEvent: ReplayWebhookEvent, replayTriggerEvent: PipelineTriggerEvent) {
        prepareWebhookRequest(replayEvent = replayEvent, replayTriggerEvent = replayTriggerEvent)
        CodeWebhookEventDispatcher.dispatchReplayEvent(streamBridge = streamBridge, event = replayEvent)
    }

    override fun replayBuild(replayEvent: ReplayWebhookEvent, replayTriggerEvent: PipelineTriggerEvent): String? {
        val pipelineId = replayEvent.pipelineId ?: return null
        val webhookRequest = prepareWebhookRequest(replayEvent = replayEvent, replayTriggerEvent = replayTriggerEvent)
        val repository = replayTriggerEvent.eventSource?.let {
            getRepository(projectId = replayEvent.projectId, repoHashId = it)
        } ?: return null
        return if (grayService.isGrayRepo(repository.scmCode, repository.projectName)) {
            replayBuildGrayRepo(
                replayEvent = replayEvent,
                pipelineId = pipelineId,
                repository = repository,
                webhookRequest = webhookRequest,
                replayTriggerEvent = replayTriggerEvent
            )
        } else {
            replayBuildOldRepo(
                replayEvent = replayEvent,
                pipelineId = pipelineId,
                webhookRequest = webhookRequest,
                replayTriggerEvent = replayTriggerEvent
            )
        }
    }

    /**
     * 校验代码库权限，并以重放事件的 requestId 复制一份源 webhook 请求，后续解析都基于这份请求
     */
    private fun prepareWebhookRequest(
        replayEvent: ReplayWebhookEvent,
        replayTriggerEvent: PipelineTriggerEvent
    ): RepositoryWebhookRequest {
        client.get(ServiceRepositoryPermissionResource::class).validatePermission(
            userId = replayEvent.userId,
            projectId = replayEvent.projectId,
            repositoryHashId = replayTriggerEvent.eventSource!!,
            permission = AuthPermission.USE
        )
        val webhookRequest = client.get(ServiceRepositoryWebhookResource::class).getWebhookRequest(
            requestId = replayEvent.replayRequestId
        ).data ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_WEBHOOK_REQUEST_NOT_FOUND)
        val replayWebhookRequest = webhookRequest.copy(
            requestId = replayTriggerEvent.requestId,
            createTime = LocalDateTime.now()
        )
        client.get(ServiceRepositoryWebhookResource::class).saveWebhookRequest(
            repositoryWebhookRequest = replayWebhookRequest
        )
        return replayWebhookRequest
    }

    private fun replayBuildGrayRepo(
        replayEvent: ReplayWebhookEvent,
        pipelineId: String,
        repository: Repository,
        webhookRequest: RepositoryWebhookRequest,
        replayTriggerEvent: PipelineTriggerEvent
    ): String? {
        val projectId = replayEvent.projectId
        val webhook = client.get(ServiceRepositoryWebhookResource::class).webhookParseByRepo(
            scmCode = repository.scmCode,
            projectId = projectId,
            repoHashId = repository.repoHashId!!,
            request = WebhookParseRequest(
                requestId = replayTriggerEvent.requestId,
                headers = webhookRequest.requestHeader,
                queryParams = webhookRequest.requestParam,
                body = webhookRequest.requestBody
            )
        ).data ?: run {
            logger.info("replay webhook request body is null|${replayEvent.replayRequestId}")
            return null
        }
        pipelineTriggerEventDao.updateEventBody(
            dslContext = dslContext,
            projectId = projectId,
            eventId = replayEvent.eventId,
            eventBody = ScmWebhookEventBody(
                headers = webhookRequest.requestHeader,
                queryParams = webhookRequest.requestParam,
                webhook = webhook
            )
        )
        if (webhook.skipCi()) {
            logger.info("skip this webhook request|scmCode:${repository.scmCode}")
            return null
        }
        return scmWebhookTriggerBuildService.trigger(
            projectId = projectId,
            pipelineId = pipelineId,
            version = null,
            eventId = replayEvent.eventId,
            repository = repository,
            webhook = webhook,
            eventTime = LocalDateTime.now()
        )
    }

    private fun replayBuildOldRepo(
        replayEvent: ReplayWebhookEvent,
        pipelineId: String,
        webhookRequest: RepositoryWebhookRequest,
        replayTriggerEvent: PipelineTriggerEvent
    ): String? {
        val scmType = replayEvent.scmType ?: return null
        val request = WebhookRequest(
            headers = webhookRequest.requestHeader,
            body = webhookRequest.requestBody
        )
        val event = webhookEventFactory.parseEvent(scmType = scmType, request = request) ?: run {
            logger.warn("Failed to parse webhook event")
            return null
        }
        val matcher = webhookEventFactory.createScmWebHookMatcher(scmType = scmType, event = event)
        val preMatch = matcher.preMatch()
        if (!preMatch.isMatch) {
            logger.info("webhook replay trigger pre match|${preMatch.reason}")
            return null
        }
        EventCacheUtil.initEventCache()
        try {
            return pipelineBuildWebhookTriggerService.triggerSinglePipeline(
                subscriber = WebhookTriggerPipeline(
                    projectId = replayEvent.projectId,
                    pipelineId = pipelineId
                ),
                matcher = matcher,
                triggerEvent = replayTriggerEvent,
                repoEventIdMap = ConcurrentHashMap()
            )
        } finally {
            EventCacheUtil.remove()
        }
    }

    private fun getRepository(projectId: String, repoHashId: String): Repository? {
        return try {
            client.get(ServiceRepositoryResource::class).get(
                projectId = projectId,
                repositoryId = repoHashId,
                repositoryType = RepositoryType.ID
            ).data
        } catch (ignored: Exception) {
            logger.warn("fail to get repository|projectId=$projectId|repoHashId=$repoHashId", ignored)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmTriggerReplayHandler::class.java)
    }
}
