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
 *
 */

package com.tencent.devops.project.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.model.project.tables.TProjectApproval
import com.tencent.devops.model.project.tables.records.TProjectApprovalRecord
import com.tencent.devops.project.pojo.ProjectApprovalInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProjectApprovalDao {

    @Suppress("LongParameterList")
    fun create(
        dslContext: DSLContext,
        userId: String,
        projectCreateInfo: ProjectCreateInfo,
        approvalStatus: Int,
        subjectScopes: List<SubjectScopeInfo>
    ): Int {
        with(TProjectApproval.T_PROJECT_APPROVAL) {
            return dslContext.insertInto(
                this,
                PROJECT_NAME,
                ENGLISH_NAME,
                DESCRIPTION,
                BG_ID,
                BG_NAME,
                DEPT_ID,
                DEPT_NAME,
                CENTER_ID,
                CENTER_NAME,
                CREATOR,
                CREATED_AT,
                APPROVAL_STATUS,
                LOGO_ADDR,
                SUBJECT_SCOPES,
                AUTH_SECRECY
            ).values(
                projectCreateInfo.projectName,
                projectCreateInfo.englishName,
                projectCreateInfo.description,
                projectCreateInfo.bgId,
                projectCreateInfo.bgName,
                projectCreateInfo.deptId,
                projectCreateInfo.deptName,
                projectCreateInfo.centerId,
                projectCreateInfo.centerName,
                userId,
                LocalDateTime.now(),
                approvalStatus,
                projectCreateInfo.logoAddress ?: "",
                JsonUtil.toJson(subjectScopes, false),
                projectCreateInfo.authSecrecy ?: false
            ).execute()
        }
    }

    fun getByEnglishName(dslContext: DSLContext, englishName: String): ProjectApprovalInfo? {
        with(TProjectApproval.T_PROJECT_APPROVAL) {
            val record =
                dslContext.selectFrom(this).where(ENGLISH_NAME.eq(englishName)).fetchAny() ?: return null
            return convert(record)
        }
    }

    fun updateApprovalStatus(
        dslContext: DSLContext,
        userId: String,
        projectCode: String,
        statusEnum: ProjectApproveStatus
    ): Int {
        with(TProjectApproval.T_PROJECT_APPROVAL) {
            return dslContext.update(this)
                .set(APPROVAL_STATUS, statusEnum.status)
                .set(APPROVER, userId)
                .set(APPROVAL_TIME, LocalDateTime.now())
                .where(ENGLISH_NAME.eq(projectCode))
                .execute()
        }
    }

    fun convert(record: TProjectApprovalRecord): ProjectApprovalInfo {
        return with(record) {
            ProjectApprovalInfo(
                projectName = projectName,
                englishName = englishName,
                description = description,
                bgId = bgId?.toString(),
                bgName = bgName,
                deptId = deptId?.toString(),
                deptName = deptName,
                centerId = centerId?.toString(),
                centerName = centerName,
                creator = creator,
                createdAt = DateTimeUtil.toDateTime(createdAt, "yyyy-MM-dd"),
                updatedAt = updatedAt?.let { DateTimeUtil.toDateTime(it, "yyyy-MM-dd") },
                approvalStatus = approvalStatus,
                logoAddr = logoAddr,
                subjectScopes = subjectScopes?.let {
                    JsonUtil.to(it, object : TypeReference<ArrayList<SubjectScopeInfo>>() {})
                },
                authSecrecy = authSecrecy,
                approvalTime = approvalTime?.let { DateTimeUtil.toDateTime(it, "yyyy-MM-dd'T'HH:mm:ssZ") },
                approver = approver
            )
        }
    }
}
