package com.tencent.devops.process.service.pipeline.task

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketEventAtomElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscription
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.processor.TriggerContainerVersionPostProcessor
import com.tencent.devops.store.api.common.ServiceStoreComponentBaseResource
import com.tencent.devops.store.pojo.common.BK_STORE_COMMON_TRIGGER
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_SOURCE
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_TYPE
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_TARGET
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.trigger.enums.TriggerTargetEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MarketEventElementVersionProcessor @Autowired constructor(
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val client: Client
) : PipelineTaskVersionProcessor {

    override fun postProcessBeforeSave(
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting,
        element: Element,
        variables: Map<String, String>
    ) {
        // 保存版本资源之前进行内容校验
        checkTrigger(
            element = element as MarketEventAtomElement,
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
        // 保存触发事件关联信息
        saveTrigger(
            userId = context.userId,
            projectId = pipelineResourceVersion.projectId,
            pipelineId = pipelineResourceVersion.pipelineId,
            channelCode = context.pipelineBasicInfo.channelCode,
            element = element as MarketEventAtomElement,
            variables = variables,
            pipelineSetting = pipelineSetting,
            transactionContext = transactionContext
        )
    }

    /**
     * 校验触发器相关逻辑
     */
    fun checkTrigger(
        element: MarketEventAtomElement,
        variables: Map<String, String>
    ) {
        val atomCode = element.atomCode
        val version = element.version
        val componentDetail = getComponentDetail(atomCode, version) ?: run {
            logger.warn("component[$atomCode@$version] not found, skip check")
            return
        }
        when (componentDetail.ownerStoreCode) {
            BK_STORE_COMMON_TRIGGER -> checkCommonTrigger(
                element = element,
                storeCode = componentDetail.storeCode,
                variables = variables
            )
            // 自定义触发事件暂无需额外校验
            else -> Unit
        }
    }

    /**
     * 保存触发器相关逻辑
     */
    fun saveTrigger(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineSetting: PipelineSetting,
        channelCode: ChannelCode,
        element: MarketEventAtomElement,
        variables: Map<String, String>,
        transactionContext: DSLContext
    ) {
        val atomCode = element.atomCode
        val version = element.version
        val componentDetail = getComponentDetail(atomCode, version) ?: run {
            logger.warn("component[$atomCode@$version] not found, skip handle")
            return
        }
        when (componentDetail.ownerStoreCode) {
            BK_STORE_COMMON_TRIGGER -> saveCommonTrigger(
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                element = element,
                storeCode = componentDetail.storeCode,
                variables = variables,
                userId = userId,
                transactionContext = transactionContext
            )

            else -> saveCustomTrigger(
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                element = element,
                variables = variables,
                componentDetail = componentDetail,
                userId = userId,
                pipelineSetting = pipelineSetting,
                transactionContext = transactionContext
            )
        }
    }

    private fun getComponentDetail(
        atomCode: String,
        version: String
    ) = try {
        client.get(ServiceStoreComponentBaseResource::class).getComponentDataInfoByCode(
            storeType = StoreTypeEnum.TRIGGER_EVENT,
            storeCode = atomCode,
            version = version
        ).data
    } catch (ignored: Exception) {
        logger.warn("fail to get component[$atomCode@$version] detail", ignored)
        null
    }

    /**
     * 通用事件校验
     */
    private fun checkCommonTrigger(
        element: MarketEventAtomElement,
        variables: Map<String, String>,
        storeCode: String
    ) {
        logger.warn("skip|unknown common trigger[$storeCode]")
    }

    /**
     * 通用事件保存
     */
    private fun saveCommonTrigger(
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        element: MarketEventAtomElement,
        variables: Map<String, String>,
        storeCode: String,
        userId: String,
        transactionContext: DSLContext
    ) {
        logger.warn("skip|unknown common trigger[$storeCode]")
    }

    /**
     * 自定义触发事件保存
     */
    private fun saveCustomTrigger(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineSetting: PipelineSetting,
        channelCode: ChannelCode,
        element: MarketEventAtomElement,
        variables: Map<String, String>,
        componentDetail: StoreDetailInfo,
        transactionContext: DSLContext
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
                    eventSource = pipelineSetting.envHashId ?: "",
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
            dslContext = transactionContext,
            userId = userId,
            subscription = eventSubscription
        )
    }

    override fun support(element: Element) = element is MarketEventAtomElement

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerContainerVersionPostProcessor::class.java)
    }
}
