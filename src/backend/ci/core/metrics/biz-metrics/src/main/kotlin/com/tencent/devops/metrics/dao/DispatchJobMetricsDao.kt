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

package com.tencent.devops.metrics.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.common.event.pojo.measure.DispatchJobMetricsData
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.MaxJobConcurrencyVO
import com.tencent.devops.model.metrics.tables.TDispatchJobDailyMetrics
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class DispatchJobMetricsDao {

    fun saveDispatchJobMetrics(
        dslContext: DSLContext,
        jobMetricsData: DispatchJobMetricsData
    ) {
        with(TDispatchJobDailyMetrics.T_DISPATCH_JOB_DAILY_METRICS) {
            dslContext.insertInto(this)
                .set(ID, jobMetricsData.id)
                .set(PROJECT_ID, jobMetricsData.projectId)
                .set(PRODUCT_ID, jobMetricsData.productId)
                .set(THE_DATE, jobMetricsData.theDate)
                .set(JOB_TYPE, jobMetricsData.jobType)
                .set(CHANNEL_CODE, jobMetricsData.channelCode)
                .set(MAX_JOB_CONCURRENCY, jobMetricsData.maxJobConcurrency)
                .set(SUM_JOB_COST, jobMetricsData.sumJobCost)
                .set(CHANNEL_CODE, jobMetricsData.channelCode)
                .execute()
        }
    }

    fun getMaxJobConcurrency(
        dslContext: DSLContext,
        dispatchJobReq: BaseQueryReqVO
    ): MaxJobConcurrencyVO? {
        val startDateTime =
            DateTimeUtil.stringToLocalDate(dispatchJobReq.startTime!!)!!.atStartOfDay()
        val endDateTime =
            DateTimeUtil.stringToLocalDate(dispatchJobReq.endTime!!)!!.atStartOfDay()
        with(TDispatchJobDailyMetrics.T_DISPATCH_JOB_DAILY_METRICS) {
            val subQuery = dslContext.select(
                PROJECT_ID,
                JOB_TYPE,
                DSL.max(MAX_JOB_CONCURRENCY.cast(Int::class.java)).`as`("maxJobConcurrency")
            ).from(this)
                .where(PROJECT_ID.eq(dispatchJobReq.projectId))
                .and(CREATE_TIME.between(startDateTime, endDateTime))
                .and(CHANNEL_CODE.eq("BS"))
                .groupBy(PROJECT_ID, JOB_TYPE)
                .asTable("subQuery")

            return dslContext.select(
                subQuery.field("PROJECT_ID", String::class.java),
                DSL.max(
                    DSL.`when`(
                        subQuery.field("JOB_TYPE", String::class.java)!!.eq("DOCKER_VM"),
                        subQuery.field("maxJobConcurrency", Int::class.java)
                    ).otherwise(0)
                ).`as`("VM最大并发"),
                DSL.max(
                    DSL.`when`(
                        subQuery.field("JOB_TYPE", String::class.java)!!.eq("DOCKER_DEVCLOUD"),
                        subQuery.field("maxJobConcurrency", Int::class.java)
                    ).otherwise(0)
                ).`as`("DevCloud-Linux最大并发"),
                DSL.max(
                    DSL.`when`(
                        subQuery.field("JOB_TYPE", String::class.java)!!.eq("MACOS_DEVCLOUD"),
                        subQuery.field("maxJobConcurrency", Int::class.java)
                    ).otherwise(0)
                ).`as`("DevCloud-macOS最大并发"),
                DSL.max(
                    DSL.`when`(
                        subQuery.field("JOB_TYPE", String::class.java)!!.eq("WINDOWS_DEVCLOUD"),
                        subQuery.field("maxJobConcurrency", Int::class.java)
                    ).otherwise(0)
                ).`as`("DevCloud-Windows最大并发"),
                DSL.max(
                    DSL.`when`(
                        subQuery.field("JOB_TYPE", String::class.java)!!.eq("BUILD_LESS"),
                        subQuery.field("maxJobConcurrency", Int::class.java)
                    ).otherwise(0)
                ).`as`("无编译环境最大并发"),
                DSL.max(
                    DSL.`when`(
                        subQuery.field("JOB_TYPE", String::class.java)!!.eq("OTHER"),
                        subQuery.field("maxJobConcurrency", Int::class.java)
                    ).otherwise(0)
                ).`as`("第三方构建机最大并发")
            ).from(subQuery)
                .groupBy(subQuery.field("PROJECT_ID", String::class.java))
                .skipCheck()
                .fetchAny()?.let {
                    MaxJobConcurrencyVO(
                        projectId = it.value1(),
                        dockerVm = it.value2(),
                        dockerDevcloud = it.value3(),
                        macosDevcloud = it.value4(),
                        windowsDevcloud = it.value5(),
                        buildLess = it.value6(),
                        other = it.value7()
                    )
                }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchJobMetricsDao::class.java)
    }
}
