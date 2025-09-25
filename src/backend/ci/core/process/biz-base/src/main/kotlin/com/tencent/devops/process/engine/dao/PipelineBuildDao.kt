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

package com.tencent.devops.process.engine.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.archive.pojo.ArtifactQualityMetadataAnalytics
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_HISTORY
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_HISTORY_DEBUG
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.TPipelineBuildHistoryDebug
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryDebugRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.BuildRetryInfo
import com.tencent.devops.process.enums.HistorySearchType
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.app.StartBuildContext
import com.tencent.devops.process.pojo.code.WebhookInfo
import jakarta.ws.rs.core.Response
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Record2
import org.jooq.RecordMapper
import org.jooq.SelectConditionStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineBuildDao {

    companion object {
        private val mapper = PipelineBuildInfoJooqMapper()
        private val debugMapper = PipelineDebugBuildInfoJooqMapper()
        private const val DEFAULT_PAGE_SIZE = 50
    }

    fun create(dslContext: DSLContext, startBuildContext: StartBuildContext) {
        try {
            if (!startBuildContext.debug) {
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
                        CONCURRENCY_GROUP,
                        VERSION_NAME,
                        YAML_VERSION,
                        EXECUTE_COUNT
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
                        startBuildContext.concurrencyGroup,
                        startBuildContext.versionName,
                        startBuildContext.yamlVersion,
                        startBuildContext.executeCount
                    ).execute()
                }
            } else {
                with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
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
                        CONCURRENCY_GROUP,
                        YAML_VERSION,
                        RESOURCE_MODEL,
                        EXECUTE_COUNT
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
                        startBuildContext.concurrencyGroup,
                        startBuildContext.yamlVersion,
                        startBuildContext.debugModelStr,
                        startBuildContext.executeCount
                    ).execute()
                }
            }
        } catch (t: Throwable) {
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_START_WITH_ERROR,
                params = arrayOf(t.message ?: "")
            )
        }
    }

    fun updateBuildRetryInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        retryInfo: BuildRetryInfo
    ) {
        val result = with(T_PIPELINE_BUILD_HISTORY) {
            val update = dslContext.update(this)
                .setNull(END_TIME)
                .set(QUEUE_TIME, retryInfo.nowTime)
                .set(STATUS, retryInfo.status.ordinal)
                .set(CONCURRENCY_GROUP, retryInfo.concurrencyGroup)
                .set(EXECUTE_COUNT, retryInfo.executeCount)
            retryInfo.buildParameters?.let {
                update.set(BUILD_PARAMETERS, JsonUtil.toJson(it, formatted = false))
            }
            if (retryInfo.rebuild) update.set(START_TIME, retryInfo.nowTime)
            update.where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
        if (result != 1) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val update = dslContext.update(this)
                .setNull(END_TIME)
                .set(QUEUE_TIME, retryInfo.nowTime)
                .set(STATUS, retryInfo.status.ordinal)
                .set(CONCURRENCY_GROUP, retryInfo.concurrencyGroup)
                .set(EXECUTE_COUNT, retryInfo.executeCount)
            retryInfo.buildParameters?.let {
                update.set(BUILD_PARAMETERS, JsonUtil.toJson(it, formatted = false))
            }
            if (retryInfo.rebuild) update.set(START_TIME, retryInfo.nowTime)
            update.where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .execute()
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
    ): List<BuildInfo> {
        val normal = with(T_PIPELINE_BUILD_HISTORY) {
            val where = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (!statusSet.isNullOrEmpty()) {
                where.and(STATUS.`in`(statusSet.map { it.ordinal }))
            }
            where.fetch(mapper)
        }
        return if (normal.isEmpty()) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val where = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (!statusSet.isNullOrEmpty()) {
                where.and(STATUS.`in`(statusSet.map { it.ordinal }))
            }
            where.fetch(debugMapper)
        } else normal
    }

    fun countAllBuildWithStatus(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        status: Set<BuildStatus>
    ): Int {
        val normal = with(T_PIPELINE_BUILD_HISTORY) {
            val where = dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(STATUS.`in`(status.map { it.ordinal }))
            where.fetchOne(0, Int::class.java)!!
        }
        val debug = with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val where = dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(STATUS.`in`(status.map { it.ordinal }))
            where.fetchOne(0, Int::class.java)!!
        }
        return normal + debug
    }

    fun getBuildTasksByConcurrencyGroup(
        dslContext: DSLContext,
        projectId: String,
        concurrencyGroup: String,
        statusSet: List<BuildStatus>
    ): List<Record2<String, String>> {
        val normal = with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.select(PIPELINE_ID, BUILD_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(STATUS.`in`(statusSet.map { it.ordinal }))
                .and(CONCURRENCY_GROUP.eq(concurrencyGroup)).orderBy(START_TIME.asc())
                .fetch()
        }
        val debug = with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.select(PIPELINE_ID, BUILD_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(STATUS.`in`(statusSet.map { it.ordinal }))
                .and(CONCURRENCY_GROUP.eq(concurrencyGroup)).orderBy(START_TIME.asc())
                .fetch()
        }
        return normal.plus(debug)
    }

    fun getBuildTasksByConcurrencyGroupNull(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statusSet: List<BuildStatus>
    ): List<Record2<String, String>> {
        val normal = with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.select(PIPELINE_ID, BUILD_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(STATUS.`in`(statusSet.map { it.ordinal }))
                .and(CONCURRENCY_GROUP.isNull).orderBy(START_TIME.asc())
                .fetch()
        }
        val debug = with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.select(PIPELINE_ID, BUILD_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(STATUS.`in`(statusSet.map { it.ordinal }))
                .and(CONCURRENCY_GROUP.isNull).orderBy(START_TIME.asc())
                .fetch()
        }
        return normal.plus(debug)
    }

    /**
     * 查询BuildInfo，兼容所有运行时调用，不排除已删除的调试记录
     * @param dslContext: 事务上下文
     * @param projectId: 项目Id
     * @param buildId: 构建Id
     */
    fun getBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): BuildInfo? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .fetchAny(mapper)
        } ?: with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .fetchAny(debugMapper)
        }
    }

    /**
     * 查询BuildInfo，返回给用户侧的数据，需要排除已删除的调试记录
     * @param dslContext: 事务上下文
     * @param projectId: 项目Id
     * @param buildId: 构建Id
     */
    fun getUserBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): BuildInfo? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .fetchAny(mapper)
        } ?: with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .and(DELETE_TIME.isNull)
                .fetchAny(debugMapper)
        }
    }

    fun getStartUser(dslContext: DSLContext, projectId: String, buildId: String): String? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.select(START_USER)
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .fetchOne(0, String::class.java)
        } ?: with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.select(START_USER)
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
    ): List<BuildInfo> {
        val normal = with(T_PIPELINE_BUILD_HISTORY) {
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
                .fetch(mapper)
        }
        return if (normal.isEmpty()) {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val conditions = mutableListOf<Condition>()
                conditions.add(BUILD_ID.`in`(buildIds))
                // 增加过滤，对前端屏蔽已删除的构建
                conditions.add(DELETE_TIME.isNull)
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
                    .fetch(debugMapper)
            }
        } else normal
    }

    /**
     * 跨分库查所有的构建ID
     */
    fun listBuildInfoByBuildIdsOnly(
        dslContext: DSLContext,
        buildIds: Collection<String>
    ): List<BuildInfo> {
        val normal = with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .fetch(mapper)
        }
        val debug = with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .fetch(debugMapper)
        }
        return normal.plus(debug)
    }

    fun listPipelineBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int,
        updateTimeDesc: Boolean? = null
    ): Collection<BuildInfo> {
        val normal = with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            when (updateTimeDesc) {
                true -> select.orderBy(UPDATE_TIME.desc(), BUILD_ID)
                false -> select.orderBy(UPDATE_TIME.asc(), BUILD_ID)
                null -> select.orderBy(BUILD_NUM.desc())
            }
            select.limit(offset, limit)
                .fetch(mapper)
        }
        return if (normal.isEmpty()) {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val select = dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    // 增加过滤，对前端屏蔽已删除的构建
                    .and(DELETE_TIME.isNull)
                when (updateTimeDesc) {
                    true -> select.orderBy(UPDATE_TIME.desc(), BUILD_ID)
                    false -> select.orderBy(UPDATE_TIME.asc(), BUILD_ID)
                    null -> select.orderBy(BUILD_NUM.desc())
                }
                select.limit(offset, limit)
                    .fetch(debugMapper)
            }
        } else normal
    }

    fun listPipelineBuildNum(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int,
        debugVersion: Int?
    ): Collection<Int> {
        return if (debugVersion == null) {
            with(T_PIPELINE_BUILD_HISTORY) {
                dslContext.select(BUILD_NUM).from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .orderBy(BUILD_NUM.desc())
                    .limit(offset, limit)
                    .fetch(0, Int::class.java)
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                dslContext.select(BUILD_NUM).from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(VERSION.eq(debugVersion))
                    // 增加过滤，对前端屏蔽已删除的构建
                    .and(DELETE_TIME.isNull)
                    .orderBy(BUILD_NUM.desc())
                    .limit(offset, limit)
                    .fetch(0, Int::class.java)
            }
        }
    }

    fun getBuildInfoByBuildNum(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildNum: Int?,
        statusSet: Set<BuildStatus>?,
        debug: Boolean
    ): BuildInfo? {
        return if (debug) {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val select = dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    // 增加过滤，插件按照构建号的查询也屏蔽已删除构建
                    .and(DELETE_TIME.isNull)
                if (!statusSet.isNullOrEmpty()) {
                    select.and(STATUS.`in`(statusSet.map { it.ordinal }))
                }

                if (buildNum != null && buildNum > 0) {
                    select.and(BUILD_NUM.eq(buildNum))
                } else { // 取最新的
                    select.orderBy(BUILD_NUM.desc()).limit(1)
                }
                select.fetchOne(debugMapper)
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY) {
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
                select.fetchOne(mapper)
            }
        }
    }

    fun getOneQueueBuild(dslContext: DSLContext, projectId: String, pipelineId: String): BuildInfo? {
        val release = with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(STATUS.`in`(setOf(BuildStatus.QUEUE.ordinal, BuildStatus.QUEUE_CACHE.ordinal)))
                .orderBy(BUILD_NUM.asc()).limit(1)
            select.fetchAny(mapper)
        }
        val debug = with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(STATUS.`in`(setOf(BuildStatus.QUEUE.ordinal, BuildStatus.QUEUE_CACHE.ordinal)))
                .orderBy(BUILD_NUM.asc()).limit(1)
            select.fetchAny(debugMapper)
        }
        return when {
            release == null -> debug
            debug == null -> release
            release.queueTime > debug.queueTime -> debug
            else -> release
        }
    }

    fun getOneConcurrencyQueueBuild(
        dslContext: DSLContext,
        projectId: String,
        concurrencyGroup: String,
        pipelineId: String? = null
    ): BuildInfo? {
        val release = with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CONCURRENCY_GROUP.eq(concurrencyGroup))
                .and(STATUS.`in`(setOf(BuildStatus.QUEUE.ordinal, BuildStatus.QUEUE_CACHE.ordinal)))
            if (pipelineId != null) {
                select.and(PIPELINE_ID.eq(pipelineId))
            }
            select.orderBy(QUEUE_TIME.asc(), PIPELINE_ID, BUILD_NUM.asc()).limit(1)
            select.fetchAny(mapper)
        }
        val debug = with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CONCURRENCY_GROUP.eq(concurrencyGroup))
                .and(STATUS.`in`(setOf(BuildStatus.QUEUE.ordinal, BuildStatus.QUEUE_CACHE.ordinal)))
            if (pipelineId != null) {
                select.and(PIPELINE_ID.eq(pipelineId))
            }
            select.orderBy(QUEUE_TIME.asc(), PIPELINE_ID, BUILD_NUM.asc()).limit(1)
            select.fetchAny(debugMapper)
        }
        return when {
            release == null -> debug
            debug == null -> release
            release.queueTime > debug.queueTime -> debug
            else -> release
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
        debug: Boolean?
    ) {
        if (debug != true) {
            with(T_PIPELINE_BUILD_HISTORY) {
                val update = dslContext.update(this).set(STATUS, BuildStatus.RUNNING.ordinal)
                startTime?.let { update.set(START_TIME, startTime) }
                update.setNull(ERROR_INFO)
                update.setNull(EXECUTE_TIME)
                update.where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId))).execute()
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val update = dslContext.update(this).set(STATUS, BuildStatus.RUNNING.ordinal)
                startTime?.let { update.set(START_TIME, startTime) }
                update.setNull(ERROR_INFO)
                update.setNull(EXECUTE_TIME)
                update.where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId))).execute()
            }
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
        errorInfoList: List<ErrorInfo>?,
        debug: Boolean?
    ) {
        if (debug != true) {
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
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
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
    }

    /**
     * 取最近一次构建的参数
     */
    fun getLatestBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        debug: Boolean
    ): BuildInfo? {
        return if (debug) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    // 增加过滤，对前端屏蔽已删除的构建
                    DELETE_TIME.isNull
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny(debugMapper)
        } else with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId)
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny(mapper)
        }
    }

    /**
     * 取最近一次完成的构建
     */
    fun getLatestFinishedBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): BuildInfo? {
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
            select.fetchAny(mapper)
        } ?: with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    STATUS.notIn(
                        mutableListOf(
                            BuildStatus.RUNNING.ordinal, // 3 运行中
                            BuildStatus.QUEUE.ordinal // 13 排队（新）
                        )
                    ),
                    // 增加过滤，对前端屏蔽已删除的构建
                    DELETE_TIME.isNull
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny(debugMapper)
        }
    }

    /**
     * 取最近一次成功的构建
     */
    fun getLatestFailedBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): BuildInfo? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    STATUS.eq(1)
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny(mapper)
        } ?: with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    STATUS.eq(1),
                    // 增加过滤，对前端屏蔽已删除的构建
                    DELETE_TIME.isNull
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny(debugMapper)
        }
    }

    /**
     * 取最近一次失败的构建
     */
    fun getLatestSucceedBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): BuildInfo? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    STATUS.eq(0)
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny(mapper)
        } ?: with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    STATUS.eq(0),
                    // 增加过滤，对前端屏蔽已删除的构建
                    DELETE_TIME.isNull
                )
                .orderBy(BUILD_NUM.desc()).limit(1)
            select.fetchAny(debugMapper)
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
        val success = with(T_PIPELINE_BUILD_HISTORY) {
            val update = dslContext.update(this)
                .set(STATUS, newBuildStatus.ordinal)
            startTime?.let {
                update.set(START_TIME, it)
            }
            errorInfoList?.let {
                update.set(ERROR_INFO, JsonUtil.toJson(it, formatted = false))
            }
            update.where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(STATUS.eq(oldBuildStatus.ordinal))
                .execute() == 1
        }
        return if (!success) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
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
        } else true
    }

    fun updateExecuteCount(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        executeCount: Int
    ) {
        val success = with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(EXECUTE_COUNT, executeCount)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute() == 1
        }
        if (!success) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.update(this)
                .set(EXECUTE_COUNT, executeCount)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        status: List<BuildStatus>? = null,
        startTimeEndTime: Long? = null,
        debugVersion: Int? = null
    ): Int {
        return if (debugVersion == null) {
            with(T_PIPELINE_BUILD_HISTORY) {
                val where = dslContext.selectCount().from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                if (!status.isNullOrEmpty()) {
                    where.and(STATUS.`in`(status.map { it.ordinal }))
                }
                if (startTimeEndTime != null && startTimeEndTime > 0) {
                    where.and(START_TIME.le(Timestamp(startTimeEndTime).toLocalDateTime()))
                }
                where.fetchOne(0, Int::class.java)!!
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val where = dslContext.selectCount().from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(VERSION.eq(debugVersion))
                if (!status.isNullOrEmpty()) {
                    where.and(STATUS.`in`(status.map { it.ordinal }))
                }
                if (startTimeEndTime != null && startTimeEndTime > 0) {
                    where.and(START_TIME.le(Timestamp(startTimeEndTime).toLocalDateTime()))
                }
                // 增加过滤，对前端屏蔽已删除的构建
                where.and(DELETE_TIME.isNull)
                where.fetchOne(0, Int::class.java)!!
            }
        }
    }

    fun countByStatus(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        status: List<BuildStatus>? = null,
        startTimeEndTime: Long? = null,
        onlyDebug: Boolean? = false
    ): Int {
        return if (onlyDebug != true) {
            with(T_PIPELINE_BUILD_HISTORY) {
                val where = dslContext.selectCount().from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                if (!status.isNullOrEmpty()) {
                    where.and(STATUS.`in`(status.map { it.ordinal }))
                }
                if (startTimeEndTime != null && startTimeEndTime > 0) {
                    where.and(START_TIME.le(Timestamp(startTimeEndTime).toLocalDateTime()))
                }
                where.fetchOne(0, Int::class.java)!!
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val where = dslContext.selectCount().from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                if (!status.isNullOrEmpty()) {
                    where.and(STATUS.`in`(status.map { it.ordinal }))
                }
                if (startTimeEndTime != null && startTimeEndTime > 0) {
                    where.and(START_TIME.le(Timestamp(startTimeEndTime).toLocalDateTime()))
                }
                // 增加过滤，对前端屏蔽已删除的构建
                where.and(DELETE_TIME.isNull)
                where.fetchOne(0, Int::class.java)!!
            }
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
        startUser: List<String>?,
        debug: Boolean?,
        triggerAlias: List<String>?,
        triggerBranch: List<String>?,
        triggerUser: List<String>?
    ): Int {
        return if (debug != true) {
            with(T_PIPELINE_BUILD_HISTORY) {
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
                    buildMsg = buildMsg,
                    triggerAlias = triggerAlias,
                    triggerBranch = triggerBranch,
                    triggerUser = triggerUser
                )
                where.fetchOne(0, Int::class.java)!!
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val where = dslContext.selectCount()
                    .from(this)
                    .where(PROJECT_ID.eq(projectId)).and(PIPELINE_ID.eq(pipelineId))
                makeDebugCondition(
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
                    buildMsg = buildMsg,
                    triggerAlias = triggerAlias,
                    triggerBranch = triggerBranch,
                    triggerUser = triggerUser
                )
                where.fetchOne(0, Int::class.java)!!
            }
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
        updateTimeDesc: Boolean? = null,
        debug: Boolean?,
        triggerAlias: List<String>?,
        triggerBranch: List<String>?,
        triggerUser: List<String>?
    ): Collection<BuildInfo> {
        return if (debug != true) {
            with(T_PIPELINE_BUILD_HISTORY) {
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
                    buildMsg = buildMsg,
                    triggerAlias = triggerAlias,
                    triggerBranch = triggerBranch,
                    triggerUser = triggerUser
                )

                when (updateTimeDesc) {
                    true -> where.orderBy(UPDATE_TIME.desc(), BUILD_ID)
                    false -> where.orderBy(UPDATE_TIME.asc(), BUILD_ID)
                    null -> where.orderBy(BUILD_NUM.desc())
                }

                where.limit(offset, limit).fetch(mapper)
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val where = dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId)).and(PIPELINE_ID.eq(pipelineId))
                makeDebugCondition(
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
                    buildMsg = buildMsg,
                    triggerAlias = triggerAlias,
                    triggerBranch = triggerBranch,
                    triggerUser = triggerUser
                )
                when (updateTimeDesc) {
                    true -> where.orderBy(UPDATE_TIME.desc(), BUILD_ID)
                    false -> where.orderBy(UPDATE_TIME.asc(), BUILD_ID)
                    null -> where.orderBy(BUILD_NUM.desc())
                }

                where.limit(offset, limit).fetch(debugMapper)
            }
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
        buildMsg: String?,
        triggerAlias: List<String>?,
        triggerBranch: List<String>?,
        triggerUser: List<String>?
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
        if (!triggerAlias.isNullOrEmpty() && triggerAlias.first().isNotBlank()) {
            var conditionsOr: Condition

            conditionsOr = JooqUtils.jsonExtract(t1 = WEBHOOK_INFO, t2 = "\$.webhookAliasName", lower = true)
                .like("%${triggerAlias.first().lowercase()}%")

            triggerAlias.forEachIndexed { index, s ->
                if (index == 0) return@forEachIndexed
                conditionsOr = conditionsOr.or(
                    JooqUtils.jsonExtract(WEBHOOK_INFO, "\$.webhookAliasName", lower = true)
                        .like("%${s.lowercase()}%")
                )
            }
            where.and(conditionsOr)
        }
        if (!triggerBranch.isNullOrEmpty() && triggerBranch.first().isNotBlank()) {
            var conditionsOr: Condition

            conditionsOr = JooqUtils.jsonExtract(WEBHOOK_INFO, "\$.webhookBranch", lower = true)
                .like("%${triggerBranch.first().lowercase()}%")

            triggerBranch.forEachIndexed { index, s ->
                if (index == 0) return@forEachIndexed
                conditionsOr = conditionsOr.or(
                    JooqUtils.jsonExtract(WEBHOOK_INFO, "\$.webhookBranch", lower = true)
                        .like("%${s.lowercase()}%")
                )
            }
            where.and(conditionsOr)
        }
        if (!triggerUser.isNullOrEmpty()) { // filterNotNull不能删
            where.and(TRIGGER_USER.`in`(triggerUser))
        }
    }

    private fun TPipelineBuildHistoryDebug.makeDebugCondition(
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
        buildMsg: String?,
        triggerAlias: List<String>?,
        triggerBranch: List<String>?,
        triggerUser: List<String>?
    ) {
        // 增加过滤，对前端屏蔽已删除的构建
        where.and(DELETE_TIME.isNull)
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
        if (!triggerAlias.isNullOrEmpty() && triggerAlias.first().isNotBlank()) {
            var conditionsOr: Condition

            conditionsOr = JooqUtils.jsonExtract(t1 = WEBHOOK_INFO, t2 = "\$.webhookAliasName", lower = true)
                .like("%${triggerAlias.first().lowercase()}%")

            triggerAlias.forEachIndexed { index, s ->
                if (index == 0) return@forEachIndexed
                conditionsOr = conditionsOr.or(
                    JooqUtils.jsonExtract(WEBHOOK_INFO, "\$.webhookAliasName", lower = true)
                        .like("%${s.lowercase()}%")
                )
            }
            where.and(conditionsOr)
        }
        if (!triggerBranch.isNullOrEmpty() && triggerBranch.first().isNotBlank()) {
            var conditionsOr: Condition

            conditionsOr = JooqUtils.jsonExtract(WEBHOOK_INFO, "\$.webhookBranch", lower = true)
                .like("%${triggerBranch.first().lowercase()}%")

            triggerBranch.forEachIndexed { index, s ->
                if (index == 0) return@forEachIndexed
                conditionsOr = conditionsOr.or(
                    JooqUtils.jsonExtract(WEBHOOK_INFO, "\$.webhookBranch", lower = true)
                        .like("%${s.lowercase()}%")
                )
            }
            where.and(conditionsOr)
        }
        if (!triggerUser.isNullOrEmpty()) { // filterNotNull不能删
            where.and(TRIGGER_USER.`in`(triggerUser))
        }
    }

    fun updateBuildRemark(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        remark: String?
    ) {
        val success = with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(REMARK, remark)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute() == 1
        }
        if (!success) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.update(this)
                .set(REMARK, remark)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute() == 1
        }
    }

    fun updateRecommendVersion(dslContext: DSLContext, projectId: String, buildId: String, recommendVersion: String) {
        val success = with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(RECOMMEND_VERSION, recommendVersion)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .execute() == 1
        }
        if (!success) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.update(this)
                .set(RECOMMEND_VERSION, recommendVersion)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    /**
     * 构建历史搜索下拉框
     */
    fun listHistorySearchOptions(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        debugVersion: Int?,
        type: HistorySearchType
    ): Collection<BuildInfo> {
        return if (debugVersion == null) {
            with(T_PIPELINE_BUILD_HISTORY) {
                val where = dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                when (type) {
                    HistorySearchType.MATERIAL ->
                        where.and(MATERIAL.isNotNull)

                    HistorySearchType.TRIGGER ->
                        where.and(WEBHOOK_INFO.isNotNull)
                }
                where.orderBy(BUILD_NUM.desc()).limit(DEFAULT_PAGE_SIZE)
                    .fetch(mapper)
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val where = dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                    .and(VERSION.eq(debugVersion))
                when (type) {
                    HistorySearchType.MATERIAL ->
                        where.and(MATERIAL.isNotNull)

                    HistorySearchType.TRIGGER ->
                        where.and(WEBHOOK_INFO.isNotNull)
                }
                where.orderBy(BUILD_NUM.desc()).limit(DEFAULT_PAGE_SIZE)
                    .fetch(debugMapper)
            }
        }
    }

    fun getBuildByBuildNum(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildNum: Int,
        debugVersion: Int?
    ): BuildInfo? {
        return if (debugVersion == null) {
            with(T_PIPELINE_BUILD_HISTORY) {
                dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(BUILD_NUM.eq(buildNum))
                    .fetchAny(mapper)
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(BUILD_NUM.eq(buildNum))
                    .and(VERSION.eq(debugVersion))
                    .fetchAny(debugMapper)
            }
        }
    }

    fun getBuilds(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        buildStatus: Set<BuildStatus>?,
        debugVersion: Int?
    ): List<String> {
        return if (debugVersion == null) {
            with(T_PIPELINE_BUILD_HISTORY) {
                val dsl = dslContext.select(BUILD_ID).from(this)
                    .where(PROJECT_ID.eq(projectId))
                if (!pipelineId.isNullOrBlank()) {
                    dsl.and(PIPELINE_ID.eq(pipelineId))
                }
                if (!buildStatus.isNullOrEmpty()) {
                    dsl.and(STATUS.`in`(buildStatus.map { it.ordinal }))
                }
                dsl.fetch(BUILD_ID)
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val dsl = dslContext.select(BUILD_ID).from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(VERSION.eq(debugVersion))
                if (!pipelineId.isNullOrBlank()) {
                    dsl.and(PIPELINE_ID.eq(pipelineId))
                }
                if (!buildStatus.isNullOrEmpty()) {
                    dsl.and(STATUS.`in`(buildStatus.map { it.ordinal }))
                }
                dsl.fetch(BUILD_ID)
            }
        }
    }

    fun updateArtifactList(
        dslContext: DSLContext,
        artifactList: String?,
        artifactQualityList: String?,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Int {
        val success = with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(ARTIFACT_INFO, artifactList)
                .set(ARTIFACT_QUALITY_INFO, artifactQualityList)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute() == 1
        }
        return if (!success) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.update(this)
                .set(ARTIFACT_INFO, artifactList)
                .set(ARTIFACT_QUALITY_INFO, artifactQualityList)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        } else 1
    }

    fun updateBuildMaterial(dslContext: DSLContext, projectId: String, buildId: String, material: String?) {
        val success = with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(MATERIAL, material)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .execute() == 1
        }
        if (!success) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
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
        val success = with(T_PIPELINE_BUILD_HISTORY) {
            val update = dslContext.update(this)
                .set(STAGE_STATUS, JsonUtil.toJson(stageStatus, formatted = false))
            newBuildStatus?.let { update.set(STATUS, it.ordinal) }
            errorInfoList?.let { update.set(ERROR_INFO, JsonUtil.toJson(it, formatted = false)) }
            if (oldBuildStatus == null) {
                update.where(BUILD_ID.eq(buildId))
                    .and(PROJECT_ID.eq(projectId))
                    .execute() == 1
            } else {
                update.where(BUILD_ID.eq(buildId))
                    .and(PROJECT_ID.eq(projectId))
                    .and(STATUS.eq(oldBuildStatus.ordinal))
                    .execute() == 1
            }
        }
        return if (!success) with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
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
        } else 1
    }

    fun getBuildParameters(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): String? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.select(BUILD_PARAMETERS)
                .from(this)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .fetchAny(0, String::class.java)
        } ?: with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.select(BUILD_PARAMETERS)
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
        buildParameters: Collection<BuildParameters>,
        debug: Boolean
    ): Boolean {
        return if (!debug) {
            with(T_PIPELINE_BUILD_HISTORY) {
                dslContext.update(this)
                    .set(BUILD_PARAMETERS, JsonUtil.toJson(buildParameters, formatted = false))
                    .where(BUILD_ID.eq(buildId))
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId)).execute() == 1
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                dslContext.update(this)
                    .set(BUILD_PARAMETERS, JsonUtil.toJson(buildParameters, formatted = false))
                    .where(BUILD_ID.eq(buildId))
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId)).execute() == 1
            }
        }
    }

    fun countBuildNumByTime(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        debugVersion: Int?
    ): Int {
        return if (debugVersion == null) {
            with(T_PIPELINE_BUILD_HISTORY) {
                val conditions = mutableListOf<Condition>()
                conditions.add(PROJECT_ID.eq(projectId))
                conditions.add(PIPELINE_ID.eq(pipelineId))
                conditions.add(START_TIME.ge(startTime))
                conditions.add(END_TIME.lt(endTime))
                dslContext.selectCount().from(this)
                    .where(conditions)
                    .fetchOne(0, Int::class.java)!!
            }
        } else {
            with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
                val conditions = mutableListOf<Condition>()
                conditions.add(PROJECT_ID.eq(projectId))
                conditions.add(PIPELINE_ID.eq(pipelineId))
                conditions.add(START_TIME.ge(startTime))
                conditions.add(END_TIME.lt(endTime))
                conditions.add(VERSION.eq(debugVersion))
                dslContext.selectCount().from(this)
                    .where(conditions)
                    .fetchOne(0, Int::class.java)!!
            }
        }
    }

    fun getBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): BuildInfo? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .fetchAny(mapper)
        } ?: with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .fetchAny(debugMapper)
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

    fun batchCountBuildNumByVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        versions: Set<Int>
    ): List<Record2<Int, Int>> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(VERSION.`in`(versions))
            dslContext.select(VERSION, DSL.count())
                .from(this)
                .where(conditions)
                .groupBy(VERSION)
                .fetch()
        }
    }

    fun getDebugResourceStr(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): String? {
        with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            return dslContext.select(RESOURCE_MODEL)
                .from(this)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .fetchAny(0, String::class.java)
        }
    }

    fun getDebugHistory(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int? = null
    ): List<BuildInfo> {
        with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .and(DELETE_TIME.isNotNull)
            version?.let { select.and(VERSION.eq(version)) }
            return select.fetch(debugMapper)
        }
    }

    fun getDebugFlag(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): Boolean {
        with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            return dslContext.selectCount().from(this).where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .fetchOne(0, Int::class.java)!! > 0
        }
    }

    fun clearDebugHistory(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int? = null
    ): Int {
        with(T_PIPELINE_BUILD_HISTORY_DEBUG) {
            val now = LocalDateTime.now()
            val update = dslContext.update(this)
                .set(DELETE_TIME, now)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
            version?.let { update.and(VERSION.eq(version)) }
            return update.execute()
        }
    }

    class PipelineBuildInfoJooqMapper : RecordMapper<TPipelineBuildHistoryRecord, BuildInfo> {
        override fun map(record: TPipelineBuildHistoryRecord?): BuildInfo? {
            return record?.let { t ->
                BuildInfo(
                    projectId = t.projectId,
                    pipelineId = t.pipelineId,
                    buildId = t.buildId,
                    version = t.version,
                    versionName = t.versionName ?: "V${t.version}(init)",
                    yamlVersion = t.yamlVersion,
                    buildNum = t.buildNum,
                    trigger = t.trigger,
                    status = BuildStatus.values()[t.status],
                    // queueTime在数据库中必定会写值，不为空，以防万一用当前时间兜底
                    queueTime = t.queueTime?.timestampmilli() ?: LocalDateTime.now().timestampmilli(),
                    startUser = t.startUser,
                    triggerUser = t.triggerUser ?: t.startUser ?: "",
                    startTime = t.startTime?.timestampmilli(),
                    endTime = t.endTime?.timestampmilli(),
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
                    artifactList = t.artifactInfo?.let { self ->
                        JsonUtil.getObjectMapper().readValue(self) as List<FileInfo>
                    },
                    artifactQualityList = t.artifactQualityInfo?.let { self ->
                        JsonUtil.getObjectMapper().readValue(self) as List<ArtifactQualityMetadataAnalytics>
                    },
                    retryFlag = t.isRetry,
                    executeCount = t.executeCount ?: 1,
                    executeTime = t.executeTime ?: 0,
                    concurrencyGroup = t.concurrencyGroup,
                    webhookType = t.webhookType,
                    webhookInfo = t.webhookInfo?.let { JsonUtil.to(t.webhookInfo, WebhookInfo::class.java) },
                    buildMsg = t.buildMsg,
                    errorType = t.errorType,
                    errorCode = t.errorCode,
                    errorMsg = t.errorMsg,
                    material = t.material?.let {
                        JsonUtil.getObjectMapper().readValue(it) as List<PipelineBuildMaterial>
                    },
                    updateTime = t.updateTime.timestampmilli(),
                    recommendVersion = t.recommendVersion,
                    buildNumAlias = t.buildNumAlias,
                    remark = t.remark,
                    debug = false // #8164 原历史表中查出的记录均为非调试的记录
                )
            }
        }
    }

    class PipelineDebugBuildInfoJooqMapper : RecordMapper<TPipelineBuildHistoryDebugRecord, BuildInfo> {
        override fun map(record: TPipelineBuildHistoryDebugRecord?): BuildInfo? {
            return record?.let { t ->
                BuildInfo(
                    projectId = t.projectId,
                    pipelineId = t.pipelineId,
                    buildId = t.buildId,
                    version = t.version,
                    versionName = null,
                    yamlVersion = t.yamlVersion,
                    buildNum = t.buildNum,
                    trigger = t.trigger,
                    status = BuildStatus.values()[t.status],
                    queueTime = t.queueTime?.timestampmilli() ?: 0L,
                    startUser = t.startUser,
                    triggerUser = t.triggerUser ?: t.startUser ?: "",
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
                    executeCount = t.executeCount ?: 1,
                    executeTime = t.executeTime ?: 0,
                    concurrencyGroup = t.concurrencyGroup,
                    webhookType = t.webhookType,
                    webhookInfo = t.webhookInfo?.let { JsonUtil.to(t.webhookInfo, WebhookInfo::class.java) },
                    artifactList = t.artifactInfo?.let { self ->
                        JsonUtil.to(self, object : TypeReference<List<FileInfo>?>() {})
                    },
                    artifactQualityList = t.artifactQualityInfo?.let { self ->
                        JsonUtil.getObjectMapper().readValue(self) as List<ArtifactQualityMetadataAnalytics>
                    },
                    buildMsg = t.buildMsg,
                    errorType = t.errorType,
                    errorCode = t.errorCode,
                    errorMsg = t.errorMsg,
                    material = t.material?.let {
                        JsonUtil.getObjectMapper().readValue(it) as List<PipelineBuildMaterial>
                    },
                    updateTime = t.updateTime.timestampmilli(),
                    recommendVersion = t.recommendVersion,
                    buildNumAlias = t.buildNumAlias,
                    remark = t.remark,
                    debug = true // #8164 原历史表中查出的记录均为非调试的记录
                )
            }
        }
    }
}
