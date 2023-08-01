/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineWebhookEvent
import com.tencent.devops.model.process.tables.records.TPipelineWebhookEventRecord
import com.tencent.devops.process.pojo.webhook.PipelineWebhookEvent
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class PipelineWebhookEventDao {

    fun save(dslContext: DSLContext, webhookEvent: PipelineWebhookEvent) {
        with(TPipelineWebhookEvent.T_PIPELINE_WEBHOOK_EVENT) {
            dslContext.insertInto(this).columns(
                PROJECT_ID,
                REQUEST_ID,
                EVENT_ID,
                TRIGGER_TYPE,
                EVENT_SOURCE,
                EVENT_TYPE,
                TRIGGER_USER,
                EVENT_MESSAGE,
                EVENT_DESC,
                EVENT_TIME,
                TASK_ATOM
            ).values(
                webhookEvent.projectId,
                webhookEvent.requestId,
                webhookEvent.eventId,
                webhookEvent.triggerType,
                webhookEvent.eventSource!!,
                webhookEvent.eventType,
                webhookEvent.triggerUser,
                webhookEvent.eventMessage,
                webhookEvent.eventDesc,
                webhookEvent.eventTime,
                webhookEvent.taskAtom
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        eventSource: String,
        triggerType: String?,
        eventType: String?,
        triggerUser: String?,
        startTime: Long?,
        endTime: Long?,
        limit: Int,
        offset: Int
    ): List<PipelineWebhookEvent> {
        return with(TPipelineWebhookEvent.T_PIPELINE_WEBHOOK_EVENT) {
            val conditions = buildConditions(
                projectId = projectId,
                eventSource = eventSource,
                triggerType = triggerType,
                triggerUser = triggerUser,
                eventType = eventType,
                startTime = startTime,
                endTime = endTime
            )
            dslContext.selectFrom(this)
                .where(conditions)
                .limit(limit)
                .offset(offset)
                .fetch {
                    convert(it)
                }
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        eventSource: String,
        triggerType: String?,
        eventType: String?,
        triggerUser: String?,
        startTime: Long?,
        endTime: Long?,
    ): Long {
        return with(TPipelineWebhookEvent.T_PIPELINE_WEBHOOK_EVENT) {
            val conditions = buildConditions(
                projectId = projectId,
                eventSource = eventSource,
                triggerType = triggerType,
                triggerUser = triggerUser,
                eventType = eventType,
                startTime = startTime,
                endTime = endTime
            )
            dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun get(
        dslContext: DSLContext,
        eventId: Long,
        eventSource: String
    ): PipelineWebhookEvent? {
        val record = with(TPipelineWebhookEvent.T_PIPELINE_WEBHOOK_EVENT) {
            dslContext.selectFrom(this)
                .where(EVENT_ID.eq(eventId))
                .and(EVENT_SOURCE.eq(eventSource))
                .fetchOne()
        }
        return record?.let { convert(it) }
    }

    private fun TPipelineWebhookEvent.buildConditions(
        projectId: String,
        eventSource: String? = null,
        eventId: Long? = null,
        eventType: String? = null,
        triggerUser: String? = null,
        triggerType: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(PROJECT_ID.eq(projectId))
        if (!eventSource.isNullOrBlank()) {
            conditions.add(EVENT_SOURCE.eq(eventSource))
        }
        if (eventId != null) {
            conditions.add(EVENT_ID.eq(eventId))
        }
        if (!eventType.isNullOrBlank()) {
            conditions.add(EVENT_TYPE.eq(eventType))
        }
        if (!triggerUser.isNullOrBlank()) {
            conditions.add(TRIGGER_USER.eq(triggerUser))
        }
        if (!triggerType.isNullOrBlank()) {
            conditions.add(TRIGGER_TYPE.eq(triggerType))
        }
        if (startTime != null && startTime > 0) {
            conditions.add(EVENT_TIME.ge(Timestamp(startTime).toLocalDateTime()))
        }
        if (endTime != null && endTime > 0) {
            conditions.add(EVENT_TIME.le(Timestamp(endTime).toLocalDateTime()))
        }
        return conditions
    }

    fun convert(record: TPipelineWebhookEventRecord): PipelineWebhookEvent {
        return with(record) {
            PipelineWebhookEvent(
                projectId = projectId,
                requestId = requestId,
                eventId = eventId,
                triggerType = triggerType,
                eventSource = eventSource,
                eventType = eventType,
                triggerUser = triggerUser,
                eventMessage = eventMessage,
                eventDesc = eventDesc,
                eventTime = eventTime,
                taskAtom = taskAtom
            )
        }
    }
}
