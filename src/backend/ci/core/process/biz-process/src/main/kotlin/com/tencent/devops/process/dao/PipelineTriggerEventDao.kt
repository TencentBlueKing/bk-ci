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
 */

package com.tencent.devops.process.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.process.tables.TPipelineTriggerEvent
import com.tencent.devops.model.process.tables.records.TPipelineTriggerEventRecord
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.webhook.RepoWebhookEvent
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.`when`
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineTriggerEventDao {
    fun save(
        dslContext: DSLContext,
        triggerEvent: PipelineTriggerEvent
    ) {
        with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            dslContext.insertInto(this).columns(
                ID,
                PROJECT_ID,
                EVENT_ID,
                TRIGGER_TYPE,
                EVENT_SOURCE,
                EVENT_TYPE,
                TRIGGER_USER,
                EVENT_MESSAGE,
                EVENT_DESC,
                EVENT_TIME,
                STATUS,
                PIPELINE_ID,
                PIPELINE_NAME,
                BUILD_ID,
                BUILD_NUM,
                REASON,
                REASON_DETAIL,
                CREATE_TIME
            ).values(
                triggerEvent.id!!,
                triggerEvent.projectId,
                triggerEvent.eventId,
                triggerEvent.triggerType,
                triggerEvent.eventSource,
                triggerEvent.eventType,
                triggerEvent.triggerUser,
                triggerEvent.eventMessage,
                triggerEvent.eventDesc,
                triggerEvent.eventTime,
                triggerEvent.status,
                triggerEvent.pipelineId,
                triggerEvent.pipelineName,
                triggerEvent.buildId,
                triggerEvent.buildNum,
                triggerEvent.reason,
                triggerEvent.reasonDetailList?.let {
                    JsonUtil.toJson(it)
                },
                LocalDateTime.now()
            ).execute()
        }
    }

    fun listTriggerEvent(
        dslContext: DSLContext,
        projectId: String,
        eventSource: String? = null,
        eventId: Long? = null,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int,
        offset: Int
    ): Result<TPipelineTriggerEventRecord> {
        with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            val conditions = buildConditions(
                projectId = projectId,
                eventSource = eventSource,
                eventId = eventId,
                eventType = eventType,
                triggerUser = triggerUser,
                triggerType = triggerType,
                pipelineId = pipelineId,
                startTime = startTime,
                endTime = endTime
            )
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(EVENT_TIME.desc())
                .limit(limit)
                .offset(offset)
                .fetch()
        }
    }

    fun countTriggerEvent(
        dslContext: DSLContext,
        projectId: String,
        eventSource: String? = null,
        eventId: Long? = null,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Long {
        return with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            val conditions = buildConditions(
                projectId = projectId,
                eventSource = eventSource,
                eventId = eventId,
                eventType = eventType,
                triggerUser = triggerUser,
                triggerType = triggerType,
                pipelineId = pipelineId
            )
            dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun listByEventIds(
        dslContext: DSLContext,
        eventIds: List<String>
    ): Result<TPipelineTriggerEventRecord> {
        return with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            dslContext.selectFrom(this)
                .where(EVENT_ID.`in`(eventIds))
                .fetch()
        }
    }

    fun listRepoWebhookEvent(
        dslContext: DSLContext,
        projectId: String,
        eventSource: String? = null,
        eventId: Long? = null,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int,
        offset: Int
    ): List<RepoWebhookEvent> {
        return with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            val conditions = buildConditions(
                projectId = projectId,
                eventSource = eventSource,
                eventId = eventId,
                eventType = eventType,
                triggerUser = triggerUser,
                triggerType = triggerType,
                pipelineId = pipelineId
            )
            dslContext.select(
                PROJECT_ID,
                EVENT_ID,
                EVENT_SOURCE,
                EVENT_DESC,
                EVENT_TIME,
                count().`as`("total"),
                count(`when`(STATUS.eq(PipelineTriggerStatus.SUCCEED.name), 1)).`as`("success ")
            )
                .from(this)
                .where(conditions)
                .groupBy(PROJECT_ID, EVENT_ID, EVENT_SOURCE, EVENT_DESC, EVENT_TIME)
                .orderBy(EVENT_TIME.desc())
                .limit(limit)
                .offset(offset)
                .fetch().map {
                    RepoWebhookEvent(
                        projectId = it.value1(),
                        eventId = it.value2(),
                        repoHashId = it.value3(),
                        eventDesc = it.value4(),
                        eventTime = it.value5().timestampmilli(),
                        total = it.value6(),
                        success = it.value7()
                    )
                }
        }
    }

    fun countRepoWebhookEvent(
        dslContext: DSLContext,
        projectId: String,
        eventSource: String? = null,
        eventId: Long? = null,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Long {
        return with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            val conditions = buildConditions(
                projectId = projectId,
                eventSource = eventSource,
                eventId = eventId,
                eventType = eventType,
                triggerUser = triggerUser,
                triggerType = triggerType,
                pipelineId = pipelineId
            )
            dslContext.selectCount().from(this)
                .where(conditions)
                .groupBy(PROJECT_ID, EVENT_ID, EVENT_SOURCE, EVENT_DESC, EVENT_TIME)
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun get(
        dslContext: DSLContext,
        id: Long
    ): PipelineTriggerEvent? {
        val record = with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            dslContext.selectFrom(this).where(ID.eq(id)).fetchOne()
        }
        return record?.let { convert(it) }
    }

    private fun TPipelineTriggerEvent.buildConditions(
        projectId: String,
        eventSource: String? = null,
        eventId: Long? = null,
        eventType: String? = null,
        triggerUser: String? = null,
        triggerType: String? = null,
        pipelineId: String? = null,
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
        if (!pipelineId.isNullOrBlank()) {
            conditions.add(PIPELINE_ID.eq(pipelineId))
        }
        if (startTime != null && startTime > 0) {
            conditions.add(CREATE_TIME.ge(Timestamp(startTime).toLocalDateTime()))
        }
        if (endTime != null && endTime > 0) {
            conditions.add(CREATE_TIME.le(Timestamp(endTime).toLocalDateTime()))
        }
        return conditions
    }

    fun convert(record: TPipelineTriggerEventRecord): PipelineTriggerEvent {
        return with (record) {
            PipelineTriggerEvent(
                projectId = projectId,
                eventId = eventId,
                eventSource = eventSource,
                triggerType = triggerType,
                triggerUser = triggerUser,
                eventType = eventType,
                eventMessage = eventMessage,
                eventDesc = eventDesc,
                eventTime = eventTime,
                status = status,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                buildId = buildId,
                buildNum = buildNum,
                reason = reason,
                reasonDetailList = JsonUtil.to(reasonDetail, object: TypeReference<List<String>>(){}),
                createTime = createTime.timestamp()
            )
        }
    }
}
