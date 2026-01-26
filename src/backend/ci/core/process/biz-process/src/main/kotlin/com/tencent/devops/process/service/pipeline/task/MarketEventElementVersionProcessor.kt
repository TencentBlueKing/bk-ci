package com.tencent.devops.process.service.pipeline.task

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketEventAtomElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerTriggerTaskService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscription
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.processor.TriggerContainerVersionPostProcessor
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.pojo.common.BK_STORE_COMMON_TRIGGER
import com.tencent.devops.store.pojo.common.BK_STORE_CREATIVE_STREAM_TIMER_TRIGGER
import com.tencent.devops.store.pojo.common.KEY_ADVANCE_EXPRESSION
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_START_PARAMS
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_SOURCE
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_TYPE
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_TARGET
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.trigger.enums.TriggerTargetEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class MarketEventElementVersionProcessor @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val client: Client,
    private val pipelineTimerService: PipelineTimerService,
    @Lazy
    private val pipelineTimerTriggerTaskService: PipelineTimerTriggerTaskService
) : PipelineTaskVersionProcessor {

    override fun postProcessAfterSave(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting,
        element: Element,
        variables: Map<String, String>
    ) {
        handleTrigger(
            userId = context.userId,
            projectId = pipelineResourceVersion.projectId,
            pipelineId = pipelineResourceVersion.pipelineId,
            channelCode = context.pipelineBasicInfo.channelCode,
            element = element as MarketEventAtomElement,
            variables = variables,
            pipelineSetting = pipelineSetting
        )
    }

    fun handleTrigger(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineSetting: PipelineSetting,
        channelCode: ChannelCode,
        element: MarketEventAtomElement,
        variables: Map<String, String>
    ) {
        val atomCode = element.atomCode
        val version = element.version
        val componentDetail = client.get(ServiceStoreComponentResource::class).getComponentDataInfoByCode(
            storeType = StoreTypeEnum.TRIGGER_EVENT,
            storeCode = atomCode,
            version = version
        ).data ?: throw InvalidParamException("component[$atomCode@$version] not found")
        when (componentDetail.ownerStoreCode) {
            BK_STORE_COMMON_TRIGGER -> handleCommonTrigger(
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                element = element,
                storeCode = componentDetail.storeCode,
                variables = variables,
                userId = userId
            )

            else -> handleCustomTrigger(
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                element = element,
                variables = variables,
                componentDetail = componentDetail,
                userId = userId,
                pipelineSetting = pipelineSetting
            )
        }
    }

    /**
     * 通用事件
     */
    private fun handleCommonTrigger(
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        element: MarketEventAtomElement,
        variables: Map<String, String>,
        storeCode: String,
        userId: String
    ) {
        when (storeCode) {
            BK_STORE_CREATIVE_STREAM_TIMER_TRIGGER -> {
                logger.info("$projectId|$pipelineId|save timer trigger")
                val inputMap = element.data[KEY_INPUT] as Map<String, Any>
                val advanceExpression = inputMap[KEY_ADVANCE_EXPRESSION] as String?
                if (advanceExpression.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ILLEGAL_TIMER_CRONTAB
                    )
                }
                val expressions = pipelineTimerTriggerTaskService.convertAdvanceExpression(
                    advanceExpression = listOf(advanceExpression),
                    params = variables
                )
                val startParam = (inputMap[KEY_START_PARAMS] as String?)?.let {
                    if (it.isNotBlank()) {
                        JsonUtil.to(it, object : TypeReference<List<Map<String, Any>>>() {})
                    } else {
                        null
                    }
                }?.filter { it.containsKey("key") }
                        ?.associate { it["key"].toString() to (it["value"]?.toString() ?: "") }
                pipelineTimerService.saveTimer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    channelCode = channelCode,
                    userId = userId,
                    branchs = null,
                    noScm = null,
                    repoHashId = null,
                    crontabExpressions = expressions,
                    startParam = startParam,
                    taskId = element.id ?: ""
                )
            }

            else -> {
                logger.warn("skip|unknown common trigger[$storeCode]")
            }
        }
    }

    /**
     * 自定义触发事件
     */
    private fun handleCustomTrigger(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineSetting: PipelineSetting,
        channelCode: ChannelCode,
        element: MarketEventAtomElement,
        variables: Map<String, String>,
        componentDetail: StoreDetailInfo
    ) {
        val triggerTarget = componentDetail.extData?.get(KEY_TRIGGER_TARGET)?.toString()
        val eventType = componentDetail.storeCode.substringAfter("${componentDetail.ownerStoreCode}-")
        val eventSubscription = when (triggerTarget) {
            TriggerTargetEnum.CREATIVE.name -> {
                PipelineEventSubscription(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = element.id!!,
                    eventCode = element.atomCode,
                    eventSource = "", // pipelineSetting.envName
                    eventType = eventType,
                    channelCode = channelCode,
                    triggerTarget = TriggerTargetEnum.CREATIVE
                )
            }

            TriggerTargetEnum.PIPELINE.name -> {
                val inputMap = element.data[KEY_INPUT] as Map<String, Any>
                val eventSource = inputMap[KEY_TRIGGER_EVENT_TYPE]?.toString()?.let {
                    EnvUtils.parseEnv(it, variables)
                } ?: throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf(KEY_TRIGGER_EVENT_TYPE)
                )
                val eventType = inputMap[KEY_TRIGGER_EVENT_SOURCE]?.toString()?.let {
                    EnvUtils.parseEnv(it, variables)
                } ?: throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf(KEY_TRIGGER_EVENT_TYPE)
                )
                PipelineEventSubscription(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = element.id!!,
                    eventCode = element.atomCode,
                    eventSource = eventSource,
                    eventType = eventType,
                    channelCode = channelCode,
                    triggerTarget = TriggerTargetEnum.PIPELINE
                )
            }

            else -> {
                logger.warn("unknown trigger target: $triggerTarget")
                return
            }
        }
        pipelineEventSubscriptionDao.save(
            dslContext = dslContext,
            userId = userId,
            subscription = eventSubscription
        )
    }

    override fun support(element: Element) = element is MarketEventAtomElement

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerContainerVersionPostProcessor::class.java)
    }
}