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

package com.tencent.devops.dispatch.dao

import com.tencent.devops.dispatch.pojo.JobQuotaProject
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.model.dispatch.tables.TDispatchQuotaProject
import com.tencent.devops.model.dispatch.tables.records.TDispatchQuotaProjectRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class JobQuotaProjectDao {

    fun get(
        dslContext: DSLContext,
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String
    ): TDispatchQuotaProjectRecord? {
        with(TDispatchQuotaProject.T_DISPATCH_QUOTA_PROJECT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VM_TYPE.eq(jobQuotaVmType.name))
                .and(CHANNEL_CODE.eq(channelCode))
                .fetchOne()
        }
    }

    fun list(dslContext: DSLContext, projectId: String?): Result<TDispatchQuotaProjectRecord?> {
        with(TDispatchQuotaProject.T_DISPATCH_QUOTA_PROJECT) {
            val where = dslContext.selectFrom(this)
            return if (projectId.isNullOrBlank()) {
                where.orderBy(UPDATED_TIME.desc()).fetch()
            } else {
                where.where(PROJECT_ID.like("%$projectId%")).orderBy(UPDATED_TIME.desc()).fetch()
            }
        }
    }

    fun add(dslContext: DSLContext, jobQuota: JobQuotaProject) {
        with(TDispatchQuotaProject.T_DISPATCH_QUOTA_PROJECT) {
            val now = LocalDateTime.now()
            dslContext.insertInto(this,
                PROJECT_ID,
                VM_TYPE,
                CHANNEL_CODE,
                RUNNING_JOBS_MAX,
                RUNNING_TIME_JOB_MAX,
                RUNNING_TIME_PROJECT_MAX,
                CREATED_TIME,
                UPDATED_TIME,
                OPERATOR
            )
                .values(
                    jobQuota.projectId,
                    jobQuota.vmType.name,
                    jobQuota.channelCode,
                    jobQuota.runningJobMax,
                    jobQuota.runningTimeJobMax,
                    jobQuota.runningTimeProjectMax,
                    now,
                    now,
                    jobQuota.operator ?: ""
                )
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String
    ) {
        with(TDispatchQuotaProject.T_DISPATCH_QUOTA_PROJECT) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VM_TYPE.eq(jobQuotaVmType.name))
                .and(CHANNEL_CODE.eq(channelCode))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        jobQuota: JobQuotaProject
    ): Boolean {
        with(TDispatchQuotaProject.T_DISPATCH_QUOTA_PROJECT) {
            return dslContext.update(this)
                .set(RUNNING_JOBS_MAX, jobQuota.runningJobMax)
                .set(RUNNING_TIME_JOB_MAX, jobQuota.runningTimeJobMax)
                .set(RUNNING_TIME_PROJECT_MAX, jobQuota.runningTimeProjectMax)
                .set(UPDATED_TIME, LocalDateTime.now())
                .set(OPERATOR, jobQuota.operator ?: "")
                .where(PROJECT_ID.eq(projectId))
                .and(VM_TYPE.eq(jobQuotaVmType.name))
                .and(CHANNEL_CODE.eq(jobQuota.channelCode))
                .execute() == 1
        }
    }
}
