package com.tencent.devops.process.service.pipeline.version.processor

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscription
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerContainerVersionPostProcessor @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao
) : PipelineVersionCreatePostProcessor {

    override fun postProcessInTransactionVersionCreate(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        val triggerContainer = pipelineResourceVersion.model.getTriggerContainer()
        val params = triggerContainer.params
        triggerContainer.elements.forEach { element ->
            when (element) {
                is MarketBuildLessAtomElement -> {
                    saveEventSubscription(
                        userId = context.userId,
                        projectId = pipelineResourceVersion.projectId,
                        pipelineId = pipelineResourceVersion.pipelineId,
                        channelCode = context.pipelineBasicInfo.channelCode,
                        element = element,
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
        element: MarketBuildLessAtomElement
    ) {
        val inputMap = element.data["input"] as Map<String, Any>
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
}
