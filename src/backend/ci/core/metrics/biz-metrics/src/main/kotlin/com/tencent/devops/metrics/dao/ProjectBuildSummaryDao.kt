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
 *
 */

package com.tencent.devops.metrics.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsData
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.ProjectUserCountV0
import com.tencent.devops.model.metrics.tables.TProjectBuildSummaryDaily
import com.tencent.devops.model.metrics.tables.TProjectUserDaily
import com.tencent.devops.model.metrics.tables.TProjectUserOperateDaily
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class ProjectBuildSummaryDao {
    companion object {
        private val TRIGGER_FIELD_MAP = with(TProjectBuildSummaryDaily.T_PROJECT_BUILD_SUMMARY_DAILY) {
            mapOf(
                StartType.MANUAL.name to MANUAL_BUILD_COUNT,
                StartType.TIME_TRIGGER.name to TIME_BUILD_COUNT,
                StartType.SERVICE.name to OPENAPI_BUILD_COUNT,
                StartType.PIPELINE.name to SUB_PIPELINE_BUILD_COUNT,
                StartType.REMOTE.name to REMOTE_BUILD_COUNT,
                StartType.WEB_HOOK.name to WEBHOOK_BUILD_COUNT
            )
        }
    }

    fun saveBuildCount(
        dslContext: DSLContext,
        projectId: String,
        productId: Int,
        trigger: String
    ) {
        val triggerField = TRIGGER_FIELD_MAP[trigger] ?: return
        with(TProjectBuildSummaryDaily.T_PROJECT_BUILD_SUMMARY_DAILY) {
            dslContext.insertInto(this)
                .set(PROJECT_ID, projectId)
                .set(PRODUCT_ID, productId)
                .set(BUILD_COUNT, BUILD_COUNT + 1)
                .set(triggerField, triggerField + 1)
                .set(THE_DATE, LocalDate.now())
                .set(CREATE_TIME, LocalDateTime.now())
                .onDuplicateKeyUpdate()
                .set(BUILD_COUNT, BUILD_COUNT + 1)
                .set(triggerField, triggerField + 1)
                .execute()
        }
    }

    fun saveProjectUser(
        dslContext: DSLContext,
        projectId: String,
        userId: String
    ): Int {
        return with(TProjectUserDaily.T_PROJECT_USER_DAILY) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                USER_ID,
                THE_DATE,
                CREATE_TIME
            ).values(
                projectId,
                userId,
                LocalDate.now(),
                LocalDateTime.now()
            ).onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun getProjectUserCount(
        dslContext: DSLContext,
        baseQueryReq: BaseQueryReqVO
    ): ProjectUserCountV0? {
        val startDateTime =
            DateTimeUtil.stringToLocalDate(baseQueryReq.startTime!!)!!.atStartOfDay()
        val endDateTime =
            DateTimeUtil.stringToLocalDate(baseQueryReq.endTime!!)!!.atStartOfDay()
        return with(TProjectUserDaily.T_PROJECT_USER_DAILY) {
            val users = dslContext.selectDistinct(USER_ID).from(this)
                .where(PROJECT_ID.eq(baseQueryReq.projectId))
                .and(CREATE_TIME.between(startDateTime, endDateTime))
                .fetchInto(String::class.java)
            ProjectUserCountV0(
                projectId = baseQueryReq.projectId!!,
                userCount = users.size,
                users = users.joinToString(separator = ",")
            )
        }
    }

    fun saveUserCount(
        dslContext: DSLContext,
        projectId: String,
        productId: Int,
        theDate: LocalDate
    ) {
        with(TProjectBuildSummaryDaily.T_PROJECT_BUILD_SUMMARY_DAILY) {
            dslContext.insertInto(this)
                .set(PROJECT_ID, projectId)
                .set(PRODUCT_ID, productId)
                .set(THE_DATE, theDate)
                .set(CREATE_TIME, LocalDateTime.now())
                .set(USER_COUNT, USER_COUNT + 1)
                .onDuplicateKeyUpdate()
                .set(USER_COUNT, USER_COUNT + 1)
                .execute()
        }
    }

    fun saveUserOperateCount(
        dslContext: DSLContext,
        projectUserOperateMetricsData: ProjectUserOperateMetricsData,
        operateCount: Int
    ) {
        with(TProjectUserOperateDaily.T_PROJECT_USER_OPERATE_DAILY) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                USER_ID,
                OPERATE,
                THE_DATE,
                OPERATE_COUNT,
                CREATE_TIME
            ).values(
                projectUserOperateMetricsData.projectId,
                projectUserOperateMetricsData.userId,
                projectUserOperateMetricsData.operate,
                projectUserOperateMetricsData.theDate,
                operateCount,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun updateUserOperateCount(
        dslContext: DSLContext,
        projectUserOperateMetricsData: ProjectUserOperateMetricsData,
        operateCount: Int
    ) {
        with(projectUserOperateMetricsData) {
            with(TProjectUserOperateDaily.T_PROJECT_USER_OPERATE_DAILY) {
                dslContext.update(this)
                    .set(OPERATE_COUNT, OPERATE_COUNT + operateCount)
                    .where(
                        PROJECT_ID.eq(projectId).and(USER_ID.eq(userId))
                            .and(OPERATE.eq(operate)).and(THE_DATE.eq(theDate))
                    ).execute()
            }
        }
    }
}
