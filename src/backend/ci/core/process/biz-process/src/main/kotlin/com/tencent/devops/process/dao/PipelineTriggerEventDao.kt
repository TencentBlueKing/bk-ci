/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.web.utils.I18nUtil.getCodeLanMessage
import com.tencent.devops.model.process.tables.TPipelineTriggerDetail
import com.tencent.devops.model.process.tables.TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL
import com.tencent.devops.model.process.tables.TPipelineTriggerEvent
import com.tencent.devops.model.process.tables.TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT
import com.tencent.devops.model.process.tables.records.TPipelineTriggerDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineTriggerEventRecord
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEventVo
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedFix
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonStatistics
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.trigger.RepoTriggerEventDetail
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.countDistinct
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
        with(T_PIPELINE_TRIGGER_EVENT) {
            dslContext.insertInto(
                this,
                REQUEST_ID,
                PROJECT_ID,
                EVENT_ID,
                TRIGGER_TYPE,
                EVENT_SOURCE,
                EVENT_TYPE,
                TRIGGER_USER,
                EVENT_DESC,
                REPLAY_REQUEST_ID,
                REQUEST_PARAMS,
                CREATE_TIME,
                EVENT_BODY
            ).values(
                triggerEvent.requestId,
                triggerEvent.projectId,
                triggerEvent.eventId,
                triggerEvent.triggerType,
                triggerEvent.eventSource,
                triggerEvent.eventType,
                triggerEvent.triggerUser,
                JsonUtil.toJson(triggerEvent.eventDesc),
                triggerEvent.replayRequestId,
                triggerEvent.requestParams?.let { JsonUtil.toJson(it, false) },
                triggerEvent.createTime,
                triggerEvent.eventBody?.let { JsonUtil.toJson(it, false) }
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        triggerEvent: PipelineTriggerEvent
    ) {
        with(T_PIPELINE_TRIGGER_EVENT) {
            dslContext.update(this)
                .set(EVENT_BODY, triggerEvent.eventBody?.let { JsonUtil.toJson(it, false) })
                .where(PROJECT_ID.eq(triggerEvent.projectId))
                .and(EVENT_ID.eq(triggerEvent.eventId))
                .execute()
        }
    }

    fun saveDetail(
        dslContext: DSLContext,
        triggerDetail: PipelineTriggerDetail
    ) {
        with(T_PIPELINE_TRIGGER_DETAIL) {
            dslContext.insertInto(
                this,
                DETAIL_ID,
                PROJECT_ID,
                EVENT_ID,
                STATUS,
                PIPELINE_ID,
                PIPELINE_NAME,
                BUILD_ID,
                BUILD_NUM,
                REASON,
                REASON_DETAIL,
                CREATE_TIME
            ).values(
                triggerDetail.detailId!!,
                triggerDetail.projectId,
                triggerDetail.eventId,
                triggerDetail.status,
                triggerDetail.pipelineId,
                triggerDetail.pipelineName,
                triggerDetail.buildId,
                triggerDetail.buildNum,
                triggerDetail.reason,
                triggerDetail.reasonDetail?.let { JsonUtil.toJson(it, false) },
                LocalDateTime.now()
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun listTriggerDetail(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long? = null,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        pipelineName: String? = null,
        reason: String?,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int,
        offset: Int
    ): List<PipelineTriggerEventVo> {
        val t1 = T_PIPELINE_TRIGGER_EVENT.`as`("t1")
        val t2 = T_PIPELINE_TRIGGER_DETAIL.`as`("t2")
        val conditions = buildConditions(
            t1 = t1,
            t2 = t2,
            projectId = projectId,
            eventId = eventId,
            eventType = eventType,
            triggerUser = triggerUser,
            triggerType = triggerType,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            startTime = startTime,
            endTime = endTime,
            reason = reason
        )
        return dslContext.select(
            t2.DETAIL_ID,
            t1.PROJECT_ID,
            t1.EVENT_ID,
            t1.TRIGGER_TYPE,
            t1.EVENT_SOURCE,
            t1.EVENT_TYPE,
            t1.TRIGGER_USER,
            t1.EVENT_DESC,
            t1.CREATE_TIME,
            t2.STATUS,
            t2.PIPELINE_ID,
            t2.PIPELINE_NAME,
            t2.BUILD_ID,
            t2.BUILD_NUM,
            t2.REASON,
            t2.REASON_DETAIL
        ).from(t2).leftJoin(t1)
            .on(t1.EVENT_ID.eq(t2.EVENT_ID)).and(t1.PROJECT_ID.eq(t2.PROJECT_ID))
            .where(conditions)
            .orderBy(t1.CREATE_TIME.desc()).limit(limit)
            .offset(offset)
            .fetch().map {
                PipelineTriggerEventVo(
                    detailId = it.value1(),
                    projectId = it.value2(),
                    eventId = it.value3(),
                    triggerType = it.value4(),
                    eventSource = it.value5(),
                    eventType = it.value6(),
                    triggerUser = it.value7(),
                    eventDesc = it.value8(),
                    eventTime = it.value9().timestampmilli(),
                    status = it.value10(),
                    pipelineId = it.value11(),
                    pipelineName = it.value12(),
                    buildId = it.value13(),
                    buildNum = it.value14(),
                    reason = it.value15(),
                    reasonDetailList = it.value16()?.let { r -> convertReasonDetail(r) }?.getReasonDetailList()
                )
            }
    }

    fun countTriggerDetail(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long? = null,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        pipelineName: String? = null,
        reason: String?,
        startTime: Long? = null,
        endTime: Long? = null
    ): Long {
        val t1 = T_PIPELINE_TRIGGER_EVENT.`as`("t1")
        val t2 = T_PIPELINE_TRIGGER_DETAIL.`as`("t2")
        val conditions = buildConditions(
            t1 = t1,
            t2 = t2,
            projectId = projectId,
            eventId = eventId,
            eventType = eventType,
            triggerUser = triggerUser,
            triggerType = triggerType,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            reason = reason,
            startTime = startTime,
            endTime = endTime
        )
        return dslContext.selectCount().from(t2).leftJoin(t1)
            .on(t1.EVENT_ID.eq(t2.EVENT_ID)).and(t1.PROJECT_ID.eq(t2.PROJECT_ID))
            .where(conditions)
            .fetchOne(0, Long::class.java)!!
    }

    fun listByEventIds(
        dslContext: DSLContext,
        eventIds: List<String>
    ): Result<TPipelineTriggerEventRecord> {
        return with(T_PIPELINE_TRIGGER_EVENT) {
            dslContext.selectFrom(this)
                .where(EVENT_ID.`in`(eventIds))
                .fetch()
        }
    }

    fun getEventIdsByEvent(
        dslContext: DSLContext,
        eventId: Long? = null,
        eventSource: String? = null,
        projectId: String,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Set<Long> {
        return with(T_PIPELINE_TRIGGER_EVENT) {
            val step = dslContext
                .selectFrom(this)
                .where(
                    buildEventCondition(
                        this,
                        projectId = projectId,
                        eventSource = eventSource,
                        eventId = eventId,
                        eventType = eventType,
                        triggerUser = triggerUser,
                        triggerType = triggerType,
                        startTime = startTime,
                        endTime = endTime
                    )
                )
                .orderBy(CREATE_TIME.desc())
            if (limit != null) {
                step.limit(limit)
            }
            if (offset != null) {
                step.offset(offset)
            }
            step.fetch(EVENT_ID).toSet()
        }
    }

    fun getCountByEvent(
        dslContext: DSLContext,
        eventId: Long? = null,
        eventSource: String? = null,
        projectId: String,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Long {
        return with(T_PIPELINE_TRIGGER_EVENT) {
            dslContext.selectCount()
                .from(this)
                .where(
                    buildEventCondition(
                        this,
                        projectId = projectId,
                        eventSource = eventSource,
                        eventId = eventId,
                        eventType = eventType,
                        triggerUser = triggerUser,
                        triggerType = triggerType,
                        startTime = startTime,
                        endTime = endTime
                    )
                ).fetchOne(0, Long::class.java)!!
        }
    }

    fun getCountByDetail(
        dslContext: DSLContext,
        pipelineName: String?,
        pipelineId: String?,
        eventId: Long? = null,
        eventSource: String? = null,
        projectId: String,
        eventType: String? = null,
        reason: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Long {
        val t1 = T_PIPELINE_TRIGGER_EVENT.`as`("t1")
        val t2 = T_PIPELINE_TRIGGER_DETAIL.`as`("t2")
        val conditions = buildConditions(
            t1 = t1,
            t2 = t2,
            projectId = projectId,
            eventSource = eventSource,
            eventId = eventId,
            eventType = eventType,
            reason = reason,
            triggerUser = triggerUser,
            triggerType = triggerType,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            startTime = startTime,
            endTime = endTime
        )
        return dslContext.select(countDistinct(t2.EVENT_ID))
            .from(t2).leftJoin(t1)
            .on(t1.EVENT_ID.eq(t2.EVENT_ID)).and(t1.PROJECT_ID.eq(t2.PROJECT_ID))
            .where(conditions)
            .fetchOne(0, Long::class.java)!!
    }

    fun getDetailEventIds(
        dslContext: DSLContext,
        pipelineName: String?,
        pipelineId: String?,
        eventId: Long? = null,
        eventSource: String? = null,
        projectId: String,
        eventType: String? = null,
        reason: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int,
        offset: Int
    ): Set<Long> {
        val t1 = T_PIPELINE_TRIGGER_EVENT.`as`("t1")
        val t2 = T_PIPELINE_TRIGGER_DETAIL.`as`("t2")
        val conditions = buildConditions(
            t1 = t1,
            t2 = t2,
            projectId = projectId,
            eventSource = eventSource,
            eventId = eventId,
            eventType = eventType,
            reason = reason,
            triggerUser = triggerUser,
            triggerType = triggerType,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            startTime = startTime,
            endTime = endTime
        )
        return dslContext.select(t2.EVENT_ID)
            .from(t2).leftJoin(t1)
            .on(t1.EVENT_ID.eq(t2.EVENT_ID)).and(t1.PROJECT_ID.eq(t2.PROJECT_ID))
            .where(conditions)
            .orderBy(t2.CREATE_TIME.desc())
            .limit(limit)
            .offset(offset)
            .fetch().distinct().map { it.value1() }.toSet()
    }

    fun listRepoTriggerEvent(
        dslContext: DSLContext,
        eventIds: Set<Long>
    ): List<TPipelineTriggerEventRecord> {
        return with(T_PIPELINE_TRIGGER_EVENT) {
            dslContext.selectFrom(this)
                .where(EVENT_ID.`in`(eventIds))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun listRepoTriggerDetail(
        dslContext: DSLContext,
        projectId: String,
        eventIds: Set<Long>,
        pipelineName: String?,
        pipelineId: String?
    ): List<RepoTriggerEventDetail> {
        return with(T_PIPELINE_TRIGGER_DETAIL) {
            val conditions = mutableListOf(
                EVENT_ID.`in`(eventIds),
                PROJECT_ID.eq(projectId)
            )
            if (!pipelineName.isNullOrBlank()) {
                conditions.add(PIPELINE_NAME.like("%$pipelineName%"))
            }
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            dslContext.select(
                PROJECT_ID,
                EVENT_ID,
                count().`as`("total"),
                count(`when`(STATUS.eq(PipelineTriggerStatus.SUCCEED.name), 1)).`as`("success")
            )
                .from(this)
                .where(conditions)
                .groupBy(PROJECT_ID, EVENT_ID)
                .fetch().map {
                    RepoTriggerEventDetail(
                        projectId = it.value1(),
                        eventId = it.value2(),
                        total = it.value3(),
                        success = it.value4()
                    )
                }
        }
    }

    fun getTriggerEvent(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long
    ): PipelineTriggerEvent? {
        val record = with(T_PIPELINE_TRIGGER_EVENT) {
            dslContext.selectFrom(this)
                .where(EVENT_ID.eq(eventId))
                .and(PROJECT_ID.eq(projectId))
                .fetchAny()
        }
        return record?.let { convertEvent(it) }
    }

    fun getEventByRequestId(
        dslContext: DSLContext,
        projectId: String,
        requestId: String,
        eventSource: String
    ): PipelineTriggerEvent? {
        val record = with(T_PIPELINE_TRIGGER_EVENT) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REQUEST_ID.eq(requestId))
                .and(EVENT_SOURCE.eq(eventSource))
                .fetchAny()
        }
        return record?.let { convertEvent(it) }
    }

    fun getTriggerDetail(
        dslContext: DSLContext,
        projectId: String,
        detailId: Long
    ): PipelineTriggerDetail? {
        val record = with(T_PIPELINE_TRIGGER_DETAIL) {
            dslContext.selectFrom(this)
                .where(DETAIL_ID.eq(detailId))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
        }
        return record?.let { convertDetail(it) }
    }

    private fun buildDetailCondition(
        t2: TPipelineTriggerDetail,
        eventId: Long? = null,
        pipelineName: String? = null,
        projectId: String,
        pipelineId: String? = null,
        reason: String? = null
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()
        with(t2) {
            if (eventId != null) {
                conditions.add(EVENT_ID.eq(eventId))
            }
            if (!pipelineName.isNullOrBlank()) {
                conditions.add(PIPELINE_NAME.like("%$pipelineName%"))
            }
            if (projectId.isNotBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (!reason.isNullOrBlank()) {
                conditions.add(REASON.eq(reason))
            }
        }
        return conditions
    }

    private fun buildEventCondition(
        t1: TPipelineTriggerEvent,
        eventId: Long? = null,
        eventSource: String? = null,
        projectId: String,
        eventType: String?,
        triggerUser: String? = null,
        triggerType: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()
        with(t1) {
            if (eventId != null) {
                conditions.add(EVENT_ID.eq(eventId))
            }
            if (!eventSource.isNullOrBlank()) {
                conditions.add(EVENT_SOURCE.eq(eventSource))
            }
            if (projectId.isNotBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
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
            if (startTime != null) {
                conditions.add(CREATE_TIME.ge(Timestamp(startTime).toLocalDateTime()))
            }
            if (endTime != null) {
                conditions.add(CREATE_TIME.le(Timestamp(endTime).toLocalDateTime()))
            }
        }
        return conditions
    }

    private fun buildConditions(
        t1: TPipelineTriggerEvent,
        t2: TPipelineTriggerDetail,
        projectId: String,
        pipelineName: String? = null,
        eventSource: String? = null,
        eventId: Long? = null,
        eventType: String? = null,
        reason: String? = null,
        triggerUser: String? = null,
        triggerType: String? = null,
        pipelineId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.addAll(
            buildEventCondition(
                t1,
                eventId = eventId,
                eventSource = eventSource,
                projectId = projectId,
                eventType = eventType,
                triggerType = triggerType,
                triggerUser = triggerUser,
                startTime = startTime,
                endTime = endTime
            )
        )
        conditions.addAll(
            buildDetailCondition(
                t2,
                eventId = eventId,
                projectId = projectId,
                pipelineName = pipelineName,
                pipelineId = pipelineId,
                reason = reason
            )
        )
        return conditions
    }

    fun convertEvent(record: TPipelineTriggerEventRecord): PipelineTriggerEvent {
        return with(record) {
            PipelineTriggerEvent(
                requestId = requestId,
                projectId = projectId,
                eventId = eventId,
                eventSource = eventSource,
                triggerType = triggerType,
                triggerUser = triggerUser,
                eventType = eventType,
                eventDesc = JsonUtil.to(eventDesc, I18Variable::class.java).getCodeLanMessage(),
                replayRequestId = replayRequestId,
                requestParams = requestParams?.let {
                    JsonUtil.to(
                        it,
                        object : TypeReference<Map<String, String>>() {})
                },
                createTime = createTime,
                eventBody = eventBody?.let {
                    JsonUtil.to(it, Webhook::class.java)
                }
            )
        }
    }

    fun convertDetail(record: TPipelineTriggerDetailRecord): PipelineTriggerDetail {
        return with(record) {
            PipelineTriggerDetail(
                detailId = detailId,
                projectId = projectId,
                eventId = eventId,
                status = status,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                buildId = buildId,
                buildNum = buildNum,
                reason = reason,
                reasonDetail = reasonDetail?.let { convertReasonDetail(it) },
                createTime = createTime.timestamp()
            )
        }
    }

    fun convertReasonDetail(reasonDetail: String): PipelineTriggerReasonDetail {
        val jsonNode = JsonUtil.getObjectMapper().readTree(reasonDetail)
        return if (jsonNode.isArray) {
            PipelineTriggerFailedFix(JsonUtil.to(reasonDetail, object : TypeReference<List<String>>() {}))
        } else {
            JsonUtil.to(reasonDetail, PipelineTriggerReasonDetail::class.java)
        }
    }

    fun triggerReasonStatistics(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long,
        pipelineId: String? = null,
        pipelineName: String? = null
    ): PipelineTriggerReasonStatistics {
        return with(T_PIPELINE_TRIGGER_DETAIL) {
            val conditions = mutableListOf(
                EVENT_ID.eq(eventId),
                PROJECT_ID.eq(projectId)
            ).let {
                if (!pipelineId.isNullOrBlank()) {
                    it.add(PIPELINE_ID.eq(pipelineId))
                }
                if (!pipelineName.isNullOrBlank()) {
                    it.add(PIPELINE_NAME.eq(pipelineName))
                }
                it
            }
            dslContext.select(
                REASON,
                count()
            ).from(this)
                .where(conditions)
                .groupBy(REASON)
                .fetch()
                .associate {
                    it.value1() to it.value2()
                }.let {
                    PipelineTriggerReasonStatistics(
                        triggerSuccess = it[PipelineTriggerReason.TRIGGER_SUCCESS.name] ?: 0,
                        triggerFailed = it[PipelineTriggerReason.TRIGGER_FAILED.name] ?: 0,
                        triggerNotMatch = it[PipelineTriggerReason.TRIGGER_NOT_MATCH.name] ?: 0
                    )
                }
        }
    }
}
