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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.db.utils.JooqUtils.count
import com.tencent.devops.metrics.constant.Constants.BK_ATOM_CODE
import com.tencent.devops.metrics.constant.Constants.BK_ATOM_NAME
import com.tencent.devops.metrics.constant.Constants.BK_ATOM_POSITION
import com.tencent.devops.metrics.constant.Constants.BK_BUILD_ID
import com.tencent.devops.metrics.constant.Constants.BK_BUILD_NUM
import com.tencent.devops.metrics.constant.Constants.BK_CHANNEL_CODE
import com.tencent.devops.metrics.constant.Constants.BK_CLASSIFY_CODE
import com.tencent.devops.metrics.constant.Constants.BK_END_TIME
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_CODE
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_COUNT
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_MSG
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_TYPE
import com.tencent.devops.metrics.constant.Constants.BK_PIPELINE_ID
import com.tencent.devops.metrics.constant.Constants.BK_PIPELINE_NAME
import com.tencent.devops.metrics.constant.Constants.BK_PROJECT_ID
import com.tencent.devops.metrics.constant.Constants.BK_START_TIME
import com.tencent.devops.metrics.constant.Constants.BK_START_USER
import com.tencent.devops.metrics.pojo.`do`.AtomFailDetailInfoDO
import com.tencent.devops.metrics.pojo.qo.QueryAtomFailInfoQO
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.model.metrics.tables.TAtomFailDetailData
import com.tencent.devops.model.metrics.tables.TAtomFailSummaryData
import com.tencent.devops.model.metrics.tables.TProjectPipelineLabelInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record4
import org.jooq.Result
import org.jooq.SelectJoinStep
import org.springframework.stereotype.Repository

@Repository
class AtomFailInfoDao {

    fun queryAtomErrorCodeStatisticsInfo(
        dslContext: DSLContext,
        queryCondition: QueryAtomFailInfoQO
    ): Result<Record4<Int, Int, String, Int>> {
        with(TAtomFailDetailData.T_ATOM_FAIL_DETAIL_DATA) {
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            val pipelineLabelIds = queryCondition.baseQueryReq.pipelineLabelIds
            val conditions = getConditions(
                queryCondition,
                tProjectPipelineLabelInfo
            )
            val errorCount = count<Int>(ERROR_CODE).`as`(BK_ERROR_COUNT)
            val step = dslContext.select(
                this.ERROR_TYPE.`as`(BK_ERROR_TYPE),
                this.ERROR_CODE.`as`(BK_ERROR_CODE),
                this.ERROR_MSG.`as`(BK_ERROR_MSG),
                errorCount
            ).from(this)
            if (!pipelineLabelIds.isNullOrEmpty()) {
                step.leftJoin(tProjectPipelineLabelInfo)
                    .on(this.PIPELINE_ID.eq(tProjectPipelineLabelInfo.PIPELINE_ID))
            }
            return step.where(conditions)
                .groupBy(this.ERROR_CODE)
                .orderBy(errorCount.desc())
                .offset((queryCondition.page - 1) * queryCondition.pageSize)
                .limit(queryCondition.pageSize)
                .fetch()
        }
    }

    fun queryAtomErrorCodeOverviewCount(
        dslContext: DSLContext,
        queryCondition: QueryAtomFailInfoQO
    ): Int {
        with(TAtomFailDetailData.T_ATOM_FAIL_DETAIL_DATA) {
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            val conditions = getConditions(
                queryCondition,
                tProjectPipelineLabelInfo
            )
            val step = dslContext.select(this.ERROR_CODE).from(this)
            val conditionStep = getConditionStep(queryCondition.baseQueryReq, step, tProjectPipelineLabelInfo)
            return conditionStep
                .where(conditions)
                .groupBy(this.ERROR_CODE)
                .execute()
        }
    }

    private fun TAtomFailDetailData.getConditions(
        queryAtomFailInfo: QueryAtomFailInfoQO,
        tProjectPipelineLabelInfo: TProjectPipelineLabelInfo
    ): MutableList<Condition> {
        val baseQueryReq = queryAtomFailInfo.baseQueryReq
        val conditions = mutableListOf<Condition>()
        conditions.add(this.PROJECT_ID.eq(queryAtomFailInfo.projectId))
        if (!baseQueryReq.pipelineIds.isNullOrEmpty()) {
            conditions.add(this.PIPELINE_ID.`in`(baseQueryReq.pipelineIds))
        }
        if (!baseQueryReq.pipelineLabelIds.isNullOrEmpty()) {
            conditions.add(tProjectPipelineLabelInfo.LABEL_ID.`in`(baseQueryReq.pipelineLabelIds))
        }
        if (!queryAtomFailInfo.atomCodes.isNullOrEmpty()) {
            conditions.add(ATOM_CODE.`in`(queryAtomFailInfo.atomCodes))
        }
        if (!queryAtomFailInfo.errorTypes.isNullOrEmpty()) {
            conditions.add(this.ERROR_TYPE.`in`(queryAtomFailInfo.errorTypes))
        }
        if (!queryAtomFailInfo.errorCodes.isNullOrEmpty()) {
            conditions.add(this.ERROR_CODE.`in`(queryAtomFailInfo.errorCodes))
        }
        val endDateTime = DateTimeUtil.stringToLocalDate(baseQueryReq.endTime)?.atStartOfDay()
        val startDateTime = DateTimeUtil.stringToLocalDate(baseQueryReq.startTime)?.atStartOfDay()
        if (!startDateTime!!.isEqual(endDateTime)) {
            conditions.add(this.STATISTICS_TIME.between(startDateTime, endDateTime))
        } else {
            conditions.add(this.STATISTICS_TIME.eq(startDateTime))
        }
        return conditions
    }

