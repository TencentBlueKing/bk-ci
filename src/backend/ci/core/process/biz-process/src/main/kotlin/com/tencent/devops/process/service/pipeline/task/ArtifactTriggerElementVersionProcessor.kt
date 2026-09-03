package com.tencent.devops.process.service.pipeline.task

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.pojo.webhook.BkRepoAssociationType
import com.tencent.devops.common.archive.pojo.webhook.BkRepoEventType
import com.tencent.devops.common.archive.pojo.webhook.BkRepoWebhookCreateRequest
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerInput
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactRepositoryType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineEventRegister
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscription
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_CUSTOM
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_IMAGE
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactWebhookConstant.REPO_PIPELINE
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.trigger.PipelineEventRegisterService
import com.tencent.devops.process.trigger.artifact.ArtifactWebhookUtils
import com.tencent.devops.process.utils.PipelineVarUtil
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
 * 1. 保存前校验必填项（事务外）；
 * 2. 保证源侧 webhook 已注册（流水线/自定义 PATH，镜像 REPO）；
 * 3. 幂等写入/删除 T_PIPELINE_EVENT_SUBSCRIPTION。
 */
@Service
class ArtifactTriggerElementVersionProcessor @Autowired constructor(
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val pipelineEventRegisterService: PipelineEventRegisterService,
    private val bkRepoClient: BkRepoClient
) : PipelineTaskVersionProcessor {

    @Value("\${external.webhook.artifact.callbackUrl:}")
    private val callbackUrl: String = ""

    @Value("\${external.webhook.artifact.secret:#{null}}")
    private val webhookSecret: String? = null

    override fun support(element: Element): Boolean = element is ArtifactTriggerElement

    override fun postProcessBeforeSave(
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting,
        element: Element,
        variables: Map<String, String>
    ) {
        val triggerElement = element as ArtifactTriggerElement
        if (!triggerElement.elementEnabled()) {
            return
        }
        validateInput(
            elementName = triggerElement.name,
            input = triggerElement.data.input,
            variables = variables
        )
    }

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
        val eventSource = buildEventSource(projectId = projectId, repository = repository)
        val triggers = buildTriggers(repository = repository)
        val eventScope = buildEventScope(input = input, variables = variables)
        val (associationType, associationId) = buildAssociation(
            projectId = projectId,
            repository = repository,
            path = eventScope
        )
        if (callbackUrl.isNotBlank()) {
            pipelineEventRegisterService.saveIfAbsent(
                userId = context.userId,
                register = PipelineEventRegister(
                    projectId = projectId,
                    eventCode = ArtifactTriggerElement.classType,
                    eventSource = eventSource,
                    eventType = eventType,
                    eventScope = eventScope,
                    callbackUrl = callbackUrl
                )
            ) {
                createBkRepoWebhook(
                    userId = context.userId,
                    projectId = projectId,
                    associationType = associationType,
                    associationId = associationId,
                    triggers = triggers
                )
                null
            }
        }
        val subscription = PipelineEventSubscription(
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = taskId,
            eventCode = ArtifactTriggerElement.classType,
            eventSource = eventSource,
            eventType = eventType,
            eventScope = eventScope,
            channelCode = context.pipelineBasicInfo.channelCode,
            triggerTarget = if (context.pipelineBasicInfo.channelCode == ChannelCode.CREATIVE_STREAM) {
                TriggerTargetEnum.CREATIVE
            } else {
                TriggerTargetEnum.PIPELINE
            }
        )
        pipelineEventSubscriptionDao.save(
            dslContext = transactionContext,
            userId = context.userId,
            subscription = subscription
        )
        logger.info(
            "register artifact subscription success|$projectId|$pipelineId|$taskId|" +
                "eventSource=$eventSource|eventType=$eventType|eventScope=$eventScope"
        )
    }

    private fun validateInput(
        elementName: String,
        input: ArtifactTriggerInput,
        variables: Map<String, String>
    ) {
        when (input.repository) {
            ArtifactRepositoryType.PIPELINE ->
                validateWatchPath(
                    elementName = elementName,
                    fieldCode = ProcessMessageCode.BK_ARTIFACT_TRIGGER_FIELD_WATCH_PIPELINE,
                    raw = input.watchPipeline,
                    variables = variables
                )
            ArtifactRepositoryType.CUSTOM ->
                validateWatchPath(
                    elementName = elementName,
                    fieldCode = ProcessMessageCode.BK_ARTIFACT_TRIGGER_FIELD_WATCH_ROOT_PATH,
                    raw = input.watchRootPath,
                    variables = variables
                )
            ArtifactRepositoryType.IMAGE ->
                resolveRequiredParam(
                    elementName = elementName,
                    fieldCode = ProcessMessageCode.BK_ARTIFACT_TRIGGER_FIELD_IMAGE,
                    raw = input.image,
                    variables = variables
                )
        }
    }

    private fun buildEventScope(
        input: ArtifactTriggerInput,
        variables: Map<String, String>
    ): String? {
        return when (input.repository) {
            ArtifactRepositoryType.PIPELINE -> ArtifactWebhookUtils.normalizePath(
                EnvUtils.parseEnv(input.watchPipeline, variables)
            )
            ArtifactRepositoryType.CUSTOM -> ArtifactWebhookUtils.normalizePath(
                EnvUtils.parseEnv(input.watchRootPath, variables)
            )
            ArtifactRepositoryType.IMAGE -> null
        }
    }

    private fun validateWatchPath(
        elementName: String,
        fieldCode: String,
        raw: String?,
        variables: Map<String, String>
    ) {
        ArtifactWebhookUtils.normalizePath(
            raw = resolveRequiredParam(elementName, fieldCode, raw, variables)
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_ARTIFACT_TRIGGER_ROOT_PATH,
            params = arrayOf(elementName)
        )
    }

    private fun resolveRequiredParam(
        elementName: String,
        fieldCode: String,
        raw: String?,
        variables: Map<String, String>
    ): String {
        val resolved = EnvUtils.parseEnv(raw, variables)
        if (resolved.isBlank() || PipelineVarUtil.isVar(resolved)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_ARTIFACT_TRIGGER_PARAM_INVALID,
                params = arrayOf(
                    elementName,
                    I18nUtil.getCodeLanMessage(messageCode = fieldCode, defaultMessage = fieldCode)
                )
            )
        }
        return resolved
    }

    /**
     * 保证 bkrepo webhook 已指向本平台回调地址。制品库不回写 EXTERNAL_ID。
     */
    private fun createBkRepoWebhook(
        userId: String,
        projectId: String,
        associationType: BkRepoAssociationType,
        associationId: String,
        triggers: List<BkRepoEventType>
    ) {
        val existedWebhooks = invokeBkRepoApi("listWebhooks") {
            bkRepoClient.listWebhooks(
                userId = userId,
                projectId = projectId,
                associationType = associationType,
                associationId = associationId
            )
        }.filter { it.url == callbackUrl }
        val existedTriggers = existedWebhooks.flatMap { it.triggers }.toSet()
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

    private fun buildWebhookHeaders(): Map<String, String> {
        return if (webhookSecret.isNullOrBlank()) {
            emptyMap()
        } else {
            mapOf(ArtifactWebhookConstant.HEADER_BKREPO_WEBHOOK_SECRET to webhookSecret)
        }
    }

    private fun buildAssociation(
        projectId: String,
        repository: ArtifactRepositoryType,
        path: String? = null
    ): Pair<BkRepoAssociationType, String> {
        return when (repository) {
            ArtifactRepositoryType.PIPELINE -> BkRepoAssociationType.PATH to
                BkRepoAssociationType.PATH.buildAssociationId(
                    projectId = projectId,
                    repoName = REPO_PIPELINE,
                    path = path
                )
            ArtifactRepositoryType.CUSTOM -> BkRepoAssociationType.PATH to
                BkRepoAssociationType.PATH.buildAssociationId(
                    projectId = projectId,
                    repoName = REPO_CUSTOM,
                    path = path
                )
            ArtifactRepositoryType.IMAGE -> BkRepoAssociationType.REPO to
                BkRepoAssociationType.REPO.buildAssociationId(
                    projectId = projectId,
                    repoName = REPO_IMAGE
                )
        }
    }

    private fun buildTriggers(repository: ArtifactRepositoryType): List<BkRepoEventType> {
        return when (repository) {
            ArtifactRepositoryType.IMAGE -> listOf(BkRepoEventType.VERSION_CREATED)
            else -> listOf(BkRepoEventType.NODE_CREATED)
        }
    }

    private fun buildEventSource(projectId: String, repository: ArtifactRepositoryType): String {
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
