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

package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.pojo.JobQuotaSystem
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.model.dispatch.tables.TDispatchQuotaSystem
import com.tencent.devops.model.dispatch.tables.records.TDispatchQuotaSystemRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class JobQuotaSystemDao {

    fun get(dslContext: DSLContext, jobQuotaVmType: JobQuotaVmType): TDispatchQuotaSystemRecord? {
        with(TDispatchQuotaSystem.T_DISPATCH_QUOTA_SYSTEM) {
            return dslContext.selectFrom(this)
                .where(VM_TYPE.eq(jobQuotaVmType.name))
                .fetchOne()
        }
    }

    fun list(dslContext: DSLContext): Result<TDispatchQuotaSystemRecord?> {
        with(TDispatchQuotaSystem.T_DISPATCH_QUOTA_SYSTEM) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun convert(vmType: JobQuotaVmType, record: TDispatchQuotaSystemRecord): JobQuotaSystem {
        return JobQuotaSystem(
            vmType = vmType,
            runningJobMaxSystem = record.runningJobsMaxSystem,
            runningJobMaxProject = record.runningJobsMaxProject,
            runningTimeJobMax = record.runningTimeJobMax,
            runningTimeJobMaxProject = record.runningTimeJobMaxProject,
            runningJobMaxGitCiSystem = record.runningJobsMaxGitciSystem,
            runningJobMaxGitCiProject = record.runningJobsMaxGitciProject,
            runningTimeJobMaxGitCi = record.runningTimeJobMaxGitci,
            runningTimeJobMaxProjectGitCi = record.runningTimeJobMaxProjectGitci,
            projectRunningJobThreshold = record.projectRunningJobThreshold,
            projectRunningTimeThreshold = record.projectRunningTimeThreshold,
            systemRunningJobThreshold = record.systemRunningJobThreshold,
            createdTime = record.createdTime.timestamp(),
            updatedTime = record.updatedTime.timestamp(),
            operator = record.operator
        )
    }

    fun add(dslContext: DSLContext, jobQuota: JobQuotaSystem) {
        with(TDispatchQuotaSystem.T_DISPATCH_QUOTA_SYSTEM) {
            val now = LocalDateTime.now()
            dslContext.insertInto(this,
                VM_TYPE,
                RUNNING_JOBS_MAX_SYSTEM,
                RUNNING_JOBS_MAX_PROJECT,
                RUNNING_TIME_JOB_MAX,
                RUNNING_TIME_JOB_MAX_PROJECT,
                RUNNING_JOBS_MAX_GITCI_SYSTEM,
                RUNNING_JOBS_MAX_GITCI_PROJECT,
                RUNNING_TIME_JOB_MAX_GITCI,
                RUNNING_TIME_JOB_MAX_PROJECT_GITCI,
                PROJECT_RUNNING_JOB_THRESHOLD,
                PROJECT_RUNNING_TIME_THRESHOLD,
                SYSTEM_RUNNING_JOB_THRESHOLD,
                CREATED_TIME,
                UPDATED_TIME,
                OPERATOR
            )
                .values(
                    jobQuota.vmType.name,
                    jobQuota.runningJobMaxSystem,
                    jobQuota.runningJobMaxProject,
                    jobQuota.runningTimeJobMax,
                    jobQuota.runningTimeJobMaxProject,
                    jobQuota.runningJobMaxGitCiSystem,
                    jobQuota.runningJobMaxGitCiProject,
                    jobQuota.runningTimeJobMaxGitCi,
                    jobQuota.runningTimeJobMaxProjectGitCi,
                    jobQuota.projectRunningJobThreshold,
                    jobQuota.projectRunningTimeThreshold,
                    jobQuota.systemRunningJobThreshold,
                    now,
                    now,
                    jobQuota.operator
                )
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, jobQuotaVmType: JobQuotaVmType) {
        with(TDispatchQuotaSystem.T_DISPATCH_QUOTA_SYSTEM) {
            dslContext.deleteFrom(this)
                .where(VM_TYPE.eq(jobQuotaVmType.name))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        jobQuotaVmType: JobQuotaVmType,
        jobQuota: JobQuotaSystem
    ): Boolean {
        with(TDispatchQuotaSystem.T_DISPATCH_QUOTA_SYSTEM) {
            return dslContext.update(this)
                .set(RUNNING_JOBS_MAX_SYSTEM, jobQuota.runningJobMaxSystem)
                .set(RUNNING_JOBS_MAX_PROJECT, jobQuota.runningJobMaxProject)
                .set(RUNNING_TIME_JOB_MAX, jobQuota.runningTimeJobMax)
                .set(RUNNING_TIME_JOB_MAX_PROJECT, jobQuota.runningTimeJobMaxProject)
                .set(RUNNING_JOBS_MAX_GITCI_SYSTEM, jobQuota.runningJobMaxGitCiSystem)
                .set(RUNNING_JOBS_MAX_GITCI_PROJECT, jobQuota.runningJobMaxGitCiProject)
                .set(RUNNING_TIME_JOB_MAX_GITCI, jobQuota.runningTimeJobMaxGitCi)
                .set(RUNNING_TIME_JOB_MAX_PROJECT_GITCI, jobQuota.runningTimeJobMaxProjectGitCi)
                .set(PROJECT_RUNNING_JOB_THRESHOLD, jobQuota.projectRunningJobThreshold)
                .set(PROJECT_RUNNING_TIME_THRESHOLD, jobQuota.projectRunningTimeThreshold)
                .set(SYSTEM_RUNNING_JOB_THRESHOLD, jobQuota.systemRunningJobThreshold)
                .set(UPDATED_TIME, LocalDateTime.now())
                .set(OPERATOR, jobQuota.operator ?: "")
                .where(VM_TYPE.eq(jobQuotaVmType.name))
                .execute() == 1
        }
    }
}