    private fun <T> TAtomFailDetailData.getConditionStep(
        baseQueryReq: BaseQueryReqVO,
        step: SelectJoinStep<Record1<T>>,
        tProjectPipelineLabelInfo: TProjectPipelineLabelInfo
    ): SelectJoinStep<Record1<T>> {
        return if (!baseQueryReq.pipelineLabelIds.isNullOrEmpty()) {
            step.leftJoin(tProjectPipelineLabelInfo)
                .on(this.PIPELINE_ID.eq(tProjectPipelineLabelInfo.PIPELINE_ID))
        } else {
            step
        }
    }

    fun queryAtomFailDetailInfo(
        dslContext: DSLContext,
        queryCondition: QueryAtomFailInfoQO
    ): List<AtomFailDetailInfoDO> {
        with(TAtomFailDetailData.T_ATOM_FAIL_DETAIL_DATA) {
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            val conditions = getConditions(
                queryCondition,
                tProjectPipelineLabelInfo
            )
            val step = dslContext.select(
                this.PROJECT_ID.`as`(BK_PROJECT_ID),
                this.PIPELINE_ID.`as`(BK_PIPELINE_ID),
                this.PIPELINE_NAME.`as`(BK_PIPELINE_NAME),
                this.CHANNEL_CODE.`as`(BK_CHANNEL_CODE),
                this.BUILD_ID.`as`(BK_BUILD_ID),
                this.BUILD_NUM.`as`(BK_BUILD_NUM),
                this.ATOM_CODE.`as`(BK_ATOM_CODE),
                this.ATOM_NAME.`as`(BK_ATOM_NAME),
                this.ATOM_POSITION.`as`(BK_ATOM_POSITION),
                this.CLASSIFY_CODE.`as`(BK_CLASSIFY_CODE),
                this.START_USER.`as`(BK_START_USER),
                this.START_TIME.`as`(BK_START_TIME),
                this.END_TIME.`as`(BK_END_TIME),
                this.ERROR_TYPE.`as`(BK_ERROR_TYPE),
                this.ERROR_CODE.`as`(BK_ERROR_CODE),
                this.ERROR_MSG.`as`(BK_ERROR_MSG)
            ).from(this)
            if (!queryCondition.baseQueryReq.pipelineLabelIds.isNullOrEmpty()) {
                step.leftJoin(tProjectPipelineLabelInfo)
                    .on(this.PIPELINE_ID.eq(tProjectPipelineLabelInfo.PIPELINE_ID))
            }
            return step.where(conditions)
                .groupBy(this.PIPELINE_ID, this.BUILD_NUM)
                .orderBy(START_TIME.desc())
                .offset((queryCondition.page - 1) * queryCondition.pageSize)
                .limit(queryCondition.pageSize)
                .fetchInto(AtomFailDetailInfoDO::class.java)
        }
    }

    fun queryAtomFailDetailCount(
        dslContext: DSLContext,
        queryCondition: QueryAtomFailInfoQO
    ): Long {
        with(TAtomFailDetailData.T_ATOM_FAIL_DETAIL_DATA) {
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            val conditions = getConditions(
                queryCondition,
                tProjectPipelineLabelInfo
            )
            val step = dslContext.select(ID).from(this)
            val conditionStep = getConditionStep(queryCondition.baseQueryReq, step, tProjectPipelineLabelInfo)
            return conditionStep
                .where(conditions)
                .groupBy(this.PIPELINE_ID, this.BUILD_NUM)
                .execute().toLong()
        }
    }

    fun limitAtomCodes(dslContext: DSLContext, projectIds: List<String>): List<String> {
        with(TAtomFailSummaryData.T_ATOM_FAIL_SUMMARY_DATA) {
            return dslContext.select(ATOM_CODE)
                .from(this)
                .where(PROJECT_ID.`in`(projectIds))
                .fetchInto(String::class.java).distinct()
        }
    }

    fun getAtomErrorInfos(dslContext: DSLContext, projectId: String): Result<Record4<String, Int, Int, String>> {
        with(TAtomFailDetailData.T_ATOM_FAIL_DETAIL_DATA) {
            return dslContext.select(
                ATOM_CODE.`as`(BK_ATOM_CODE),
                ERROR_CODE.`as`(BK_ERROR_CODE),
                ERROR_TYPE.`as`(BK_ERROR_TYPE),
                ERROR_MSG.`as`(BK_ERROR_MSG)
            )
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .groupBy(ATOM_CODE, ERROR_CODE, ERROR_TYPE)
                .fetch()
        }
    }
}
