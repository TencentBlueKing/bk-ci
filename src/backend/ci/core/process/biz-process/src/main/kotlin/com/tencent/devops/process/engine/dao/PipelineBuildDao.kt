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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.dao

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_HISTORY
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.pojo.BuildStageStatus
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class PipelineBuildDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        version: Int,
        buildNum: Int,
        trigger: String,
        status: BuildStatus,
        startUser: String,
        triggerUser: String,
        taskCount: Int,
        firstTaskId: String,
        channelCode: ChannelCode,
        parentBuildId: String?,
        parentTaskId: String?,
        webhookType: String?,
        webhookInfo: String?
    ) {

        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.insertInto(
                this,
                BUILD_ID,
                BUILD_NUM,
                PROJECT_ID,
                PIPELINE_ID,
                PARENT_BUILD_ID,
                PARENT_TASK_ID,
                START_TIME,
                START_USER,
                TRIGGER_USER,
                STATUS,
                TRIGGER,
                TASK_COUNT,
                FIRST_TASK_ID,
                CHANNEL,
                VERSION,
                QUEUE_TIME,
                WEBHOOK_TYPE,
                WEBHOOK_INFO
            ).values(
                buildId,
                buildNum,
                projectId,
                pipelineId,
                parentBuildId,
                parentTaskId,
                LocalDateTime.now(),
                startUser,
                triggerUser,
                status.ordinal,
                trigger,
                taskCount,
                firstTaskId,
                channelCode.name,
                version,
                LocalDateTime.now(),
                webhookType,
                webhookInfo
            ).execute()
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
            if (statusSet != null && statusSet.isNotEmpty()) {
                val statusIntSet = mutableSetOf<Int>()
                if (statusSet.isNotEmpty()) {
                    statusSet.forEach {
                        statusIntSet.add(it.ordinal)
                    }
                }
                where.and(STATUS.`in`(statusIntSet))
            }
            where.fetch()
        }
    }

    fun getBuildInfo(
        dslContext: DSLContext,
        buildId: String
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetchAny()
        }
    }

    /**
     * 旧接口适配
     */
    fun listBuildInfoByBuildIds(
        dslContext: DSLContext,
        buildIds: Collection<String>
    ): Result<TPipelineBuildHistoryRecord> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .fetch()
        }
    }

    fun deletePipelineBuild(dslContext: DSLContext, buildId: String) {
        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.delete(this)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deletePipelineBuilds(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun listPipelineBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): Collection<TPipelineBuildHistoryRecord> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .orderBy(BUILD_NUM.desc())
                .limit(offset, limit)
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

            if (statusSet != null && statusSet.isNotEmpty()) {
                val statusIntSet = mutableSetOf<Int>()
                if (statusSet.isNotEmpty()) {
                    statusSet.forEach {
                        statusIntSet.add(it.ordinal)
                    }
                }
                select.and(STATUS.`in`(statusIntSet))
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
                .and(STATUS.eq(BuildStatus.QUEUE.ordinal))
                .orderBy(BUILD_NUM.asc()).limit(1)
            select.fetchAny()
        }
    }

    /**
     * 1：开始构建
     */
    fun startBuild(dslContext: DSLContext, buildId: String, retry: Boolean = false) {
        with(T_PIPELINE_BUILD_HISTORY) {
            val update = dslContext.update(this).set(STATUS, BuildStatus.RUNNING.ordinal)
            if (!retry) {
                update.set(START_TIME, LocalDateTime.now())
            }
            update.set(IS_RETRY, retry)
            update.where(BUILD_ID.eq(buildId)).execute()
        }
    }

    /**
     * 2:结束构建
     */
    fun finishBuild(
        dslContext: DSLContext,
        buildId: String,
        buildStatus: BuildStatus,
        executeTime: Long?,
        buildParameters: String?,
        recommendVersion: String?,
        remark: String? = null,
        errorInfoList: List<ErrorInfo>?
    ) {
        with(T_PIPELINE_BUILD_HISTORY) {
            val baseQuery = dslContext.update(this)
                .set(STATUS, buildStatus.ordinal)
                .set(END_TIME, LocalDateTime.now())
                .set(EXECUTE_TIME, executeTime)
                .set(BUILD_PARAMETERS, buildParameters)
                .set(RECOMMEND_VERSION, recommendVersion)

            if (!remark.isNullOrBlank()) {
                baseQuery.set(REMARK, remark)
            }

            if (errorInfoList != null) {
                baseQuery.set(ERROR_INFO, JsonUtil.toJson(errorInfoList))
            } else {
                baseQuery.setNull(ERROR_INFO)
            }

            baseQuery.where(BUILD_ID.eq(buildId)).execute()
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
    fun getLatestFinishedBuild(dslContext: DSLContext, projectId: String, pipelineId: String): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val select = dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId),
                    PROJECT_ID.eq(projectId),
                    STATUS.notIn(
                        mutableListOf(
                            3, // 3 运行中
                            13 // 13 排队（新）
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
    fun getLatestFailedBuild(dslContext: DSLContext, projectId: String, pipelineId: String): TPipelineBuildHistoryRecord? {
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
    fun getLatestSuccessedBuild(dslContext: DSLContext, projectId: String, pipelineId: String): TPipelineBuildHistoryRecord? {
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
        buildId: String,
        oldBuildStatus: BuildStatus,
        newBuildStatus: BuildStatus
    ): Boolean {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(STATUS, newBuildStatus.ordinal)
                .where(BUILD_ID.eq(buildId)).and(STATUS.eq(oldBuildStatus.ordinal))
                .execute()
        } == 1
    }

    fun updateStageCancelStatus(
        dslContext: DSLContext,
        buildId: String
    ): Boolean {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(END_TIME, LocalDateTime.now())
                .where(BUILD_ID.eq(buildId)).and(STATUS.eq(BuildStatus.STAGE_SUCCESS.ordinal))
                .execute()
        } == 1
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
                startTime = t.startTime?.timestampmilli() ?: 0L,
                endTime = t.endTime?.timestampmilli() ?: 0L,
                taskCount = t.taskCount,
                firstTaskId = t.firstTaskId,
                parentBuildId = t.parentBuildId,
                parentTaskId = t.parentTaskId,
                channelCode = ChannelCode.valueOf(t.channel),
                errorInfoList = try {
                    if (t.errorInfo != null) JsonUtil.getObjectMapper().readValue(t.errorInfo) as List<ErrorInfo> else null
                } catch (e: Exception) {
                    null
                }
            )
        }
    }

    fun count(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetchOne(0, Int::class.java)
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
        buildNoEnd: Int?
    ): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val where = dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (materialAlias != null && materialAlias.isNotEmpty() && materialAlias.first().isNotBlank()) {
                var conditionsOr: Condition
                conditionsOr =
                    jsonExtract(MATERIAL, "\$[*].aliasName", true).like("%${materialAlias.first().toLowerCase()}%")
                materialAlias.forEachIndexed { index, s ->
                    if (index == 0) return@forEachIndexed
                    conditionsOr =
                        conditionsOr.or(jsonExtract(MATERIAL, "\$[*].aliasName", true).like("%${s.toLowerCase()}%"))
                }
                where.and(conditionsOr)
            }
            if (materialUrl != null && materialUrl.isNotEmpty()) {
                where.and(jsonExtract(MATERIAL, "\$[*].url").like("%$materialUrl%"))
            }
            if (materialBranch != null && materialBranch.isNotEmpty() && materialBranch.first().isNotBlank()) {
                var conditionsOr: Condition
                conditionsOr =
                    jsonExtract(MATERIAL, "\$[*].branchName", true).like("%${materialBranch.first().toLowerCase()}%")
                materialBranch.forEachIndexed { index, s ->
                    if (index == 0) return@forEachIndexed
                    conditionsOr =
                        conditionsOr.or(jsonExtract(MATERIAL, "\$[*].branchName", true).like("%${s.toLowerCase()}%"))
                }
                where.and(conditionsOr)
            }
            if (materialCommitId != null && materialCommitId.isNotEmpty()) {
                where.and(jsonExtract(MATERIAL, "\$[*].newCommitId").like("%$materialCommitId%"))
            }
            if (materialCommitMessage != null && materialCommitMessage.isNotEmpty()) {
                where.and(jsonExtract(MATERIAL, "\$[*].newCommitComment").like("%$materialCommitMessage%"))
            }
            if (status != null && status.isNotEmpty()) { // filterNotNull不能删
                where.and(STATUS.`in`(status.map { it.ordinal }))
            }
            if (trigger != null && trigger.isNotEmpty()) { // filterNotNull不能删
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
                    timestampDiff(
                        DatePart.SECOND,
                        START_TIME.cast(java.sql.Timestamp::class.java),
                        END_TIME.cast(java.sql.Timestamp::class.java)
                    ).greaterOrEqual(totalTimeMin)
                )
            }
            if (totalTimeMax != null && totalTimeMax > 0) {
                where.and(
                    timestampDiff(
                        DatePart.SECOND,
                        START_TIME.cast(java.sql.Timestamp::class.java),
                        END_TIME.cast(java.sql.Timestamp::class.java)
                    ).lessOrEqual(totalTimeMax)
                )
            }
            if (remark != null && remark.isNotEmpty()) {
                where.and(REMARK.like("%$remark%"))
            }
            if (buildNoStart != null && buildNoStart > 0) {
                where.and(BUILD_NUM.ge(buildNoStart))
            }
            if (buildNoEnd != null && buildNoEnd > 0) {
                where.and(BUILD_NUM.le(buildNoEnd))
            }
            where.fetchOne(0, Int::class.java)
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
        buildNoEnd: Int?
    ): Collection<TPipelineBuildHistoryRecord> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val where = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (materialAlias != null && materialAlias.isNotEmpty() && materialAlias.first().isNotBlank()) {
                var conditionsOr: Condition
                conditionsOr =
                    jsonExtract(MATERIAL, "\$[*].aliasName", true).like("%${materialAlias.first().toLowerCase()}%")
                materialAlias.forEachIndexed { index, s ->
                    if (index == 0) return@forEachIndexed
                    conditionsOr =
                        conditionsOr.or(jsonExtract(MATERIAL, "\$[*].aliasName", true).like("%${s.toLowerCase()}%"))
                }
                where.and(conditionsOr)
            }
            if (materialUrl != null && materialUrl.isNotEmpty()) {
                where.and(jsonExtract(MATERIAL, "\$[*].url").like("%$materialUrl%"))
            }
            if (materialBranch != null && materialBranch.isNotEmpty() && materialBranch.first().isNotBlank()) {
                var conditionsOr: Condition
                conditionsOr =
                    jsonExtract(MATERIAL, "\$[*].branchName", true).like("%${materialBranch.first().toLowerCase()}%")
                materialBranch.forEachIndexed { index, s ->
                    if (index == 0) return@forEachIndexed
                    conditionsOr =
                        conditionsOr.or(jsonExtract(MATERIAL, "\$[*].branchName", true).like("%${s.toLowerCase()}%"))
                }
                where.and(conditionsOr)
            }
            if (materialCommitId != null && materialCommitId.isNotEmpty()) {
                where.and(jsonExtract(MATERIAL, "\$[*].newCommitId").like("%$materialCommitId%"))
            }
            if (materialCommitMessage != null && materialCommitMessage.isNotEmpty()) {
                where.and(jsonExtract(MATERIAL, "\$[*].newCommitComment").like("%$materialCommitMessage%"))
            }
            if (status != null && status.isNotEmpty()) { // filterNotNull不能删
                where.and(STATUS.`in`(status.map { it.ordinal }))
            }
            if (trigger != null && trigger.isNotEmpty()) { // filterNotNull不能删
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
                    timestampDiff(
                        DatePart.SECOND,
                        START_TIME.cast(java.sql.Timestamp::class.java),
                        END_TIME.cast(java.sql.Timestamp::class.java)
                    ).greaterOrEqual(totalTimeMin)
                )
            }
            if (totalTimeMax != null && totalTimeMax > 0) {
                where.and(
                    timestampDiff(
                        DatePart.SECOND,
                        START_TIME.cast(java.sql.Timestamp::class.java),
                        END_TIME.cast(java.sql.Timestamp::class.java)
                    ).lessOrEqual(totalTimeMax)
                )
            }
            if (remark != null && remark.isNotEmpty()) {
                where.and(REMARK.like("%$remark%"))
            }
            if (buildNoStart != null && buildNoStart > 0) {
                where.and(BUILD_NUM.ge(buildNoStart))
            }
            if (buildNoEnd != null && buildNoEnd > 0) {
                where.and(BUILD_NUM.le(buildNoEnd))
            }
            where.orderBy(BUILD_NUM.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun updateBuildRemark(dslContext: DSLContext, buildId: String, remark: String?) {
        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(REMARK, remark)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun countAllByStatus(dslContext: DSLContext, status: BuildStatus): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectCount().from(this)
                .where(STATUS.eq(status.ordinal))
                .fetchOne(0, Int::class.java)
        }
    }

    fun countNotEmptyArtifact(dslContext: DSLContext, startTime: Long, endTime: Long): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectCount().from(this)
                .where(ARTIFACT_INFO.isNotNull)
                .and(ARTIFACT_INFO.notEqual("[ ]"))
                .and(START_TIME.le(Timestamp(startTime).toLocalDateTime()))
                .and(START_TIME.ge(Timestamp(endTime).toLocalDateTime()))
                .fetchOne(0, Int::class.java)
        }
    }

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>, t2: Field<Timestamp>): Field<Long> {
        return DSL.field(
            "timestampdiff({0}, {1}, {2})",
            Long::class.java, DSL.keyword(part.toSQL()), t1, t2
        )
    }

    fun jsonExtract(t1: Field<String>, t2: String, lower: Boolean = false): Field<String> {
        return if (lower) {
            DSL.field(
                "LOWER(JSON_EXTRACT({0}, {1}))",
                String::class.java, t1, t2
            )
        } else {
            DSL.field(
                "JSON_EXTRACT({0}, {1})",
                String::class.java, t1, t2
            )
        }
    }

    fun getBuildHistoryMaterial(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Collection<TPipelineBuildHistoryRecord> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .orderBy(BUILD_NUM.desc()).limit(10)
                .fetch()
        }
    }

    fun getBuildByBuildNo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildNo: Int
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_NUM.eq(buildNo))
                .fetchAny()
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

    fun updateBuildMaterial(dslContext: DSLContext, buildId: String, material: String?) {
        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(MATERIAL, material)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun updateBuildStageStatus(
        dslContext: DSLContext,
        buildId: String,
        stageStatus: List<BuildStageStatus>
    ): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                .set(STAGE_STATUS, JsonUtil.toJson(stageStatus))
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun updateBuildParameters(dslContext: DSLContext, buildId: String, buildParameters: String) {
        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.update(this)
                    .set(BUILD_PARAMETERS, buildParameters)
                    .where(BUILD_ID.eq(buildId))
                    .execute()
        }
    }

    fun getBuildParameters(dslContext: DSLContext, buildId: String): String? {
        with(T_PIPELINE_BUILD_HISTORY) {
            val record = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .fetchOne()
            return record?.buildParameters
        }
    }
}