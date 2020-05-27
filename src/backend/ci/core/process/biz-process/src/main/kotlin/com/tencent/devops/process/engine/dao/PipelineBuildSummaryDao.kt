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

import com.tencent.devops.common.db.util.JooqUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_SUMMARY
import com.tencent.devops.model.process.Tables.T_PIPELINE_INFO
import com.tencent.devops.model.process.Tables.T_PIPELINE_SETTING
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.PipelineFilterParam
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_FAVORITE_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SelectOnConditionStep
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBuildSummaryDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildNo: BuildNo?
    ) {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_NO
            ).values(projectId, pipelineId, buildNo?.buildNo ?: 0).execute()
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.delete(this)
                .where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    fun get(dslContext: DSLContext, pipelineId: String): TPipelineBuildSummaryRecord? {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchAny()
        }
    }

    fun getBuildNo(dslContext: DSLContext, pipelineId: String): Int {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.select(BUILD_NO).from(this)
                .where(PIPELINE_ID.eq(pipelineId)).fetchOne(BUILD_NO, Int::class.java)
        }
    }

    fun getSummaries(dslContext: DSLContext, pipelineIds: Set<String>): Result<TPipelineBuildSummaryRecord> {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .fetch()
        }
    }

    fun updateBuildNo(dslContext: DSLContext, pipelineId: String, buildNo: Int) {

        with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.update(this)
                .set(BUILD_NO, buildNo)
                .where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    fun updateBuildNum(dslContext: DSLContext, pipelineId: String, buildNum: Int = 0): Int {

        with(T_PIPELINE_BUILD_SUMMARY) {
            if (buildNum == 0)
                dslContext.update(this)
                    .set(BUILD_NUM, BUILD_NUM + 1)
                    .where(PIPELINE_ID.eq(pipelineId)).execute()
            else
                dslContext.update(this)
                    .set(BUILD_NUM, buildNum)
                    .where(PIPELINE_ID.eq(pipelineId)).execute()
        }
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.select(BUILD_NUM)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchOne(0, Int::class.java)
        }
    }

    fun listPipelineInfoBuildSummaryCount(
        dslContext: DSLContext,
        projectId: String,
        channelCode: ChannelCode,
        pipelineIds: Collection<String>? = null,
        viewId: String?,
        favorPipelines: List<String>,
        authPipelines: List<String>,
        pipelineFilterParamList: List<PipelineFilterParam>?,
        permissionFlag: Boolean? = null
    ): Long {
        val conditions = generatePipelineFilterCondition(
            projectId = projectId,
            channelCode = channelCode,
            pipelineIds = pipelineIds,
            viewId = viewId,
            favorPipelines = favorPipelines,
            authPipelines = authPipelines,
            pipelineFilterParamList = pipelineFilterParamList,
            permissionFlag = permissionFlag
        )
        val t = getPipelineInfoBuildSummaryBaseQuery(dslContext, favorPipelines, authPipelines).where(conditions).asTable("t")
        return dslContext.selectCount().from(t).fetchOne(0, Long::class.java)
    }

    fun listPipelineInfoBuildSummary(
        dslContext: DSLContext,
        projectId: String,
        channelCode: ChannelCode,
        sortType: PipelineSortType? = null,
        pipelineIds: Collection<String>? = null,
        favorPipelines: List<String> = emptyList(),
        authPipelines: List<String> = emptyList(),
        viewId: String? = null,
        pipelineFilterParamList: List<PipelineFilterParam>? = null,
        permissionFlag: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null,
        offsetNum: Int? = 0
    ): Result<out Record> {
        val conditions = generatePipelineFilterCondition(
            projectId = projectId,
            channelCode = channelCode,
            pipelineIds = pipelineIds,
            viewId = viewId,
            favorPipelines = favorPipelines,
            authPipelines = authPipelines,
            pipelineFilterParamList = pipelineFilterParamList,
            permissionFlag = permissionFlag
        )
        return listPipelineInfoBuildSummaryByConditions(
            dslContext = dslContext,
            conditions = conditions,
            sortType = sortType,
            favorPipelines = favorPipelines,
            authPipelines = authPipelines,
            page = page,
            pageSize = pageSize,
            offsetNum = offsetNum
        )
    }

    private fun generatePipelineFilterCondition(
        projectId: String,
        channelCode: ChannelCode,
        pipelineIds: Collection<String>?,
        viewId: String?,
        favorPipelines: List<String>,
        authPipelines: List<String>,
        pipelineFilterParamList: List<PipelineFilterParam>?,
        permissionFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(T_PIPELINE_INFO.PROJECT_ID.eq(projectId))
        conditions.add(T_PIPELINE_INFO.CHANNEL.eq(channelCode.name))
        conditions.add(T_PIPELINE_INFO.DELETE.eq(false))
        if (pipelineIds != null && pipelineIds.isNotEmpty()) {
            conditions.add(T_PIPELINE_INFO.PIPELINE_ID.`in`(pipelineIds))
        }
        if (permissionFlag != null) {
            if (permissionFlag) {
                conditions.add(T_PIPELINE_INFO.PIPELINE_ID.`in`(authPipelines))
            } else {
                conditions.add(T_PIPELINE_INFO.PIPELINE_ID.notIn(authPipelines))
            }
        }
        if (pipelineFilterParamList != null && pipelineFilterParamList.isNotEmpty()) {
            handleFilterParamCondition(pipelineFilterParamList[0], conditions)
        }
        when (viewId) {
            PIPELINE_VIEW_FAVORITE_PIPELINES -> conditions.add(T_PIPELINE_INFO.PIPELINE_ID.`in`(favorPipelines))
            PIPELINE_VIEW_MY_PIPELINES -> conditions.add(T_PIPELINE_INFO.PIPELINE_ID.`in`(authPipelines))
            PIPELINE_VIEW_ALL_PIPELINES -> {
                // 查询所有流水线
            }
            else -> if (pipelineFilterParamList != null && pipelineFilterParamList.size > 1) handleFilterParamCondition(pipelineFilterParamList[1], conditions)
        }
        return conditions
    }

    private fun handleFilterParamCondition(
        pipelineFilterParam: PipelineFilterParam,
        conditions: MutableList<Condition>
    ) {
        val logic = pipelineFilterParam.logic
        val conditionAndFlag: Boolean = (logic == Logic.AND)
        val filterByPipelineNames = pipelineFilterParam.filterByPipelineNames
        var filterConditions = generateInitCondition(conditionAndFlag)
        // 过滤流水线名称
        if (filterByPipelineNames.isNotEmpty()) {
            var filterByPipelineNameConditions = generateInitCondition(conditionAndFlag)
            filterByPipelineNames.forEach {
                val pipelineName = it.pipelineName
                val pipelineNameField = T_PIPELINE_INFO.PIPELINE_NAME
                val bkCondition = it.condition
                val subCondition = generateSubCondition(bkCondition, pipelineNameField, pipelineName)
                filterByPipelineNameConditions = if (conditionAndFlag) {
                    filterByPipelineNameConditions.and(subCondition)
                } else {
                    filterByPipelineNameConditions.or(subCondition)
                }
            }
            filterConditions = if (conditionAndFlag) {
                filterConditions.and(filterByPipelineNameConditions)
            } else {
                filterConditions.or(filterByPipelineNameConditions)
            }
        }
        // 过滤流水线创建人
        val filterByPipelineCreators = pipelineFilterParam.filterByPipelineCreators
        if (filterByPipelineCreators.isNotEmpty()) {
            var filterByPipelineCreatorConditions = generateInitCondition(conditionAndFlag)
            filterByPipelineCreators.forEach {
                val userIds = it.userIds
                val pipelineCreatorField = T_PIPELINE_INFO.CREATOR
                val subCondition = pipelineCreatorField.`in`(userIds)
                filterByPipelineCreatorConditions = if (conditionAndFlag) {
                    filterByPipelineCreatorConditions.and(subCondition)
                } else {
                    filterByPipelineCreatorConditions.or(subCondition)
                }
            }
            filterConditions = if (conditionAndFlag) {
                filterConditions.and(filterByPipelineCreatorConditions)
            } else {
                filterConditions.or(filterByPipelineCreatorConditions)
            }
        }
        // 过滤流水线标签
        val filterByLabelInfo = pipelineFilterParam.filterByLabelInfo
        val filterByLabels = filterByLabelInfo.filterByLabels
        val labelToPipelineMap = filterByLabelInfo.labelToPipelineMap
        if (filterByLabels.isNotEmpty()) {
            var filterByLabelConditions = generateInitCondition(conditionAndFlag)
            filterByLabels.forEach { filterByLabel ->
                val labelIds = filterByLabel.labelIds
                val pipelineIdField = T_PIPELINE_INFO.PIPELINE_ID
                var subCondition = DSL.falseCondition()
                labelIds.forEach {
                    val labelPipelineIds = labelToPipelineMap?.get(it)
                    if (labelPipelineIds != null) {
                        subCondition = subCondition.or(pipelineIdField.`in`(labelPipelineIds))
                    }
                }
                filterByLabelConditions = if (conditionAndFlag) {
                    filterByLabelConditions.and(subCondition)
                } else {
                    filterByLabelConditions.or(subCondition)
                }
            }
            filterConditions = if (conditionAndFlag) {
                filterConditions.and(filterByLabelConditions)
            } else {
                filterConditions.or(filterByLabelConditions)
            }
        }
        conditions.add(filterConditions)
    }

    private fun generateInitCondition(conditionAndFlag: Boolean) =
        if (conditionAndFlag) DSL.trueCondition() else DSL.falseCondition()

    private fun generateSubCondition(
        bkCondition: com.tencent.devops.process.pojo.classify.enums.Condition,
        field: TableField<TPipelineInfoRecord, String>,
        fieldValue: String
    ): Condition? {
        return when (bkCondition) {
            com.tencent.devops.process.pojo.classify.enums.Condition.LIKE -> field.contains(
                fieldValue
            )
            com.tencent.devops.process.pojo.classify.enums.Condition.NOT_LIKE -> field.notLike(
                "%$fieldValue%"
            )
            com.tencent.devops.process.pojo.classify.enums.Condition.EQUAL -> field.eq(
                fieldValue
            )
            com.tencent.devops.process.pojo.classify.enums.Condition.NOT_EQUAL -> field.ne(
                fieldValue
            )
            com.tencent.devops.process.pojo.classify.enums.Condition.INCLUDE -> JooqUtils.strPosition(
                field,
                fieldValue
            ).gt(0)
            com.tencent.devops.process.pojo.classify.enums.Condition.NOT_INCLUDE -> JooqUtils.strPosition(
                field,
                fieldValue
            ).le(0)
        }
    }

    /**
     * 分页查询多个projectId对应的PipelineInfoBuildSummary
     */
    fun listPipelineInfoBuildSummary(
        dslContext: DSLContext,
        projectIds: Set<String>?,
        channelCodes: Set<ChannelCode>?,
        limit: Int?,
        offset: Int?
    ): Result<out Record> {
        val conditions = mutableListOf<Condition>()
        conditions.add(T_PIPELINE_INFO.DELETE.eq(false))
        if (projectIds != null && projectIds.isNotEmpty()) {
            conditions.add(T_PIPELINE_INFO.PROJECT_ID.`in`(projectIds))
        }
        if (channelCodes != null && channelCodes.isNotEmpty()) {
            conditions.add(T_PIPELINE_INFO.CHANNEL.`in`(channelCodes.map { it.name }))
        }
        val where = getPipelineInfoBuildSummaryBaseQuery(dslContext).where(conditions)
        if (limit != null && limit >= 0) {
            where.limit(limit)
        }
        if (offset != null && offset >= 0) {
            where.offset(offset)
        }
        return where.fetch()
    }

    /**
     * 无Project信息，直接根据pipelineIds查询
     */
    fun listPipelineInfoBuildSummary(
        dslContext: DSLContext,
        channelCodes: Set<ChannelCode>?,
        pipelineIds: Collection<String>
    ): Result<out Record> {
        val conditions = mutableListOf<Condition>()
        conditions.add(T_PIPELINE_INFO.PIPELINE_ID.`in`(pipelineIds))
        conditions.add(T_PIPELINE_INFO.DELETE.eq(false))
        if (channelCodes != null && channelCodes.isNotEmpty()) {
            conditions.add(T_PIPELINE_INFO.CHANNEL.`in`(channelCodes.map { it.name }))
        }
        return listPipelineInfoBuildSummaryByConditions(
            dslContext = dslContext,
            conditions = conditions
        )
    }

    /**
     * 查询条件作为变量进行查询
     */
    fun listPipelineInfoBuildSummaryByConditions(
        dslContext: DSLContext,
        conditions: MutableCollection<Condition>,
        sortType: PipelineSortType? = null,
        favorPipelines: List<String> = emptyList(),
        authPipelines: List<String> = emptyList(),
        page: Int? = null,
        pageSize: Int? = null,
        offsetNum: Int? = 0
    ): Result<out Record> {
        val t = getPipelineInfoBuildSummaryBaseQuery(dslContext, favorPipelines, authPipelines).where(conditions).asTable("t")
        val baseStep = dslContext.select().from(t)
        if (sortType != null) {
            val sortTypeField = when (sortType) {
                PipelineSortType.NAME -> {
                    t.field("PIPELINE_NAME").asc()
                }
                PipelineSortType.CREATE_TIME -> {
                    t.field("CREATE_TIME").desc()
                }
                PipelineSortType.UPDATE_TIME -> {
                    t.field("UPDATE_TIME").desc()
                }
            }
            baseStep.orderBy(sortTypeField)
        }
        return if ((null != page && null != pageSize) && !(page == 1 && pageSize == -1)) {
            baseStep.limit((page - 1) * pageSize + (offsetNum ?: 0), pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    /**
    * 获取PipelineInfo与BuildSummary Join后的表
    */
    fun getPipelineInfoBuildSummaryBaseQuery(
        dslContext: DSLContext,
        favorPipelines: List<String> = emptyList(),
        authPipelines: List<String> = emptyList()
    ): SelectOnConditionStep<Record> {
        return dslContext.select(
            T_PIPELINE_INFO.PIPELINE_ID,
            T_PIPELINE_INFO.PROJECT_ID,
            T_PIPELINE_INFO.VERSION,
            T_PIPELINE_INFO.PIPELINE_NAME,
            T_PIPELINE_INFO.CREATE_TIME,
            T_PIPELINE_INFO.UPDATE_TIME,
            T_PIPELINE_INFO.CHANNEL,
            T_PIPELINE_INFO.CREATOR,
            T_PIPELINE_INFO.MANUAL_STARTUP,
            T_PIPELINE_INFO.ELEMENT_SKIP,
            T_PIPELINE_INFO.TASK_COUNT,
            T_PIPELINE_SETTING.DESC,
            T_PIPELINE_SETTING.RUN_LOCK_TYPE,
            T_PIPELINE_BUILD_SUMMARY.BUILD_NUM,
            T_PIPELINE_BUILD_SUMMARY.BUILD_NO,
            T_PIPELINE_BUILD_SUMMARY.FINISH_COUNT,
            T_PIPELINE_BUILD_SUMMARY.RUNNING_COUNT,
            T_PIPELINE_BUILD_SUMMARY.QUEUE_COUNT,
            T_PIPELINE_BUILD_SUMMARY.LATEST_BUILD_ID,
            T_PIPELINE_BUILD_SUMMARY.LATEST_TASK_COUNT,
            T_PIPELINE_BUILD_SUMMARY.LATEST_START_USER,
            T_PIPELINE_BUILD_SUMMARY.LATEST_START_TIME,
            T_PIPELINE_BUILD_SUMMARY.LATEST_END_TIME,
            T_PIPELINE_BUILD_SUMMARY.LATEST_TASK_NAME,
            T_PIPELINE_BUILD_SUMMARY.LATEST_STATUS
        )
            .from(T_PIPELINE_INFO)
            .innerJoin(
                T_PIPELINE_SETTING
                    .innerJoin(
                        T_PIPELINE_BUILD_SUMMARY
                    ).on(T_PIPELINE_SETTING.PIPELINE_ID.eq(T_PIPELINE_BUILD_SUMMARY.PIPELINE_ID))
            ).on(T_PIPELINE_INFO.PIPELINE_ID.eq(T_PIPELINE_SETTING.PIPELINE_ID))
    }

    /**
     * 获取PipelineInfo与BuildSummary Join后的表
     */

    /**
     * 1：新构建时都先进入排队，计数
     */
    fun updateQueueCount(dslContext: DSLContext, pipelineId: String, queueIncrement: Int = 1) {
        with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.update(this)
                .set(QUEUE_COUNT, QUEUE_COUNT + queueIncrement)
                .where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    /**
     * 2：写最新一次运行中的构建信息
     */
    fun startLatestRunningBuild(
        dslContext: DSLContext,
        latestRunningBuild: LatestRunningBuild
    ): Int {
        return with(latestRunningBuild) {
            with(T_PIPELINE_BUILD_SUMMARY) {
                dslContext.update(this)
                    .set(LATEST_BUILD_ID, buildId)
                    .set(LATEST_TASK_COUNT, taskCount)
                    .set(LATEST_START_USER, userId)
                    .set(QUEUE_COUNT, QUEUE_COUNT - 1)
                    .set(RUNNING_COUNT, RUNNING_COUNT + 1)
                    .set(LATEST_START_TIME, LocalDateTime.now())
                    .where(PIPELINE_ID.eq(pipelineId)).execute()
                dslContext.update(this)
                    .set(LATEST_STATUS, status.ordinal) // 一般必须是RUNNING
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(LATEST_BUILD_ID.eq(buildId)).execute()
            }
        }
    }

    /**
     * 更新运行中的任务信息摘要
     */
    fun updateCurrentBuildTask(dslContext: DSLContext, latestRunningBuild: LatestRunningBuild) {
        with(latestRunningBuild) {
            with(T_PIPELINE_BUILD_SUMMARY) {
                dslContext.update(this)
                    .set(LATEST_TASK_ID, currentTaskId)
                    .set(LATEST_TASK_NAME, currentTaskName)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(LATEST_BUILD_ID.eq(buildId)).execute()
            }
        }
    }

    /**
     * 3：结束运行记录
     */
    fun finishLatestRunningBuild(dslContext: DSLContext, latestRunningBuild: LatestRunningBuild, isStageFinish: Boolean) {
        val count = with(latestRunningBuild) {
            with(T_PIPELINE_BUILD_SUMMARY) {
                val update =
                    dslContext.update(this)
                    .set(LATEST_STATUS, status.ordinal) // 不一定是FINISH，也有可能其它失败的status
                    .set(LATEST_END_TIME, LocalDateTime.now()) // 结束时间
                    .set(LATEST_TASK_ID, "") // 结束时清空
                    .set(LATEST_TASK_NAME, "") // 结束时清空
                    .set(FINISH_COUNT, FINISH_COUNT + 1)

                if (!isStageFinish) update.set(RUNNING_COUNT, RUNNING_COUNT - 1)
                update.where(PIPELINE_ID.eq(pipelineId))
                    .and(LATEST_BUILD_ID.eq(buildId))
                    .execute()
            }
        }
        // 没更新到，可能是因为他不是当前最新一次构建，那么要做的一件事是对finishCount值做加1，同时runningCount值减1
        if (count == 0) {
            with(latestRunningBuild) {
                with(T_PIPELINE_BUILD_SUMMARY) {
                    val update =
                        dslContext.update(this)
                        .set(FINISH_COUNT, FINISH_COUNT + 1)
                    if (!isStageFinish) update.set(RUNNING_COUNT, RUNNING_COUNT - 1)
                    update.where(PIPELINE_ID.eq(pipelineId))
                        .execute()
                }
            }
        }
    }

    /**
     * 4：正在队列中运行的数量刷新
     */
    fun updateRunningCount(
        dslContext: DSLContext,
        pipelineId: String,
        buildId: String,
        runningIncrement: Int = 1
    ) {
        with(T_PIPELINE_BUILD_SUMMARY) {
            val count = dslContext.selectCount().from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(LATEST_BUILD_ID.eq(buildId))
                .fetchOne(0, Int::class.java)

            val update =
                dslContext.update(this).set(RUNNING_COUNT, RUNNING_COUNT + runningIncrement)

            if (count > 0) {
                // 如果本次构建是最新一次，则要把状态和完成时间也刷新
                update.set(LATEST_END_TIME, LocalDateTime.now())
                if (runningIncrement > 0) {
                    update.set(LATEST_STATUS, BuildStatus.RUNNING.ordinal)
                } else {
                    update.set(LATEST_STATUS, BuildStatus.STAGE_SUCCESS.ordinal)
                        .set(LATEST_END_TIME, LocalDateTime.now())
                }
            }
            update.where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

//    fun resetTransferRecord(dslContext: DSLContext, pipelineId: String, startTime: Date?, endTime: Date?, historyBuildCount: Int) {
//        with(T_PIPELINE_BUILD_SUMMARY) {
//            dslContext.update(this)
//                    .set(FINISH_COUNT, historyBuildCount)
//                    .set(LATEST_START_TIME, if (startTime == null) null else LocalDateTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault()))
//                    .set(LATEST_END_TIME, if (endTime == null) null else LocalDateTime.ofInstant(endTime.toInstant(), ZoneId.systemDefault()))
//                    .set(QUEUE_COUNT, 0)
//                    .where(PIPELINE_ID.eq(pipelineId)).execute()
//        }
//    }
}
