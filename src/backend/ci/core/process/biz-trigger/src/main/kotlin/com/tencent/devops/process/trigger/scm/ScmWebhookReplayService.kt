package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.process.pojo.trigger.ScmWebhookEventBody
import com.tencent.devops.process.trigger.dao.PipelineTriggerEventDao
import com.tencent.devops.process.trigger.event.ReplayWebhookEvent
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.ServiceRepositoryWebhookResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.webhook.WebhookParseRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 代码库触发事件异步重放，pipelineId 为空时重放事件下的全部流水线
 */
@Service
class ScmWebhookReplayService(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineTriggerEventDao: PipelineTriggerEventDao,
    private val grayService: WebhookGrayService,
    private val scmWebhookEventFactory: ScmWebhookEventFactory,
    private val webhookManager: WebhookManager,
    private val pipelineBuildWebhookMatchService: PipelineBuildWebhookMatchService
) {

    fun replay(replayEvent: ReplayWebhookEvent) {
        with(replayEvent) {
            val triggerEvent = pipelineTriggerEventDao.getTriggerEvent(
                dslContext = dslContext,
                projectId = projectId,
                eventId = eventId
            ) ?: run {
                logger.info("replay trigger event not found|$eventId")
                return
            }
            val webhookRequest = client.get(ServiceRepositoryWebhookResource::class).getWebhookRequest(
                requestId = triggerEvent.requestId
            ).data ?: run {
                logger.info("replay webhook request not found|$replayRequestId")
                return
            }
            val repository = triggerEvent.eventSource?.let {
                getRepository(
                    projectId = projectId,
                    repoHashId = it
                )
            } ?: return
            val grayRepo = grayService.isGrayRepo(repository.scmCode, repository.projectName)
            if (grayRepo) {
                logger.info("The current replay event will execute the new trigger logic")
                val webhook = client.get(ServiceRepositoryWebhookResource::class).webhookParseByRepo(
                    scmCode = repository.scmCode,
                    projectId = projectId,
                    repoHashId = repository.repoHashId!!,
                    request = WebhookParseRequest(
                        requestId = triggerEvent.requestId,
                        headers = webhookRequest.requestHeader,
                        queryParams = webhookRequest.requestParam,
                        body = webhookRequest.requestBody
                    )
                ).data ?: run {
                    logger.info("replay webhook request body is null|$replayRequestId")
                    return
                }
                val eventBody = ScmWebhookEventBody(
                    headers = webhookRequest.requestHeader,
                    queryParams = webhookRequest.requestParam,
                    webhook = webhook
                )
                pipelineTriggerEventDao.updateEventBody(
                    dslContext = dslContext,
                    projectId = projectId,
                    eventId = eventId,
                    eventBody = eventBody
                )
                webhookManager.fireEvent(
                    eventId = triggerEvent.eventId!!,
                    eventTime = LocalDateTime.now(),
                    repository = repository,
                    webhook = webhook,
                    replayPipelineId = pipelineId
                )
            } else {
                val replayScmType = scmType ?: run {
                    logger.warn("replay event scmType is empty|$eventId")
                    return
                }
                val request = WebhookRequest(
                    headers = webhookRequest.requestHeader,
                    body = webhookRequest.requestBody
                )
                val event = scmWebhookEventFactory.parseEvent(scmType = replayScmType, request = request) ?: run {
                    logger.warn("Failed to parse webhook event")
                    return
                }
                val matcher = scmWebhookEventFactory.createScmWebHookMatcher(scmType = replayScmType, event = event)

                pipelineBuildWebhookMatchService.replay(
                    replayEvent = replayEvent,
                    triggerEvent = triggerEvent,
                    matcher = matcher
                )
            }
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
        private val logger = LoggerFactory.getLogger(ScmWebhookReplayService::class.java)
    }
}
