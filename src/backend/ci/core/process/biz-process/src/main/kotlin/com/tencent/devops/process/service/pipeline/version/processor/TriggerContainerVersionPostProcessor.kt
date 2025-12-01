package com.tencent.devops.process.service.pipeline.version.processor

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscription
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerContainerVersionPostProcessor @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val pipelineRepositoryService: PipelineRepositoryService
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
        val params = pipelineRepositoryService.getTriggerParams(triggerContainer)
        triggerContainer.elements.forEach { element ->
            when (element) {
                is MarketBuildLessAtomElement -> {
                    saveEventSubscription(
                        userId = context.userId,
                        projectId = pipelineResourceVersion.projectId,
                        pipelineId = pipelineResourceVersion.pipelineId,
                        channelCode = context.pipelineBasicInfo.channelCode,
                        element = element,
                        params = params
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
        element: MarketBuildLessAtomElement,
        params: Map<String, String>
    ) {
        val inputMap = element.data["input"] as Map<String, Any>
        // TODO: 如何跟envId对应起来?
        val eventSource = inputMap["ci.event.source"]?.toString()
        val eventType = inputMap["ci.event.type"]?.toString()
        if (!eventSource.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL
            )
        }
        if (!eventType.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL
            )
        }
        val eventSubscription = PipelineEventSubscription(
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = element.id!!,
            eventCode = element.atomCode,
            eventSource = eventSource!!,
            eventType = eventType!!,
            channelCode = channelCode
        )
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
