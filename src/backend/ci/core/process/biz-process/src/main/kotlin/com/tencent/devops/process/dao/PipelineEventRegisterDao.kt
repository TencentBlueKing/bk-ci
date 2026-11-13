package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineEventRegister
import com.tencent.devops.process.pojo.trigger.PipelineEventRegister
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineEventRegisterDao {

    fun count(
        dslContext: DSLContext,
        projectId: String,
        eventCode: String,
        eventSource: String,
        eventType: String,
        eventScopes: List<String>? = null,
        callbackUrl: String
    ): Int {
        with(TPipelineEventRegister.T_PIPELINE_EVENT_REGISTER) {
            val step = dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(EVENT_CODE.eq(eventCode))
                .and(EVENT_SOURCE.eq(eventSource))
                .and(EVENT_TYPE.eq(eventType))
                .and(CALLBACK_URL.eq(callbackUrl))
            if (!eventScopes.isNullOrEmpty()) {
                step.and(EVENT_SCOPE.`in`(eventScopes))
            }
            return step.fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun save(
        dslContext: DSLContext,
        userId: String,
        id: Long,
        register: PipelineEventRegister
    ) {
        val now = LocalDateTime.now()
        with(TPipelineEventRegister.T_PIPELINE_EVENT_REGISTER) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                EVENT_CODE,
                EVENT_SOURCE,
                EVENT_TYPE,
                EVENT_SCOPE,
                CALLBACK_URL,
                EXTERNAL_ID,
                CREATOR,
                CREATE_TIME,
                MODIFIER,
                UPDATE_TIME
            ).values(
                id,
                register.projectId,
                register.eventCode,
                register.eventSource,
                register.eventType,
                register.eventScope.orEmpty(),
                register.callbackUrl,
                register.externalId,
                userId,
                now,
                userId,
                now
            ).onDuplicateKeyUpdate()
                .set(CALLBACK_URL, register.callbackUrl)
                .set(EXTERNAL_ID, register.externalId)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, now)
                .execute()
        }
    }
}
