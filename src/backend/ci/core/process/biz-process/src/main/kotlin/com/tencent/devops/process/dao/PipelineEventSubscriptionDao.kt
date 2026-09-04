package com.tencent.devops.process.dao

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.model.process.tables.TPipelineEventSubscription
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscription
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineEventSubscriptionDao {

    fun save(
        dslContext: DSLContext,
        userId: String,
        subscription: PipelineEventSubscription
    ) {
        val now = LocalDateTime.now()
        with(TPipelineEventSubscription.T_PIPELINE_EVENT_SUBSCRIPTION) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                TASK_ID,
                EVENT_CODE,
                EVENT_SOURCE,
                EVENT_TYPE,
                EVENT_SCOPE,
                TRIGGER_TARGET,
                CHANNEL,
                CREATOR,
                CREATE_TIME,
                MODIFIER,
                UPDATE_TIME
            ).values(
                subscription.projectId,
                subscription.pipelineId,
                subscription.taskId,
                subscription.eventCode,
                subscription.eventSource,
                subscription.eventType,
                subscription.eventScope,
                subscription.triggerTarget.name,
                subscription.channelCode.name,
                userId,
                now,
                userId,
                now
            ).onDuplicateKeyUpdate()
                .set(EVENT_CODE, subscription.eventCode)
                .set(EVENT_SOURCE, subscription.eventSource)
                .set(EVENT_TYPE, subscription.eventType)
                .set(EVENT_SCOPE, subscription.eventScope)
                .set(TRIGGER_TARGET, subscription.triggerTarget.name)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, now)
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        taskId: String
    ) {
        with(TPipelineEventSubscription.T_PIPELINE_EVENT_SUBSCRIPTION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(TASK_ID.eq(taskId))
                .execute()
        }
    }

    fun listSubscribedTaskIds(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        eventSource: String,
        eventType: String,
        eventCode: String,
        eventScopes: List<String>? = null
    ): Set<String> {
        with(TPipelineEventSubscription.T_PIPELINE_EVENT_SUBSCRIPTION) {
            val step = dslContext.select(TASK_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(EVENT_SOURCE.eq(eventSource))
                .and(EVENT_TYPE.eq(eventType))
                .and(EVENT_CODE.eq(eventCode))
            if (!eventScopes.isNullOrEmpty()) {
                step.and(EVENT_SCOPE.`in`(eventScopes))
            }
            return step.fetch().mapNotNull { it.value1() }.toSet()
        }
    }

    fun listEventSubscriber(
        dslContext: DSLContext,
        eventSource: String,
        eventType: String,
        eventCode: String,
        eventScopes: List<String>? = null
    ): List<PipelineEventSubscriber> {
        with(TPipelineEventSubscription.T_PIPELINE_EVENT_SUBSCRIPTION) {
            val step = dslContext.select(PROJECT_ID, PIPELINE_ID, CHANNEL)
                .from(this)
                .where(EVENT_SOURCE.eq(eventSource))
                .and(EVENT_TYPE.eq(eventType))
                .and(EVENT_CODE.eq(eventCode))
            if (!eventScopes.isNullOrEmpty()) {
                step.and(EVENT_SCOPE.`in`(eventScopes))
            }
            return step.fetch().map {
                PipelineEventSubscriber(
                    projectId = it.value1(),
                    pipelineId = it.value2(),
                    channelCode = ChannelCode.valueOf(it.value3())
                )
            }
        }
    }
}
