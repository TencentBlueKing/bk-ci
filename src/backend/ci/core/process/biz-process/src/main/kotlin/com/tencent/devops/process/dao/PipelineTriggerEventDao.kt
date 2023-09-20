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
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.web.utils.I18nUtil.getCodeLanMessage
import com.tencent.devops.model.process.tables.TPipelineTriggerDetail
import com.tencent.devops.model.process.tables.TPipelineTriggerEvent
import com.tencent.devops.model.process.tables.records.TPipelineTriggerDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineTriggerEventRecord
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEventVo
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.trigger.RepoTriggerEventVo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
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
            dslContext.insertInto(
                this,
                PROJECT_ID,
                EVENT_ID,
                TRIGGER_TYPE,
                EVENT_SOURCE,
                EVENT_TYPE,
                TRIGGER_USER,
                EVENT_DESC,
                HOOK_REQUEST_ID,
                REQUEST_PARAMS,
                EVENT_TIME
            ).values(
                triggerEvent.projectId,
                triggerEvent.eventId,
                triggerEvent.triggerType,
                triggerEvent.eventSource,
                triggerEvent.eventType,
                triggerEvent.triggerUser,
                JsonUtil.toJson(triggerEvent.eventDesc),
                triggerEvent.hookRequestId,
                triggerEvent.requestParams?.let { JsonUtil.toJson(it) },
                triggerEvent.eventTime
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun saveDetail(
        dslContext: DSLContext,
        triggerDetail: PipelineTriggerDetail
    ) {
        with(TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL) {
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
                triggerDetail.reasonDetailList?.let {
                    JsonUtil.toJson(it)
                },
                LocalDateTime.now()
            ).execute()
        }
    }

    fun listTriggerEvent(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long? = null,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int,
        offset: Int
    ): List<PipelineTriggerEventVo> {
        val t1 = TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT.`as`("t1")
        val t2 = TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL.`as`("t2")
        val conditions = buildConditions(
            t1 = t1,
            t2 = t2,
            projectId = projectId,
            eventId = eventId,
            eventType = eventType,
            triggerUser = triggerUser,
            triggerType = triggerType,
            pipelineId = pipelineId,
            startTime = startTime,
            endTime = endTime
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
            t1.EVENT_TIME,
            t2.STATUS,
            t2.PIPELINE_ID,
            t2.PIPELINE_NAME,
            t2.BUILD_ID,
            t2.BUILD_NUM,
            t2.REASON,
            t2.REASON_DETAIL
        ).from(t1).leftJoin(t2)
            .on(t1.EVENT_ID.eq(t2.EVENT_ID)).and(t1.PROJECT_ID.eq(t2.PROJECT_ID))
            .where(conditions)
            .orderBy(t1.EVENT_TIME.desc()).limit(limit)
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
                    reasonDetailList = it.value16()
                        ?.let { r -> JsonUtil.to(r, object : TypeReference<List<String>>() {}) }
                )
            }
    }

    fun countTriggerEvent(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long? = null,
        eventType: String? = null,
        triggerType: String? = null,
        triggerUser: String? = null,
        pipelineId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Long {
        val t1 = TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT.`as`("t1")
        val t2 = TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL.`as`("t2")
        val conditions = buildConditions(
            t1 = t1,
            t2 = t2,
            projectId = projectId,
            eventId = eventId,
            eventType = eventType,
            triggerUser = triggerUser,
            triggerType = triggerType,
            pipelineId = pipelineId,
            startTime = startTime,
            endTime = endTime
        )
        return dslContext.selectCount().from(t1).leftJoin(t2)
            .on(t1.EVENT_ID.eq(t2.EVENT_ID)).and(t1.PROJECT_ID.eq(t2.PROJECT_ID))
            .where(conditions)
            .fetchOne(0, Long::class.java)!!
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

    fun listRepoTriggerEvent(
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
    ): List<RepoTriggerEventVo> {
        val t1 = TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT.`as`("t1")
        val t2 = TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL.`as`("t2")
        val conditions = buildConditions(
            t1 = t1,
            t2 = t2,
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
        return dslContext.select(
            t1.PROJECT_ID,
            t1.EVENT_ID,
            t1.EVENT_SOURCE,
            t1.EVENT_DESC,
            t1.EVENT_TIME,
            count().`as`("total"),
            count(`when`(t2.STATUS.eq(PipelineTriggerStatus.SUCCEED.name), 1)).`as`("success ")
        ).from(t1).leftJoin(t2)
            .on(t1.EVENT_ID.eq(t2.EVENT_ID)).and(t1.PROJECT_ID.eq(t2.PROJECT_ID))
            .where(conditions)
            .groupBy(t1.PROJECT_ID, t1.EVENT_ID, t1.EVENT_SOURCE, t1.EVENT_DESC, t1.EVENT_TIME)
            .orderBy(t1.EVENT_TIME.desc())
            .limit(limit)
            .offset(offset)
            .fetch().map {
                RepoTriggerEventVo(
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

    fun countRepoTriggerEvent(
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
        val t1 = TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT.`as`("t1")
        val t2 = TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL.`as`("t2")
        val conditions = buildConditions(
            t1 = t1,
            t2 = t2,
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
        return dslContext.select(DSL.countDistinct(t1.PROJECT_ID, t1.EVENT_ID))
            .from(t1)
            .leftJoin(t2)
            .on(t1.EVENT_ID.eq(t2.EVENT_ID)).and(t1.PROJECT_ID.eq(t2.PROJECT_ID))
            .where(conditions)
            .fetchOne(0, Long::class.java)!!
    }

    fun getTriggerEvent(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long
    ): PipelineTriggerEvent? {
        val record = with(TPipelineTriggerEvent.T_PIPELINE_TRIGGER_EVENT) {
            dslContext.selectFrom(this)
                .where(EVENT_ID.eq(eventId))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
        }
        return record?.let { convertEvent(it) }
    }

    fun getTriggerDetail(
        dslContext: DSLContext,
        projectId: String,
        detailId: Long
    ): PipelineTriggerDetail? {
        val record = with(TPipelineTriggerDetail.T_PIPELINE_TRIGGER_DETAIL) {
            dslContext.selectFrom(this)
                .where(DETAIL_ID.eq(detailId))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
        }
        return record?.let { convertDetail(it) }
    }

    private fun buildConditions(
        t1: TPipelineTriggerEvent,
        t2: TPipelineTriggerDetail,
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
        conditions.add(t1.PROJECT_ID.eq(projectId))
        if (!eventSource.isNullOrBlank()) {
            conditions.add(t1.EVENT_SOURCE.eq(eventSource))
        }
        if (eventId != null) {
            conditions.add(t1.EVENT_ID.eq(eventId))
        }
        if (!eventType.isNullOrBlank()) {
            conditions.add(t1.EVENT_TYPE.eq(eventType))
        }
        if (!triggerUser.isNullOrBlank()) {
            conditions.add(t1.TRIGGER_USER.eq(triggerUser))
        }
        if (!triggerType.isNullOrBlank()) {
            conditions.add(t1.TRIGGER_TYPE.eq(triggerType))
        }
        if (!pipelineId.isNullOrBlank()) {
            conditions.add(t2.PIPELINE_ID.eq(pipelineId))
        }
        if (startTime != null && startTime > 0) {
            conditions.add(t1.EVENT_TIME.ge(Timestamp(startTime).toLocalDateTime()))
        }
        if (endTime != null && endTime > 0) {
            conditions.add(t1.EVENT_TIME.le(Timestamp(endTime).toLocalDateTime()))
        }
        return conditions
    }

    fun convertEvent(record: TPipelineTriggerEventRecord): PipelineTriggerEvent {
        return with(record) {
            PipelineTriggerEvent(
                projectId = projectId,
                eventId = eventId,
                eventSource = eventSource,
                triggerType = triggerType,
                triggerUser = triggerUser,
                eventType = eventType,
                eventDesc = JsonUtil.to(eventDesc, I18Variable::class.java).getCodeLanMessage(),
                hookRequestId = hookRequestId,
                requestParams = requestParams?.let {
                    JsonUtil.to(
                        it,
                        object : TypeReference<Map<String, String>>() {})
                },
                eventTime = eventTime
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
                reasonDetailList = reasonDetail?.let { JsonUtil.to(it, object : TypeReference<List<String>>() {}) },
                createTime = createTime.timestamp()
            )
        }
    }
}
