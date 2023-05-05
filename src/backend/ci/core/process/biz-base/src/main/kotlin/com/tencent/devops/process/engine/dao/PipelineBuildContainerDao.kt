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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_CONTAINER
import com.tencent.devops.model.process.tables.records.TPipelineBuildContainerRecord
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.RecordMapper
import org.jooq.util.mysql.MySQLDSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineBuildContainerDao {

    fun create(dslContext: DSLContext, buildContainer: PipelineBuildContainer) {

        val count =
            with(T_PIPELINE_BUILD_CONTAINER) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    STAGE_ID,
                    MATRIX_GROUP_FLAG,
                    MATRIX_GROUP_ID,
                    CONTAINER_TYPE,
                    SEQ,
                    CONTAINER_ID,
                    CONTAINER_HASH_ID,
                    STATUS,
                    START_TIME,
                    END_TIME,
                    COST,
                    EXECUTE_COUNT,
                    CONDITIONS
                )
                    .values(
                        buildContainer.projectId,
                        buildContainer.pipelineId,
                        buildContainer.buildId,
                        buildContainer.stageId,
                        buildContainer.matrixGroupFlag,
                        buildContainer.matrixGroupId,
                        buildContainer.containerType,
                        buildContainer.seq,
                        buildContainer.containerId,
                        buildContainer.containerHashId,
                        buildContainer.status.ordinal,
                        buildContainer.startTime,
                        buildContainer.endTime,
                        buildContainer.cost,
                        buildContainer.executeCount,
                        buildContainer.controlOption.let { self -> JsonUtil.toJson(self, formatted = false) }
                    )
                    .execute()
            }
        logger.info("save the buildContainer=$buildContainer, result=${count == 1}")
    }

    fun batchSave(dslContext: DSLContext, containerList: Collection<PipelineBuildContainer>) {
        with(T_PIPELINE_BUILD_CONTAINER) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                STAGE_ID,
                CONTAINER_ID,
                CONTAINER_HASH_ID,
                MATRIX_GROUP_FLAG,
                MATRIX_GROUP_ID,
                CONTAINER_TYPE,
                SEQ,
                STATUS,
                START_TIME,
                END_TIME,
                COST,
                EXECUTE_COUNT,
                CONDITIONS
            ).also { insert ->
                containerList.forEach {
                    insert.values(
                        it.projectId,
                        it.pipelineId,
                        it.buildId,
                        it.stageId,
                        it.containerId,
                        it.containerHashId,
                        it.matrixGroupFlag,
                        it.matrixGroupId,
                        it.containerType,
                        it.seq,
                        it.status.ordinal,
                        it.startTime,
                        it.endTime,
                        it.cost,
                        it.executeCount,
                        it.controlOption.let { self -> JsonUtil.toJson(self, formatted = false) }
                    )
                }
            }.onDuplicateKeyUpdate()
                .set(STATUS, MySQLDSL.values(STATUS))
                .set(START_TIME, MySQLDSL.values(START_TIME))
                .set(END_TIME, MySQLDSL.values(END_TIME))
                .set(COST, MySQLDSL.values(COST))
                .set(EXECUTE_COUNT, MySQLDSL.values(EXECUTE_COUNT))
                .execute()
        }
    }

    fun batchUpdate(dslContext: DSLContext, containerList: List<PipelineBuildContainer>) {
        with(T_PIPELINE_BUILD_CONTAINER) {
            containerList.forEach {
                dslContext.update(this)
                    .set(PIPELINE_ID, it.pipelineId)
                    .set(MATRIX_GROUP_ID, it.matrixGroupId)
                    .set(CONTAINER_TYPE, it.containerType)
                    .set(CONTAINER_ID, it.containerId)
                    .set(CONTAINER_HASH_ID, it.containerHashId)
                    .set(STATUS, it.status.ordinal)
                    .set(START_TIME, it.startTime)
                    .set(END_TIME, it.endTime)
                    .set(COST, it.cost)
                    .set(EXECUTE_COUNT, it.executeCount)
                    .set(CONDITIONS, it.controlOption.let { self -> JsonUtil.toJson(self, formatted = false) })
                    .where(BUILD_ID.eq(it.buildId).and(STAGE_ID.eq(it.stageId)).and(SEQ.eq(it.seq)))
                    .execute()
            }
        }
    }

    fun getByContainerId(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        stageId: String?,
        containerId: String
    ): PipelineBuildContainer? {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            val query = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
            if (!stageId.isNullOrBlank()) {
                query.and(STAGE_ID.eq(stageId))
            }
            query.and(CONTAINER_ID.eq(containerId)).fetchAny(mapper)
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        controlOption: PipelineBuildContainerControlOption?,
        buildStatus: BuildStatus
    ): Int {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            val update = dslContext.update(this).set(STATUS, buildStatus.ordinal)

            controlOption?.let { update.set(CONDITIONS, JsonUtil.toJson(controlOption, formatted = false)) }

            startTime?.let { update.set(START_TIME, startTime) }

            endTime?.let {
                update.set(END_TIME, endTime)

                if (buildStatus.isFinish()) {
                    update.set(
                        COST,
                        COST + JooqUtils.timestampDiff(
                            DatePart.SECOND,
                            START_TIME.cast(java.sql.Timestamp::class.java),
                            END_TIME.cast(java.sql.Timestamp::class.java)
                        )
                    )
                }
            }

            update.where(BUILD_ID.eq(buildId)).and(PROJECT_ID.eq(projectId)).and(STAGE_ID.eq(stageId))
                .and(CONTAINER_ID.eq(containerId)).execute()
        }
    }

    fun updateControlOption(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        controlOption: PipelineBuildContainerControlOption
    ): Int {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            dslContext.update(this)
                .set(CONDITIONS, JsonUtil.toJson(controlOption, formatted = false))
                .where(BUILD_ID.eq(buildId)).and(PROJECT_ID.eq(projectId)).and(STAGE_ID.eq(stageId))
                .and(CONTAINER_ID.eq(containerId)).execute()
        }
    }

    fun listByBuildId(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        stageId: String? = null,
        containsMatrix: Boolean? = true,
        statusSet: Set<BuildStatus>? = null
    ): List<PipelineBuildContainer> {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            val conditionStep = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId)).and(BUILD_ID.eq(buildId))
            if (!stageId.isNullOrBlank()) {
                conditionStep.and(STAGE_ID.eq(stageId))
            }
            if (!statusSet.isNullOrEmpty()) {
                val statusIntSet = mutableSetOf<Int>()
                statusSet.forEach {
                    statusIntSet.add(it.ordinal)
                }
                conditionStep.and(STATUS.`in`(statusIntSet))
            }
            if (containsMatrix == false) {
                conditionStep.and(MATRIX_GROUP_ID.isNull)
            }
            conditionStep.orderBy(SEQ.asc()).fetch(mapper)
        }
    }

    fun countStageContainers(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        stageId: String,
        onlyMatrixGroup: Boolean
    ): Int {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            val count = dslContext.selectCount().from(this).where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(STAGE_ID.eq(stageId))
            if (onlyMatrixGroup) count.and(MATRIX_GROUP_FLAG.eq(true))
            count.fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun listByMatrixGroupId(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        matrixGroupId: String
    ): List<PipelineBuildContainer> {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            dslContext.selectFrom(this).where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(MATRIX_GROUP_ID.eq(matrixGroupId))
                .orderBy(SEQ.asc()).fetch(mapper)
        }
    }

    fun listBuildContainerIdsInMatrixGroup(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        matrixGroupId: String,
        stageId: String? = null
    ): Collection<String> {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            val conditionStep = dslContext.select(CONTAINER_ID).from(this)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .and(MATRIX_GROUP_ID.eq(matrixGroupId))
            if (!stageId.isNullOrBlank()) {
                conditionStep.and(STAGE_ID.eq(stageId))
            }
            conditionStep.orderBy(SEQ.asc()).fetch(CONTAINER_ID)
        }
    }

    fun deleteBuildContainerInMatrixGroup(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        matrixGroupId: String
    ): Int {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .and(MATRIX_GROUP_ID.eq(matrixGroupId))
                .execute()
        }
    }

    class PipelineBuildContainerJooqMapper : RecordMapper<TPipelineBuildContainerRecord, PipelineBuildContainer> {
        override fun map(record: TPipelineBuildContainerRecord?): PipelineBuildContainer? {
            return record?.run {
                val controlOption = if (!conditions.isNullOrBlank()) {
                    JsonUtil.to(conditions, PipelineBuildContainerControlOption::class.java)
                } else {
                    PipelineBuildContainerControlOption(jobControlOption = JobControlOption())
                }
                PipelineBuildContainer(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stageId,
                    containerType = containerType,
                    containerId = containerId,
                    containerHashId = containerHashId,
                    matrixGroupFlag = matrixGroupFlag,
                    matrixGroupId = matrixGroupId,
                    seq = seq,
                    status = BuildStatus.values()[status],
                    startTime = startTime,
                    endTime = endTime,
                    cost = cost ?: 0,
                    executeCount = executeCount ?: 1,
                    controlOption = controlOption
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineBuildContainerJooqMapper()
        private val logger = LoggerFactory.getLogger(PipelineBuildContainerDao::class.java)
    }
}
