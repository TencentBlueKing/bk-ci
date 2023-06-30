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

package com.tencent.devops.process.engine.dao

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_HISTORY
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.app.StartBuildContext
import com.tencent.devops.process.pojo.code.WebhookInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Record2
import org.jooq.Result
import org.jooq.SelectConditionStep
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.ws.rs.core.Response

@Suppress("ALL")
@Repository
class PipelineBuildDao {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
    }

    fun create(dslContext: DSLContext, startBuildContext: StartBuildContext) {
        try {
            with(T_PIPELINE_BUILD_HISTORY) {
                dslContext.insertInto(
                    this,
                    BUILD_ID,
                    BUILD_NUM,
                    PROJECT_ID,
                    PIPELINE_ID,
                    PARENT_BUILD_ID,
                    PARENT_TASK_ID,
                    START_USER,
                    TRIGGER_USER,
                    STATUS,
                    TRIGGER,
                    TASK_COUNT,
                    FIRST_TASK_ID,
                    CHANNEL,
                    VERSION,
                    QUEUE_TIME,
                    BUILD_PARAMETERS,
                    WEBHOOK_TYPE,
                    WEBHOOK_INFO,
                    BUILD_MSG,
                    BUILD_NUM_ALIAS,
                    CONCURRENCY_GROUP
                ).values(
                    startBuildContext.buildId,
                    startBuildContext.buildNum,
                    startBuildContext.projectId,
                    startBuildContext.pipelineId,
                    startBuildContext.parentBuildId,
                    startBuildContext.parentTaskId,
                    startBuildContext.userId,
                    startBuildContext.triggerUser,
                    startBuildContext.startBuildStatus.ordinal,
                    startBuildContext.startType.name,
                    startBuildContext.taskCount,
                    startBuildContext.firstTaskId,
                    startBuildContext.channelCode.name,
                    startBuildContext.resourceVersion,
                    LocalDateTime.now(),
                    JsonUtil.toJson(startBuildContext.buildParameters, formatted = false),
                    startBuildContext.webhookInfo?.webhookType,
                    startBuildContext.webhookInfo?.let { self -> JsonUtil.toJson(self, formatted = false) },
                    startBuildContext.buildMsg,
                    startBuildContext.buildNumAlias,
                    startBuildContext.concurrencyGroup
                ).execute()
            }
        } catch (t: Throwable) {
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_START_WITH_ERROR,
                params = arrayOf(t.message ?: "")
            )
        }
    }

    /**
     * 读取指定状态下的构建
     */
    fun getBuildTasksByStatus(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statusSet: Set<BuildStatus>?
    ): Result<TPipelineBuildHistoryRecord?> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val where = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (!statusSet.isNullOrEmpty()) {
                where.and(STATUS.`in`(statusSet.map { it.ordinal }))
            }
            where.fetch()
        }
    }

    fun getBuildTasksByConcurrencyGroup(
        dslContext: DSLContext,
        projectId: String,
        concurrencyGroup: String,
        statusSet: List<BuildStatus>
    ): List<Record2<String, String>> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.select(PIPELINE_ID, BUILD_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(STATUS.`in`(statusSet.map { it.ordinal }))
                .and(CONCURRENCY_GROUP.eq(concurrencyGroup)).orderBy(START_TIME.asc())
                .fetch()
        }
    }

    fun getBuildTasksByConcurrencyGroupNull(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statusSet: List<BuildStatus>
    ): List<Record2<String, String>> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.select(PIPELINE_ID, BUILD_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(STATUS.`in`(statusSet.map { it.ordinal }))
                .and(CONCURRENCY_GROUP.isNull).orderBy(START_TIME.asc())
                .fetch()
        }
    }

    fun getBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .fetchAny()
        }
    }

    fun getStartUser(dslContext: DSLContext, projectId: String, buildId: String): String? {
        with(T_PIPELINE_BUILD_HISTORY) {
            return dslContext.select(START_USER)
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .fetchOne(0, String::class.java)
        }
    }

    /**
     * 旧接口适配
     */
    fun listBuildInfoByBuildIds(
        dslContext: DSLContext,
        buildIds: Collection<String>,
        projectId: String? = null,
        startBeginTime: String? = null,
        endBeginTime: String? = null
    ): Result<TPipelineBuildHistoryRecord> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(BUILD_ID.`in`(buildIds))
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (startBeginTime != null) {
                conditions.add(START_TIME.ge(DateTimeUtil.stringToLocalDateTime(startBeginTime)))
            }
            if (endBeginTime != null) {
                conditions.add(START_TIME.le(DateTimeUtil.stringToLocalDateTime(endBeginTime)))
            }
            dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun listPipelineBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int,
        updateTimeDesc: Boolean? = null
    ): Collection<TPipelineBuildHistoryRecord> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            when (updateTimeDesc) {
                true -> select.orderBy(UPDATE_TIME.desc(), BUILD_ID)
                false -> select.orderBy(UPDATE_TIME.asc(), BUILD_ID)
                null -> select.orderBy(BUILD_NUM.desc())
            }
            select.limit(offset, limit)
                .fetch()
        }
    }

    fun listPipelineBuildNum(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): Collection<Int> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.select(BUILD_NUM).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .orderBy(BUILD_NUM.desc())
                .limit(offset, limit)
                .fetch(0, Int::class.java)
        }
    }

    fun getBuildInfoByBuildNum(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildNum: Int?,
        statusSet: Set<BuildStatus>?
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))

            if (!statusSet.isNullOrEmpty()) {
                select.and(STATUS.`in`(statusSet.map { it.ordinal }))
            }

            if (buildNum != null && buildNum > 0) {
                select.and(BUILD_NUM.eq(buildNum))
            } else { // 取最新的
                select.orderBy(BUILD_NUM.desc()).limit(1)
            }
            select.fetchOne()
        }
    }

    fun getOneQueueBuild(dslContext: DSLContext, projectId: String, pipelineId: String): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(STATUS.`in`(setOf(BuildStatus.QUEUE.ordinal, BuildStatus.QUEUE_CACHE.ordinal)))
                .orderBy(BUILD_NUM.asc()).limit(1)
            select.fetchAny()
        }
    }

    fun getOneConcurrencyQueueBuild(
        dslContext: DSLContext,
        projectId: String,
        concurrencyGroup: String,
        pipelineId: String? = null
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CONCURRENCY_GROUP.eq(concurrencyGroup))
                .and(STATUS.`in`(setOf(BuildStatus.QUEUE.ordinal, BuildStatus.QUEUE_CACHE.ordinal)))
            if (pipelineId != null) {
                select.and(PIPELINE_ID.eq(pipelineId))
            }
            select.orderBy(QUEUE_TIME.asc(), PIPELINE_ID, BUILD_NUM.asc()).limit(1)
            select.fetchAny()
        }
    }

    /**
     * 1：开始构建
     */
    fun startBuild(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        startTime: LocalDateTime?,
        executeCount: Int?
    ) {
        with(T_PIPELINE_BUILD_HISTORY) {
            val update = dslContext.update(this).set(STATUS, BuildStatus.RUNNING.ordinal)
            startTime?.let { update.set(START_TIME, startTime) }
            executeCount?.let { update.set(EXECUTE_COUNT, executeCount) }
            update.setNull(ERROR_INFO)
            update.where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId))).execute()
        }
    }

    /**
     * 2:结束构建
     */
    fun finishBuild(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        buildStatus: BuildStatus,
        executeTime: Long?,
        recommendVersion: String?,
        remark: String? = null,
        errorInfoList: List<ErrorInfo>?
    ) {
        with(T_PIPELINE_BUILD_HISTORY) {
            val baseQuery = dslContext.update(this)
                .set(STATUS, buildStatus.ordinal)
                .set(END_TIME, LocalDateTime.now())
                .set(EXECUTE_TIME, executeTime)

            if (!recommendVersion.isNullOrBlank()) {
                baseQuery.set(RECOMMEND_VERSION, recommendVersion)
            }

            if (!remark.isNullOrBlank()) {
                baseQuery.set(REMARK, remark)
            }

            if (errorInfoList != null) {
                baseQuery.set(ERROR_INFO, JsonUtil.toJson(errorInfoList, formatted = false))
            }

            baseQuery.where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId))).execute()
        }
    }

    /**
     * 取最近一次构建的参数
     */
    fun getLatestBuild(dslContext: DSLContext, projectId: String, pipelineId: String): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId)
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny()
        }
    }

    /**
     * 取最近一次完成的构建
     */
    fun getLatestFinishedBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    STATUS.notIn(
                        mutableListOf(
                            BuildStatus.RUNNING.ordinal, // 3 运行中
                            BuildStatus.QUEUE.ordinal // 13 排队（新）
                        )
                    )
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny()
        }
    }

    /**
     * 取最近一次成功的构建
     */
    fun getLatestFailedBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    STATUS.eq(1)
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny()
        }
    }

    /**
     * 取最近一次失败的构建
     */
    fun getLatestSucceedBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    STATUS.eq(0)
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        oldBuildStatus: BuildStatus,
        newBuildStatus: BuildStatus,
        startTime: LocalDateTime? = null,
        errorInfoList: List<ErrorInfo>? = null
    ): Boolean {
        with(T_PIPELINE_BUILD_HISTORY) {
            val update = dslContext.update(this)
                .set(STATUS, newBuildStatus.ordinal)
            startTime?.let {
                update.set(START_TIME, it)
            }
            errorInfoList?.let {
                update.set(ERROR_INFO, JsonUtil.toJson(it, formatted = false))
            }
            return update.where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(STATUS.eq(oldBuildStatus.ordinal))
                .execute() == 1
        }
    }

    fun updateExecuteCount(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        executeCount: Int
    ): Boolean {
        with(T_PIPELINE_BUILD_HISTORY) {
            return dslContext.update(this)
                .set(EXECUTE_COUNT, executeCount)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute() == 1
        }
    }

    fun convert(t: TPipelineBuildHistoryRecord?): BuildInfo? {
        return if (t == null) {
            null
        } else {
            BuildInfo(
                projectId = t.projectId,
                pipelineId = t.pipelineId,
                buildId = t.buildId,
                version = t.version,
                buildNum = t.buildNum,
                trigger = t.trigger,
                status = BuildStatus.values()[t.status],
                queueTime = t.queueTime?.timestampmilli() ?: 0L,
                startUser = t.startUser,
                triggerUser = t.triggerUser,
                startTime = t.startTime?.timestampmilli() ?: 0L,
                endTime = t.endTime?.timestampmilli() ?: 0L,
                taskCount = t.taskCount,
                firstTaskId = t.firstTaskId,
                parentBuildId = t.parentBuildId,
                parentTaskId = t.parentTaskId,
                channelCode = ChannelCode.valueOf(t.channel),
                errorInfoList = try {
                    if (t.errorInfo != null) {
                        JsonUtil.getObjectMapper().readValue(t.errorInfo) as List<ErrorInfo>
                    } else null
                } catch (ignored: Exception) {
                    null
                },
                stageStatus = kotlin.runCatching {
                    JsonUtil.getObjectMapper().readValue(t.stageStatus) as List<BuildStageStatus>
                }.getOrNull(),
                buildParameters = t.buildParameters?.let { self ->
                    JsonUtil.getObjectMapper().readValue(self) as List<BuildParameters>
                },
                retryFlag = t.isRetry,
                executeCount = t.executeCount,
                executeTime = t.executeTime ?: 0,
                concurrencyGroup = t.concurrencyGroup,
                webhookInfo = t.webhookInfo?.let { JsonUtil.to(t.webhookInfo, WebhookInfo::class.java) },
                buildMsg = t.buildMsg,
                errorType = t.errorType,
                errorCode = t.errorCode,
                errorMsg = t.errorMsg,
                material = t.material?.let {
                    JsonUtil.getObjectMapper().readValue(it) as List<PipelineBuildMaterial>
                },
                remark = t.remark
            )
        }
    }

    fun count(dslContext: DSLContext, projectId: String, pipelineId: String, status: List<BuildStatus>? = null): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val where = dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (!status.isNullOrEmpty()) {
                where.and(STATUS.`in`(status.map { it.ordinal }))
            }
            where.fetchOne(0, Int::class.java)!!
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        materialAlias: List<String>?,
        materialUrl: String?,
        materialBranch: List<String>?,
        materialCommitId: String?,
        materialCommitMessage: String?,
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?,
        totalTimeMin: Long?,
        totalTimeMax: Long?,
        remark: String?,
        buildNoStart: Int?,
        buildNoEnd: Int?,
        buildMsg: String?,
        startUser: List<String>?
    ): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val where = dslContext.selectCount()
                .from(this).where(PROJECT_ID.eq(projectId)).and(PIPELINE_ID.eq(pipelineId))
            makeCondition(
                where = where,
                materialAlias = materialAlias,
                materialUrl = materialUrl,
                materialBranch = materialBranch,
                materialCommitId = materialCommitId,
                materialCommitMessage = materialCommitMessage,
                status = status,
                startUser = startUser,
                trigger = trigger,
                queueTimeStartTime = queueTimeStartTime,
                queueTimeEndTime = queueTimeEndTime,
                startTimeStartTime = startTimeStartTime,
                startTimeEndTime = startTimeEndTime,
                endTimeStartTime = endTimeStartTime,
                endTimeEndTime = endTimeEndTime,
                totalTimeMin = totalTimeMin,
                totalTimeMax = totalTimeMax,
                remark = remark,
                buildNoStart = buildNoStart,
                buildNoEnd = buildNoEnd,
                buildMsg = buildMsg
            )
            where.fetchOne(0, Int::class.java)!!
        }
    }

    fun listPipelineBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        materialAlias: List<String>?,
        materialUrl: String?,
        materialBranch: List<String>?,
        materialCommitId: String?,
        materialCommitMessage: String?,
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?,
        totalTimeMin: Long?,
        totalTimeMax: Long?,
        remark: String?,
        offset: Int,
        limit: Int,
        buildNoStart: Int?,
        buildNoEnd: Int?,
        buildMsg: String?,
        startUser: List<String>?,
        updateTimeDesc: Boolean? = null
    ): Collection<TPipelineBuildHistoryRecord> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val where = dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).and(PIPELINE_ID.eq(pipelineId))
            makeCondition(
                where = where,
                materialAlias = materialAlias,
                materialUrl = materialUrl,
                materialBranch = materialBranch,
                materialCommitId = materialCommitId,
                materialCommitMessage = materialCommitMessage,
                status = status,
                startUser = startUser,
                trigger = trigger,
                queueTimeStartTime = queueTimeStartTime,
                queueTimeEndTime = queueTimeEndTime,
                startTimeStartTime = startTimeStartTime,
                startTimeEndTime = startTimeEndTime,
                endTimeStartTime = endTimeStartTime,
                endTimeEndTime = endTimeEndTime,
                totalTimeMin = totalTimeMin,
                totalTimeMax = totalTimeMax,
                remark = remark,
                buildNoStart = buildNoStart,
                buildNoEnd = buildNoEnd,
                buildMsg = buildMsg
            )

            when (updateTimeDesc) {
                true -> where.orderBy(UPDATE_TIME.desc(), BUILD_ID)
                false -> where.orderBy(UPDATE_TIME.asc(), BUILD_ID)
                null -> where.orderBy(BUILD_NUM.desc())
            }

            where.limit(offset, limit).fetch()
        }
    }

    private fun TPipelineBuildHistory.makeCondition(
        where: SelectConditionStep<*>,
        materialAlias: List<String>?,
        materialUrl: String?,
        materialBranch: List<String>?,
        materialCommitId: String?,
        materialCommitMessage: String?,
        status: List<BuildStatus>?,
        startUser: List<String>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?,
        totalTimeMin: Long?,
        totalTimeMax: Long?,
        remark: String?,
        buildNoStart: Int?,
        buildNoEnd: Int?,
        buildMsg: String?
    ) {
        if (!materialAlias.isNullOrEmpty() && materialAlias.first().isNotBlank()) {
            var conditionsOr: Condition

            conditionsOr = JooqUtils.jsonExtract(t1 = MATERIAL, t2 = "\$[*].aliasName", lower = true)
                .like("%${materialAlias.first().lowercase()}%")

            materialAlias.forEachIndexed { index, s ->
                if (index == 0) return@forEachIndexed
                conditionsOr = conditionsOr.or(
                    JooqUtils.jsonExtract(MATERIAL, "\$[*].aliasName", lower = true).like("%${s.lowercase()}%")
                )
            }
            where.and(conditionsOr)
        }

        if (!materialUrl.isNullOrBlank()) {
            where.and(JooqUtils.jsonExtract(MATERIAL, "\$[*].url").like("%$materialUrl%"))
        }

        if (!materialBranch.isNullOrEmpty() && materialBranch.first().isNotBlank()) {
            var conditionsOr: Condition

            conditionsOr = JooqUtils.jsonExtract(MATERIAL, "\$[*].branchName", lower = true)
                .like("%${materialBranch.first().lowercase()}%")

            materialBranch.forEachIndexed { index, s ->
                if (index == 0) return@forEachIndexed
                conditionsOr = conditionsOr.or(
                    JooqUtils.jsonExtract(MATERIAL, "\$[*].branchName", lower = true).like("%${s.lowercase()}%")
                )
            }
            where.and(conditionsOr)
        }
        if (!materialCommitId.isNullOrBlank()) {
            where.and(JooqUtils.jsonExtract(MATERIAL, "\$[*].newCommitId").like("%$materialCommitId%"))
        }
        if (!materialCommitMessage.isNullOrBlank()) {
            where.and(JooqUtils.jsonExtract(MATERIAL, "\$[*].newCommitComment").like("%$materialCommitMessage%"))
        }
        if (!status.isNullOrEmpty()) { // filterNotNull不能删
            where.and(STATUS.`in`(status.map { it.ordinal }))
        }
        if (!startUser.isNullOrEmpty()) {
            where.and(START_USER.`in`(startUser.map { it }))
        }
        if (!trigger.isNullOrEmpty()) { // filterNotNull不能删
            where.and(TRIGGER.`in`(trigger.map { it.name }))
        }
        if (queueTimeStartTime != null && queueTimeStartTime > 0) {
            where.and(QUEUE_TIME.ge(Timestamp(queueTimeStartTime).toLocalDateTime()))
        }
        if (queueTimeEndTime != null && queueTimeEndTime > 0) {
            where.and(QUEUE_TIME.le(Timestamp(queueTimeEndTime).toLocalDateTime()))
        }
        if (startTimeStartTime != null && startTimeStartTime > 0) {
            where.and(START_TIME.ge(Timestamp(startTimeStartTime).toLocalDateTime()))
        }
        if (startTimeEndTime != null && startTimeEndTime > 0) {
            where.and(START_TIME.le(Timestamp(startTimeEndTime).toLocalDateTime()))
        }
        if (endTimeStartTime != null && endTimeStartTime > 0) {
            where.and(END_TIME.ge(Timestamp(endTimeStartTime).toLocalDateTime()))
        }
        if (endTimeEndTime != null && endTimeEndTime > 0) {
            where.and(END_TIME.le(Timestamp(endTimeEndTime).toLocalDateTime()))
        }
        if (totalTimeMin != null && totalTimeMin > 0) {
            where.and(
                JooqUtils.timestampDiff(
                    part = DatePart.SECOND,
                    t1 = START_TIME.cast(Timestamp::class.java),
                    t2 = END_TIME.cast(Timestamp::class.java)
                ).greaterOrEqual(totalTimeMin)
            )
        }
        if (totalTimeMax != null && totalTimeMax > 0) {
            where.and(
                JooqUtils.timestampDiff(
                    part = DatePart.SECOND,
                    t1 = START_TIME.cast(Timestamp::class.java),
                    t2 = END_TIME.cast(Timestamp::class.java)
                ).lessOrEqual(totalTimeMax)
            )
        }
        if (!remark.isNullOrBlank()) {
            where.and(REMARK.like("%$remark%"))
        }
        if (buildNoStart != null && buildNoStart > 0) {
            where.and(BUILD_NUM.ge(buildNoStart))
        }
        if (buildNoEnd != null && buildNoEnd > 0) {
            where.and(BUILD_NUM.le(buildNoEnd))
        }
        if (!buildMsg.isNullOrBlank()) {
            where.and(BUILD_MSG.like("%$buildMsg%"))
        }
    }

    fun updateBuildRemark(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        remark: String?
    ) {
        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(REMARK, remark)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun updateRecommendVersion(dslContext: DSLContext, projectId: String, buildId: String, recommendVersion: String) {
        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(RECOMMEND_VERSION, recommendVersion)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun getBuildHistoryMaterial(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Collection<TPipelineBuildHistoryRecord> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .orderBy(BUILD_NUM.desc()).limit(DEFAULT_PAGE_SIZE)
                .fetch()
        }
    }

    fun getBuildByBuildNum(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildNum: Int
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_NUM.eq(buildNum))
                .fetchAny()
        }
    }

    fun getBuilds(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        buildStatus: Set<BuildStatus>?
    ): List<String> {
        with(T_PIPELINE_BUILD_HISTORY) {
            val dsl = dslContext.select(BUILD_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!buildStatus.isNullOrEmpty()) {
                dsl.and(STATUS.`in`(buildStatus.map { it.ordinal }))
            }
            return dsl.fetch(BUILD_ID)
        }
    }

    fun getBuildCount(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        buildStatus: Set<BuildStatus>?
    ): Int {
        with(T_PIPELINE_BUILD_HISTORY) {
            val dsl = dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!buildStatus.isNullOrEmpty()) {
                dsl.and(STATUS.`in`(buildStatus.map { it.ordinal }))
            }
            return dsl.fetchOne(0, Int::class.java)!!
        }
    }

    fun updateArtifactList(
        dslContext: DSLContext,
        artifactList: String?,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(ARTIFACT_INFO, artifactList)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun updateBuildMaterial(dslContext: DSLContext, projectId: String, buildId: String, material: String?) {
        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(MATERIAL, material)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun updateBuildStageStatus(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        stageStatus: List<BuildStageStatus>,
        oldBuildStatus: BuildStatus? = null,
        newBuildStatus: BuildStatus? = null,
        errorInfoList: List<ErrorInfo>? = null
    ): Int {
        with(T_PIPELINE_BUILD_HISTORY) {
            val update = dslContext.update(this)
                .set(STAGE_STATUS, JsonUtil.toJson(stageStatus, formatted = false))
            newBuildStatus?.let { update.set(STATUS, it.ordinal) }
            errorInfoList?.let { update.set(ERROR_INFO, JsonUtil.toJson(it, formatted = false)) }
            return if (oldBuildStatus == null) {
                update.where(BUILD_ID.eq(buildId))
                    .and(PROJECT_ID.eq(projectId))
                    .execute()
            } else {
                update.where(BUILD_ID.eq(buildId))
                    .and(PROJECT_ID.eq(projectId))
                    .and(STATUS.eq(oldBuildStatus.ordinal))
                    .execute()
            }
        }
    }

    fun getBuildParameters(dslContext: DSLContext, projectId: String, buildId: String): String? {
        with(T_PIPELINE_BUILD_HISTORY) {
            return dslContext.select(BUILD_PARAMETERS)
                .from(this)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .fetchAny(0, String::class.java)
        }
    }

    fun updateBuildParameters(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildParameters: Collection<BuildParameters>
    ): Boolean {
        with(T_PIPELINE_BUILD_HISTORY) {
            return dslContext.update(this)
                .set(BUILD_PARAMETERS, JsonUtil.toJson(buildParameters, formatted = false))
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId)).execute() == 1
        }
    }

    fun countBuildNumByTime(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(START_TIME.ge(startTime))
            conditions.add(END_TIME.lt(endTime))
            dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId).and(BUILD_ID.eq(buildId)))).fetchAny()
        }
    }

    fun countBuildNumByVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(VERSION.eq(version))
            dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }
}
