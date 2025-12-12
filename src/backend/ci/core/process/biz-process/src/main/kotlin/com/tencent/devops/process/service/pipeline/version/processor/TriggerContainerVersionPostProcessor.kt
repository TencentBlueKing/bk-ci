package com.tencent.devops.process.service.pipeline.version.processor

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.element.market.MarketEventAtomElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscription
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_SOURCE
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_TYPE
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_TARGET
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.trigger.enums.TriggerTargetEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerContainerVersionPostProcessor @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client
) : PipelineVersionCreatePostProcessor {

    override fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        if (pipelineResourceVersion.status != VersionStatus.RELEASED) {
            logger.warn(
                "pipeline version[${pipelineResourceVersion.status}] is not released, " +
                        "skip trigger container version post processor"
            )
            return
        }
        val triggerContainer = pipelineResourceVersion.model.getTriggerContainer()
        val variables = pipelineRepositoryService.getTriggerParams(triggerContainer)
        triggerContainer.elements.forEach { element ->
            when (element) {
                is MarketEventAtomElement -> {
                    saveEventSubscription(
                        userId = context.userId,
                        projectId = pipelineResourceVersion.projectId,
                        pipelineId = pipelineResourceVersion.pipelineId,
                        channelCode = context.pipelineBasicInfo.channelCode,
                        element = element,
                        variables = variables
                    )
                }
            }
        }
    }

    private fun saveEventSubscription(
        userId: String,
        projectId: String,
        pipelineId: String,
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
        val triggerTarget = componentDetail.extData?.get(KEY_TRIGGER_TARGET)?.toString()
        val eventSubscription = when (triggerTarget) {
            TriggerTargetEnum.CREATIVE.name -> {
                PipelineEventSubscription(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = element.id!!,
                    eventCode = element.atomCode,
                    eventSource = "",
                    eventType = "",
                    channelCode = channelCode
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
                    channelCode = channelCode
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

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerContainerVersionPostProcessor::class.java)
    }
}
