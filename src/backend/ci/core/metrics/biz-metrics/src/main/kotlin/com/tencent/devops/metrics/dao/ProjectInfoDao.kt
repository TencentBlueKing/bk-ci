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

package com.tencent.devops.metrics.dao

import com.tencent.devops.common.event.pojo.measure.PipelineLabelRelateInfo
import com.tencent.devops.model.metrics.tables.TAtomOverviewData
import com.tencent.devops.model.metrics.tables.TErrorTypeDict
import com.tencent.devops.model.metrics.tables.TProjectPipelineLabelInfo
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineLabelInfo
import com.tencent.devops.metrics.pojo.qo.QueryProjectInfoQO
import com.tencent.devops.model.metrics.tables.records.TProjectPipelineLabelInfoRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProjectInfoDao {

    fun queryProjectAtomList(
        dslContext: DSLContext,
        projectId: String,
        page: Int,
        pageSize: Int,
        keyWord: String? = null
    ): List<AtomBaseInfoDO> {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (!keyWord.isNullOrBlank()) {
                conditions.add(ATOM_NAME.like("%$keyWord%"))
            }
            return dslContext.select(ATOM_CODE, ATOM_NAME)
                .from(this)
                .where(conditions)
                .groupBy(ATOM_CODE)
                .orderBy(TOTAL_EXECUTE_COUNT.desc())
                .limit((page - 1) * pageSize, pageSize)
                .fetchInto(AtomBaseInfoDO::class.java)
        }
    }

    fun queryProjectAtomCount(
        dslContext: DSLContext,
        projectId: String,
        keyWord: String? = null
    ): Long {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (!keyWord.isNullOrBlank()) {
                conditions.add(ATOM_NAME.like("%$keyWord%"))
            }
            return dslContext.select(ATOM_CODE)
                .from(this)
                .where(conditions)
                .groupBy(ATOM_CODE)
                .execute().toLong()
        }
    }

    fun queryProjectPipelineLabels(
        dslContext: DSLContext,
        queryCondition: QueryProjectInfoQO
    ): List<PipelineLabelInfo> {
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(queryCondition.projectId))
            if (!queryCondition.pipelineIds.isNullOrEmpty()) {
                conditions.add(PIPELINE_ID.`in`(queryCondition.pipelineIds))
            }
            if (!queryCondition.keyword.isNullOrBlank()) {
                conditions.add(LABEL_NAME.like("%${queryCondition.keyword}%"))
            }
            return dslContext.select(LABEL_ID, LABEL_NAME).from(this)
                .where(conditions)
                .groupBy(LABEL_ID)
                .orderBy(LABEL_ID)
                .limit((queryCondition.page - 1) * queryCondition.pageSize, queryCondition.pageSize)
                .fetchInto(PipelineLabelInfo::class.java)
        }
    }

    fun queryProjectPipelineLabelsCount(
        dslContext: DSLContext,
        queryCondition: QueryProjectInfoQO
    ): Long {
        val conditions = mutableListOf<Condition>()
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {

            if (!queryCondition.pipelineIds.isNullOrEmpty()) {
                conditions.add(PIPELINE_ID.`in`(queryCondition.pipelineIds))
            }
            conditions.add(PROJECT_ID.eq(queryCondition.projectId))
            if (!queryCondition.keyword.isNullOrBlank()) {
                conditions.add(LABEL_NAME.like("%${queryCondition.keyword}%"))
            }
            return dslContext.selectDistinct(LABEL_ID).from(this)
                .where(conditions)
                .execute().toLong()
        }
    }

    fun queryPipelineErrorTypes(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int,
        keyWord: String?
    ): List<Int> {
        with(TErrorTypeDict.T_ERROR_TYPE_DICT) {
            val conditions = mutableListOf<Condition>()
            if (!keyWord.isNullOrBlank()) {
                conditions.add(this.NAME.like("%$keyWord%"))
            }
            val step = dslContext.select(
                ERROR_TYPE
            ).from(this)
                .where(conditions)
                .groupBy(ERROR_TYPE)
                .orderBy(ERROR_TYPE)
                .limit((page - 1) * pageSize, pageSize)
            return step.fetchInto(Int::class.java)
        }
    }

    fun queryPipelineErrorTypeCount(dslContext: DSLContext, keyWord: String?): Long {
        with(TErrorTypeDict.T_ERROR_TYPE_DICT) {
            val conditions = mutableListOf<Condition>()
            if (!keyWord.isNullOrBlank()) {
                conditions.add(this.NAME.like("%$keyWord%"))
            }
            return dslContext.selectDistinct(ERROR_TYPE)
                .from(this)
                .where(conditions)
                .execute().toLong()
        }
    }

    fun batchCreatePipelineLabelData(
        dslContext: DSLContext,
        pipelineLabelRelateInfos: List<TProjectPipelineLabelInfoRecord>
    ) {
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {
            pipelineLabelRelateInfos.forEach {
                dslContext.insertInto(this)
                    .set(ID, it.id)
                    .set(PROJECT_ID, it.projectId)
                    .set(PIPELINE_ID, it.pipelineId)
                    .set(LABEL_ID, it.labelId)
                    .set(LABEL_NAME, it.labelName)
                    .set(CREATOR, it.creator)
                    .set(MODIFIER, it.modifier)
                    .set(UPDATE_TIME, it.updateTime)
                    .set(CREATE_TIME, it.createTime)
                    .onDuplicateKeyUpdate()
                    .set(LABEL_NAME, it.labelName)
                    .set(MODIFIER, it.modifier)
                    .set(UPDATE_TIME, it.updateTime)
                    .execute()
            }
        }
    }

    fun batchDeletePipelineLabelData(
        dslContext: DSLContext,
        pipelineLabelRelateInfos: List<PipelineLabelRelateInfo>
    ) {
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {

            return dslContext.batched {
                pipelineLabelRelateInfos.forEach { pipelineLabelRelateInfo ->
                    val conditions = mutableListOf<Condition>()
                    conditions.add(this.PROJECT_ID.eq(pipelineLabelRelateInfo.projectId))
                    if (!pipelineLabelRelateInfo.pipelineId.isNullOrBlank()) {
                        conditions.add(this.PIPELINE_ID.eq(pipelineLabelRelateInfo.pipelineId))
                    }
                    if (pipelineLabelRelateInfo.labelId != null) {
                        conditions.add(LABEL_ID.eq(pipelineLabelRelateInfo.labelId))
                    }
                    it.dsl().deleteFrom(this)
                        .where(conditions)
                        .execute()
                }
            }
        }
    }

    fun batchUpdatePipelineLabelData(
        dslContext: DSLContext,
        userId: String,
        statisticsTime: LocalDateTime,
        pipelineLabelRelateInfos: List<PipelineLabelRelateInfo>
    ) {
        val conditions = mutableListOf<Condition>()
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {
            return dslContext.batched {
                pipelineLabelRelateInfos.forEach { pipelineLabelRelateInfo ->
                    if (!pipelineLabelRelateInfo.pipelineId.isNullOrBlank()) {
                        conditions.add(this.PIPELINE_ID.eq(pipelineLabelRelateInfo.pipelineId))
                    }
                    conditions.add(this.PROJECT_ID.eq(pipelineLabelRelateInfo.projectId))
                    if (pipelineLabelRelateInfo.labelId != null) {
                        conditions.add(LABEL_ID.eq(pipelineLabelRelateInfo.labelId))
                    }
                    it.dsl().update(this)
                        .set(this.LABEL_NAME, pipelineLabelRelateInfo.name)
                        .set(MODIFIER, userId)
                        .set(UPDATE_TIME, statisticsTime)
                        .where(conditions)
                        .execute()
                }
            }
        }
    }
}
