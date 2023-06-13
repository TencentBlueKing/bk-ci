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

import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_SUMMARY
import com.tencent.devops.model.process.Tables.T_PIPELINE_INFO
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.PipelineFilterParam
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_FAVORITE_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_LIST_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.TableField
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
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
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId))).execute()
        }
    }

    fun get(dslContext: DSLContext, projectId: String, pipelineId: String): TPipelineBuildSummaryRecord? {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchAny()
        }
    }

    fun getSummaries(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: Set<String>
    ): Result<TPipelineBuildSummaryRecord> {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds).and(PROJECT_ID.eq(projectId)))
                .fetch()
        }
    }

    fun updateBuildNo(dslContext: DSLContext, projectId: String, pipelineId: String, buildNo: Int) {

        with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.update(this)
                .set(BUILD_NO, buildNo)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId))).execute()
        }
    }

    fun getBuildNo(dslContext: DSLContext, projectId: String, pipelineId: String): Int? {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.select(BUILD_NO)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchOne(0, Int::class.java)
        }
    }

    fun updateBuildNum(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildNum: Int = 0,
        buildNumAlias: String? = null
    ): Int {

        with(T_PIPELINE_BUILD_SUMMARY) {
            if (buildNum == 0) {
                dslContext.update(this)
                    .set(BUILD_NUM, BUILD_NUM + 1)
                    .set(BUILD_NUM_ALIAS, buildNumAlias)
                    .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId))).execute()
            } else {
                dslContext.update(this)
                    .set(BUILD_NUM, buildNum)
                    .set(BUILD_NUM_ALIAS, buildNumAlias)
                    .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId))).execute()
            }
        }
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.select(BUILD_NUM)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun listPipelineInfoBuildSummaryCount(
        dslContext: DSLContext,
        projectId: String,
        channelCode: ChannelCode,
        pipelineIds: Collection<String>? = null,
        viewId: String? = null,
        favorPipelines: List<String> = emptyList(),
        authPipelines: List<String> = emptyList(),
        pipelineFilterParamList: List<PipelineFilterParam>? = null,
        permissionFlag: Boolean? = null,
        includeDelete: Boolean? = false,
        userId: String
    ): Long {
        val conditions = generatePipelineFilterCondition(
            projectId = projectId,
            channelCode = channelCode,
            pipelineIds = pipelineIds,
            viewId = viewId,
            favorPipelines = favorPipelines,
            authPipelines = authPipelines,
            pipelineFilterParamList = pipelineFilterParamList,
            permissionFlag = permissionFlag,
            includeDelete = includeDelete,
            userId = userId
        )
        return dslContext.selectCount().from(T_PIPELINE_INFO)
            .where(conditions)
            .fetchOne(0, Long::class.java)!!
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
        pageOffsetNum: Int? = 0,
        includeDelete: Boolean? = false,
        collation: PipelineCollation = PipelineCollation.DEFAULT,
        userId: String?
    ): Result<TPipelineInfoRecord> {
        val conditions = generatePipelineFilterCondition(
            projectId = projectId,
            channelCode = channelCode,
            pipelineIds = pipelineIds,
            viewId = viewId,
            favorPipelines = favorPipelines,
            authPipelines = authPipelines,
            pipelineFilterParamList = pipelineFilterParamList,
            permissionFlag = permissionFlag,
            includeDelete = includeDelete,
            userId = userId
        )
        return listPipelineInfoBuildSummaryByConditions(
            dslContext = dslContext,
            conditions = conditions,
            sortType = sortType,
            favorPipelines = favorPipelines,
            authPipelines = authPipelines,
            offset = page?.let { (it - 1) * (pageSize ?: 10) + (pageOffsetNum ?: 0) },
            limit = if (pageSize == -1) null else pageSize,
            collation = collation
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
        permissionFlag: Boolean?,
        includeDelete: Boolean? = false,
        userId: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(T_PIPELINE_INFO.PROJECT_ID.eq(projectId))
        conditions.add(T_PIPELINE_INFO.CHANNEL.eq(channelCode.name))
        if (includeDelete == false) {
            conditions.add(T_PIPELINE_INFO.DELETE.eq(false))
        }
        if (!pipelineIds.isNullOrEmpty()) {
            conditions.add(T_PIPELINE_INFO.PIPELINE_ID.`in`(pipelineIds))
        }
        if (permissionFlag != null) {
            if (permissionFlag) {
                conditions.add(T_PIPELINE_INFO.PIPELINE_ID.`in`(authPipelines))
            } else {
                conditions.add(T_PIPELINE_INFO.PIPELINE_ID.notIn(authPipelines))
            }
        }
        if (!pipelineFilterParamList.isNullOrEmpty()) {
            handleFilterParamCondition(pipelineFilterParamList[0], conditions)
        }
        when (viewId) {
            PIPELINE_VIEW_FAVORITE_PIPELINES -> conditions.add(T_PIPELINE_INFO.PIPELINE_ID.`in`(favorPipelines))
            PIPELINE_VIEW_MY_PIPELINES -> if (userId != null) conditions.add(T_PIPELINE_INFO.CREATOR.eq(userId))
            PIPELINE_VIEW_MY_LIST_PIPELINES -> conditions.add(T_PIPELINE_INFO.PIPELINE_ID.`in`(authPipelines))
            PIPELINE_VIEW_ALL_PIPELINES -> {
                // 查询所有流水线
            }

            else -> if (pipelineFilterParamList != null && pipelineFilterParamList.size > 1) {
                logger.warn("this view logic has deprecated , viewId:$viewId")
                handleFilterParamCondition(pipelineFilterParamList[1], conditions)
            }
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
                var subCondition: Condition = DSL.falseCondition()
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
    ): Condition {
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
     * 查询条件作为变量进行查询
     */
    fun listPipelineInfoBuildSummaryByConditions(
        dslContext: DSLContext,
        conditions: MutableCollection<Condition>,
        sortType: PipelineSortType? = null,
        favorPipelines: List<String> = emptyList(),
        authPipelines: List<String> = emptyList(),
        offset: Int? = null,
        limit: Int? = null,
        collation: PipelineCollation
    ): Result<TPipelineInfoRecord> {
        val baseStep = dslContext.selectFrom(T_PIPELINE_INFO).where(conditions)
        if (sortType != null) {
            val sortTypeField = when (sortType) {
                PipelineSortType.NAME -> {
                    T_PIPELINE_INFO.PIPELINE_NAME.let {
                        if (collation == PipelineCollation.DEFAULT || collation == PipelineCollation.ASC) {
                            it.asc()
                        } else {
                            it.desc()
                        }
                    }
                }

                PipelineSortType.CREATE_TIME -> {
                    T_PIPELINE_INFO.CREATE_TIME.let {
                        if (collation == PipelineCollation.DEFAULT || collation == PipelineCollation.DESC) {
                            it.desc()
                        } else {
                            it.asc()
                        }
                    }
                }

                PipelineSortType.UPDATE_TIME -> {
                    T_PIPELINE_INFO.UPDATE_TIME.let {
                        if (collation == PipelineCollation.DEFAULT || collation == PipelineCollation.DESC) {
                            it.desc()
                        } else {
                            it.asc()
                        }
                    }
                }

                PipelineSortType.LAST_EXEC_TIME -> {
                    T_PIPELINE_INFO.LATEST_START_TIME.let {
                        if (collation == PipelineCollation.DEFAULT || collation == PipelineCollation.DESC) {
                            it.desc()
                        } else {
                            it.asc()
                        }
                    }
                }
            }
            baseStep.orderBy(T_PIPELINE_INFO.DELETE.asc(), sortTypeField, T_PIPELINE_INFO.PIPELINE_ID)
        }
        return if (null != offset && null != limit && offset >= 0 && limit > 0) {
            baseStep.limit(limit).offset(offset).fetch()
        } else {
            baseStep.fetch()
        }
    }

    /**
     * 1：新构建时都先进入排队，计数
     */
    fun updateQueueCount(dslContext: DSLContext, projectId: String, pipelineId: String, queueIncrement: Int = 1) {
        with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.update(this)
                .set(QUEUE_COUNT, QUEUE_COUNT + queueIncrement)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId))).execute()
        }
    }

    /**
     * 2：写最新一次运行中的构建信息
     */
    fun startLatestRunningBuild(
        dslContext: DSLContext,
        latestRunningBuild: LatestRunningBuild,
        executeCount: Int
    ): Int {
        return with(latestRunningBuild) {
            with(T_PIPELINE_BUILD_SUMMARY) {
                dslContext.update(this)
                    .let {
                        if (executeCount == 1) {
                            // 只有首次才写入LATEST_BUILD_ID
                            it.set(LATEST_BUILD_ID, buildId).set(LATEST_STATUS, status.ordinal)
                        } else {
                            // 重试时只有最新的构建才能刷新LATEST_STATUS
                            it.set(
                                LATEST_STATUS,
                                DSL.`when`(LATEST_BUILD_ID.eq(buildId), status.ordinal).otherwise(LATEST_STATUS)
                            )
                        }
                    }
                    .set(LATEST_TASK_COUNT, taskCount)
                    .set(LATEST_START_USER, userId)
                    .set(QUEUE_COUNT, QUEUE_COUNT - 1)
                    .set(RUNNING_COUNT, RUNNING_COUNT + 1)
                    .set(LATEST_START_TIME, LocalDateTime.now())
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId)) // 并发的情况下，不再考虑LATEST_BUILD_ID，没有意义，而且会造成Slow SQL
                    .execute()
            }
        }
    }

    /**
     * 更新运行中的任务信息摘要
     */
    fun updateCurrentBuildTask(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        currentTaskId: String? = null,
        currentTaskName: String? = null
    ) {
        with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.update(this)
                .set(LATEST_TASK_ID, currentTaskId) // 字段目前没有用，没有实质意义，不用考虑是否是当前的LATEST_BUILD_ID
                .set(LATEST_TASK_NAME, currentTaskName) // 界面一闪而过的提示，没有实质意义，不用考虑是否是当前的LATEST_BUILD_ID
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId)) // 并发的情况下，不用考虑是否是当前的LATEST_BUILD_ID，而且会造成Slow SQL
                .execute()
        }
    }

    /**
     * 3：结束运行记录
     */
    fun finishLatestRunningBuild(
        dslContext: DSLContext,
        latestRunningBuild: LatestRunningBuild,
        isStageFinish: Boolean
    ) {
        return with(latestRunningBuild) {
            with(T_PIPELINE_BUILD_SUMMARY) {
                val update =
                    dslContext.update(this)
                        .set(
                            LATEST_STATUS,
                            DSL.`when`(LATEST_BUILD_ID.eq(buildId), status.ordinal).otherwise(LATEST_STATUS)
                        ) // 不一定是FINISH，也有可能其它失败的status
                        .set(LATEST_END_TIME, endTime) // 结束时间
                        .set(LATEST_TASK_ID, "") // 结束时清空
                        .set(LATEST_TASK_NAME, "") // 结束时清空
                        .set(FINISH_COUNT, FINISH_COUNT + 1)

                if (!isStageFinish) update.set(RUNNING_COUNT, RUNNING_COUNT - 1)
                update.where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId)) //  并发的情况下，不用考虑是否是当前的LATEST_BUILD_ID，而且会造成Slow SQL
                    .execute()
            }
        }
    }

    /**
     * 4：正在队列中运行的数量刷新
     */
    fun updateRunningCount(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        runningIncrement: Int = 1
    ) {
        with(T_PIPELINE_BUILD_SUMMARY) {
            val update = dslContext.update(this).set(RUNNING_COUNT, RUNNING_COUNT + runningIncrement)

            if (runningIncrement > 0) {
                update.set(
                    LATEST_STATUS,
                    DSL.`when`(LATEST_BUILD_ID.eq(buildId), BuildStatus.RUNNING.ordinal).otherwise(LATEST_STATUS)
                )
            } else {
                update.set(
                    LATEST_STATUS,
                    DSL.`when`(LATEST_BUILD_ID.eq(buildId), BuildStatus.STAGE_SUCCESS.ordinal).otherwise(LATEST_STATUS)
                ).set(LATEST_END_TIME, LocalDateTime.now())
            }
            update.where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId)) //  并发的情况下，不用考虑是否是当前的LATEST_BUILD_ID，而且会造成Slow SQL
                .execute()
        }
    }

    fun listSummaryByPipelineIds(
        dslContext: DSLContext,
        pipelineIds: Collection<String>,
        projectId: String? = null
    ): Result<TPipelineBuildSummaryRecord> {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PIPELINE_ID.`in`(pipelineIds))
            if (!projectId.isNullOrBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    @Deprecated("改操作不安全，业务不要使用，仅限op使用")
    fun fixPipelineSummaryCount(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        finishCount: Int,
        runningCount: Int?,
        queueCount: Int?
    ): Boolean {
        with(T_PIPELINE_BUILD_SUMMARY) {
            val update = dslContext.update(this).set(FINISH_COUNT, FINISH_COUNT + finishCount)
            if (runningCount != null) {
                update.set(RUNNING_COUNT, RUNNING_COUNT + runningCount)
            }
            if (queueCount != null) {
                update.set(QUEUE_COUNT, QUEUE_COUNT + queueCount)
            }
            return update.where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute() == 1
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildSummaryDao::class.java)
    }
}
