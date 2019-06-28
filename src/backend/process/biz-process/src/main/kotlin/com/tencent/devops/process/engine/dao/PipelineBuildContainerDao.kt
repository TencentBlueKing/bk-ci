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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.JobControlOption
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_CONTAINER
import com.tencent.devops.model.process.tables.records.TPipelineBuildContainerRecord
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBuildContainerDao {

    fun create(
        dslContext: DSLContext,
        buildContainer: PipelineBuildContainer
    ) {

        val count =
            with(T_PIPELINE_BUILD_CONTAINER) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    STAGE_ID,
                    CONTAINER_TYPE,
                    SEQ,
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
                        buildContainer.containerType,
                        buildContainer.seq,
                        buildContainer.status.ordinal,
                        buildContainer.startTime,
                        buildContainer.endTime,
                        buildContainer.cost,
                        buildContainer.executeCount,
                        if (buildContainer.controlOption != null)
                            JsonUtil.toJson(buildContainer.controlOption!!)
                        else null
                    )
                    .execute()
            }
        logger.info("save the buildContainer=$buildContainer, result=${count == 1}")
    }

    fun batchSave(dslContext: DSLContext, taskList: Collection<PipelineBuildContainer>) {
        val records = mutableListOf<TPipelineBuildContainerRecord>()
        taskList.forEach {
            with(it) {
                records.add(
                    TPipelineBuildContainerRecord(
                        projectId, pipelineId, buildId, stageId, containerId, containerType,
                        seq, status.ordinal, startTime, endTime, cost, executeCount,
                        if (controlOption != null) JsonUtil.toJson(controlOption!!) else null
                    )
                )
            }
        }
        dslContext.batchStore(records).execute()
    }

    fun get(
        dslContext: DSLContext,
        buildId: String,
        stageId: String?,
        containerId: String
    ): TPipelineBuildContainerRecord? {

        return with(T_PIPELINE_BUILD_CONTAINER) {
            val query = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId))
            if (stageId.isNullOrBlank()) {
                query.and(CONTAINER_ID.eq(containerId)).fetchAny()
            } else {
                query.and(STAGE_ID.eq(stageId)).and(CONTAINER_ID.eq(containerId)).fetchAny()
            }
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        buildId: String,
        stageId: String,
        containerId: String,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        buildStatus: BuildStatus
    ): Int {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            val update = dslContext.update(this)
                .set(STATUS, buildStatus.ordinal)

            if (startTime != null) {
                update.set(START_TIME, startTime)
            }
            if (endTime != null) {
                update.set(END_TIME, endTime)
                if (BuildStatus.isFinish(buildStatus)) {
                    update.set(COST, COST + END_TIME - START_TIME)
                }
            }

            update.where(BUILD_ID.eq(buildId)).and(STAGE_ID.eq(stageId))
                .and(CONTAINER_ID.eq(containerId)).execute()
        }
    }

    fun listByBuildId(
        dslContext: DSLContext,
        buildId: String,
        stageId: String? = null
    ): Collection<TPipelineBuildContainerRecord> {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            val conditionStep = dslContext.selectFrom(this).where(BUILD_ID.eq(buildId))
            if (!stageId.isNullOrBlank()) {
                conditionStep.and(STAGE_ID.eq(stageId))
            }
            conditionStep.orderBy(SEQ.asc()).fetch()
        }
    }

    fun deletePipelineBuildContainers(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        return with(T_PIPELINE_BUILD_CONTAINER) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun convert(tTPipelineBuildContainerRecord: TPipelineBuildContainerRecord): PipelineBuildContainer? {
        return with(tTPipelineBuildContainerRecord) {
            val controlOption = if (!conditions.isNullOrBlank()) {
                try {
                    JsonUtil.to(conditions, PipelineBuildContainerControlOption::class.java)
                } catch (ignored: Throwable) { // TODO 旧数据兼容 ，后续删除掉
                    val conditions = JsonUtil.to(conditions, object : TypeReference<List<NameAndValue>>() {})
                    PipelineBuildContainerControlOption(
                        jobControlOption = JobControlOption(
                            enable = true,
                            timeout = 0,
                            customVariables = conditions,
                            runCondition = JobRunCondition.STAGE_RUNNING
                        ),
                        mutexGroup = null
                    )
                }
            } else {
                PipelineBuildContainerControlOption(
                    jobControlOption = JobControlOption(
                        enable = true,
                        timeout = 0,
                        customVariables = emptyList(),
                        runCondition = JobRunCondition.STAGE_RUNNING
                    ),
                    mutexGroup = null
                )
            }
            PipelineBuildContainer(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                containerType = containerType,
                containerId = containerId,
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

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildContainerDao::class.java)
    }
}
