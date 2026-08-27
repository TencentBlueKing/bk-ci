package com.tencent.devops.process.service.pipeline.task

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.pojo.webhook.BkRepoAssociationType
import com.tencent.devops.common.archive.pojo.webhook.BkRepoEventType
import com.tencent.devops.common.archive.pojo.webhook.BkRepoWebhookCreateRequest
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactRepositoryType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscription
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_CUSTOM
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_IMAGE
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_PIPELINE
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.store.pojo.trigger.enums.TriggerTargetEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 制品到达触发器版本处理器
 *
 * 保存流水线时：
 * 1. 幂等写入/删除 T_PIPELINE_EVENT_SUBSCRIPTION（核心，反查订阅关系）；
 * 2. 保证 bkrepo 项目级 webhook 已指向本平台回调地址（未注册则注册）。
 */
@Service
class ArtifactTriggerElementVersionProcessor @Autowired constructor(
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val bkRepoClient: BkRepoClient
) : PipelineTaskVersionProcessor {

    @Value("\${external.webhook.artifact.callbackUrl:}")
    private val callbackUrl: String = ""

    @Value("\${external.webhook.artifact.secret:#{null}}")
    private val webhookSecret: String? = null

    override fun support(element: Element): Boolean = element is ArtifactTriggerElement

    override fun postProcessAfterSave(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting,
        element: Element,
        variables: Map<String, String>
    ) {
        val triggerElement = element as ArtifactTriggerElement
        val projectId = pipelineResourceVersion.projectId
        val pipelineId = pipelineResourceVersion.pipelineId
        val taskId = triggerElement.id ?: run {
            logger.warn("skip register artifact subscription|element id is null|$projectId|$pipelineId")
            return
        }
        if (!triggerElement.elementEnabled()) {
            logger.info("artifact trigger disabled, remove subscription|$projectId|$pipelineId|$taskId")
            pipelineEventSubscriptionDao.delete(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId
            )
            return
        }
        val input = triggerElement.data.input
        val repository = input.repository
        val eventType = input.eventType.name
        val eventSource = buildEventSource(
            projectId = projectId,
            repository = repository
        )
        val triggers = buildTriggers(repository = repository)
        // 先保证 bkrepo webhook 注册到位，再落库订阅关系
        createBkRepoWebhook(
            userId = context.userId,
            projectId = projectId,
            associationId = eventSource,
            triggers = triggers
        )
        val subscription = PipelineEventSubscription(
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = taskId,
            eventCode = ArtifactTriggerElement.classType,
            eventSource = eventSource,
            eventType = eventType,
            channelCode = context.pipelineBasicInfo.channelCode,
            triggerTarget = TriggerTargetEnum.PIPELINE
        )
        pipelineEventSubscriptionDao.save(
            dslContext = transactionContext,
            userId = context.userId,
            subscription = subscription
        )
        logger.info(
            "register artifact subscription success|$projectId|$pipelineId|$taskId|" +
                "eventSource=$eventSource|eventType=$eventType"
        )
    }

    /**
     * 保证 bkrepo 项目级 webhook 已指向本平台回调地址。
     */
    private fun createBkRepoWebhook(
        userId: String,
        projectId: String,
        associationId: String,
        triggers: List<BkRepoEventType>
    ) {
        if (callbackUrl.isBlank()) {
            logger.info("artifact webhook callbackUrl not configured, skip bkrepo webhook register|$projectId")
            return
        }
        val associationType = BkRepoAssociationType.REPO
        // 汇总已指向本平台回调地址的 webhook 所覆盖的事件类型(可能分散在多条上)
        val existedTriggers = invokeBkRepoApi("listWebhooks") {
            bkRepoClient.listWebhooks(
                userId = userId,
                projectId = projectId,
                associationType = associationType,
                associationId = associationId
            )
        }.filter { it.url == callbackUrl }
            .flatMap { it.triggers }
            .toSet()
        // 本次需要但尚未注册的事件类型；已全部覆盖则无需处理
        val toAddTriggers = triggers.filter { it !in existedTriggers }
        if (toAddTriggers.isEmpty()) {
            return
        }
        invokeBkRepoApi("createWebhook") {
            bkRepoClient.createWebhook(
                userId = userId,
                projectId = projectId,
                createRequest = BkRepoWebhookCreateRequest(
                    url = callbackUrl,
                    headers = buildWebhookHeaders(),
                    triggers = toAddTriggers,
                    associationType = associationType,
                    associationId = associationId
                )
            )
        }
        logger.info("register bkrepo artifact webhook success|$projectId|$associationId|triggers=$toAddTriggers")
    }

    private fun <T> invokeBkRepoApi(method: String, action: () -> T): T {
        try {
            return action()
        } catch (exception: RemoteServiceException) {
            logger.info("Failed to invoke bkrepo api|$method|${exception.httpStatus}", exception)
            throw ErrorCodeException(
                statusCode = exception.httpStatus,
                errorCode = ProcessMessageCode.ERROR_ARTIFACT_BKREPO_API,
                params = arrayOf(method, exception.message ?: "")
            )
        }
    }

    /**
     * 注册 webhook 时下发密钥请求头；bkrepo 回调会带回，用于校验来源。未配置则不下发。
     */
    private fun buildWebhookHeaders(): Map<String, String> {
        return if (webhookSecret.isNullOrBlank()) {
            emptyMap()
        } else {
            mapOf(ArtifactWebhookConstant.HEADER_BKREPO_WEBHOOK_SECRET to webhookSecret)
        }
    }

    private fun buildTriggers(
        repository: ArtifactRepositoryType
    ): List<BkRepoEventType> {
        return when (repository) {
            ArtifactRepositoryType.IMAGE -> listOf(BkRepoEventType.VERSION_CREATED)
            else -> listOf(BkRepoEventType.NODE_CREATED)
        }
    }

    private fun buildEventSource(
        projectId: String,
        repository: ArtifactRepositoryType
    ): String {
        return when (repository) {
            ArtifactRepositoryType.PIPELINE -> "$projectId:$REPO_PIPELINE"
            ArtifactRepositoryType.CUSTOM -> "$projectId:$REPO_CUSTOM"
            ArtifactRepositoryType.IMAGE -> "$projectId:$REPO_IMAGE"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactTriggerElementVersionProcessor::class.java)
    }
}
