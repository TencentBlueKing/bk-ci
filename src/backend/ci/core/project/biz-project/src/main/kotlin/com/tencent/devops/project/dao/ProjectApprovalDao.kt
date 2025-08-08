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

package com.tencent.devops.project.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.model.project.tables.TProjectApproval
import com.tencent.devops.model.project.tables.records.TProjectApprovalRecord
import com.tencent.devops.project.pojo.ProjectApprovalInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.enums.ProjectAuthSecrecyStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("LongParameterList")
class ProjectApprovalDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        projectCreateInfo: ProjectCreateInfo,
        approvalStatus: Int,
        subjectScopes: List<SubjectScopeInfo>,
        tipsStatus: Int
    ): Int {
        with(TProjectApproval.T_PROJECT_APPROVAL) {
            return dslContext.insertInto(
                this,
                PROJECT_NAME,
                ENGLISH_NAME,
                DESCRIPTION,
                BG_ID,
                BG_NAME,
                BUSINESS_LINE_ID,
                BUSINESS_LINE_NAME,
                DEPT_ID,
                DEPT_NAME,
                CENTER_ID,
                CENTER_NAME,
                CREATOR,
                CREATED_AT,
                UPDATOR,
                UPDATED_AT,
                APPROVAL_STATUS,
                LOGO_ADDR,
                SUBJECT_SCOPES,
                AUTH_SECRECY,
                TIPS_STATUS,
                PROJECT_TYPE,
                PRODUCT_ID,
                PRODUCT_NAME,
                PROPERTIES
            ).values(
                projectCreateInfo.projectName,
                projectCreateInfo.englishName,
                projectCreateInfo.description,
                projectCreateInfo.bgId,
                projectCreateInfo.bgName,
                projectCreateInfo.businessLineId,
                projectCreateInfo.businessLineName,
                projectCreateInfo.deptId,
                projectCreateInfo.deptName,
                projectCreateInfo.centerId,
                projectCreateInfo.centerName,
                userId,
                LocalDateTime.now(),
                userId,
                LocalDateTime.now(),
                approvalStatus,
                projectCreateInfo.logoAddress ?: "",
                JsonUtil.toJson(subjectScopes, false),
                projectCreateInfo.authSecrecy ?: ProjectAuthSecrecyStatus.PUBLIC.value,
                tipsStatus,
                projectCreateInfo.projectType,
                projectCreateInfo.productId,
                projectCreateInfo.productName,
                projectCreateInfo.properties?.let {
                    JsonUtil.toJson(it, false)
                }
            ).onDuplicateKeyUpdate()
                .set(PROJECT_NAME, projectCreateInfo.projectName)
                .set(DESCRIPTION, projectCreateInfo.description)
                .set(BG_ID, projectCreateInfo.bgId)
                .set(BG_NAME, projectCreateInfo.bgName)
                .set(DEPT_ID, projectCreateInfo.deptId)
                .set(DEPT_NAME, projectCreateInfo.deptName)
                .set(CENTER_ID, projectCreateInfo.centerId)
                .set(CENTER_NAME, projectCreateInfo.centerName)
                .set(LOGO_ADDR, projectCreateInfo.logoAddress)
                .set(SUBJECT_SCOPES, JsonUtil.toJson(subjectScopes, false))
                .set(AUTH_SECRECY, projectCreateInfo.authSecrecy)
                .set(APPROVAL_STATUS, approvalStatus)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(UPDATOR, userId)
                .set(TIPS_STATUS, tipsStatus)
                .set(PROJECT_TYPE, projectCreateInfo.projectType)
                .set(PRODUCT_ID, projectCreateInfo.productId)
                .set(PRODUCT_NAME, projectCreateInfo.productName)
                .set(PROPERTIES, projectCreateInfo.properties?.let {
                    JsonUtil.toJson(it, false)
                })
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        userId: String,
        projectUpdateInfo: ProjectUpdateInfo,
        approvalStatus: Int,
        subjectScopes: List<SubjectScopeInfo>,
        tipsStatus: Int
    ): Int {
        return with(TProjectApproval.T_PROJECT_APPROVAL) {
            dslContext.update(this)
                .set(PROJECT_NAME, projectUpdateInfo.projectName)
                .set(DESCRIPTION, projectUpdateInfo.description)
                .set(BG_ID, projectUpdateInfo.bgId)
                .set(BG_NAME, projectUpdateInfo.bgName)
                .set(BUSINESS_LINE_ID, projectUpdateInfo.businessLineId)
                .set(BUSINESS_LINE_NAME, projectUpdateInfo.businessLineName)
                .set(DEPT_ID, projectUpdateInfo.deptId)
                .set(DEPT_NAME, projectUpdateInfo.deptName)
                .set(CENTER_ID, projectUpdateInfo.centerId)
                .set(CENTER_NAME, projectUpdateInfo.centerName)
                .set(LOGO_ADDR, projectUpdateInfo.logoAddress)
                .set(SUBJECT_SCOPES, projectUpdateInfo.subjectScopes?.let { JsonUtil.toJson(it) })
                .set(AUTH_SECRECY, projectUpdateInfo.authSecrecy)
                .set(APPROVAL_STATUS, approvalStatus)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(UPDATOR, userId)
                .set(TIPS_STATUS, tipsStatus)
                .set(PROJECT_TYPE, projectUpdateInfo.projectType)
                .set(PRODUCT_ID, projectUpdateInfo.productId)
                .set(PRODUCT_NAME, projectUpdateInfo.productName)
                .set(PROPERTIES, projectUpdateInfo.properties?.let {
                    JsonUtil.toJson(it, false)
                })
                .where(ENGLISH_NAME.eq(projectUpdateInfo.englishName))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectApprovalInfo: ProjectApprovalInfo
    ): Int {
        return with(TProjectApproval.T_PROJECT_APPROVAL) {
            dslContext.update(this)
                .set(PROJECT_NAME, projectApprovalInfo.projectName)
                .set(DESCRIPTION, projectApprovalInfo.description)
                .set(BG_ID, projectApprovalInfo.bgId?.toLong())
                .set(BG_NAME, projectApprovalInfo.bgName)
                .set(BUSINESS_LINE_ID, projectApprovalInfo.businessLineId)
                .set(BUSINESS_LINE_NAME, projectApprovalInfo.businessLineName)
                .set(DEPT_ID, projectApprovalInfo.deptId?.toLong())
                .set(DEPT_NAME, projectApprovalInfo.deptName)
                .set(CENTER_ID, projectApprovalInfo.centerId?.toLong())
                .set(CENTER_NAME, projectApprovalInfo.centerName)
                .set(LOGO_ADDR, projectApprovalInfo.logoAddr)
                .set(SUBJECT_SCOPES, projectApprovalInfo.subjectScopes?.let { JsonUtil.toJson(it) })
                .set(AUTH_SECRECY, projectApprovalInfo.authSecrecy)
                .set(APPROVAL_STATUS, projectApprovalInfo.approvalStatus)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(UPDATOR, projectApprovalInfo.updator)
                .set(TIPS_STATUS, projectApprovalInfo.tipsStatus)
                .set(PROJECT_TYPE, projectApprovalInfo.projectType)
                .set(PRODUCT_ID, projectApprovalInfo.productId)
                .set(PRODUCT_NAME, projectApprovalInfo.productName)
                .set(PROPERTIES, projectApprovalInfo.properties?.let {
                    JsonUtil.toJson(it, false)
                })
                .where(ENGLISH_NAME.eq(projectApprovalInfo.englishName))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String
    ) {
        return with(TProjectApproval.T_PROJECT_APPROVAL) {
            dslContext.delete(this).where(ENGLISH_NAME.eq(projectId)).execute()
        }
    }

    fun getByEnglishName(dslContext: DSLContext, englishName: String): ProjectApprovalInfo? {
        with(TProjectApproval.T_PROJECT_APPROVAL) {
            val record =
                dslContext.selectFrom(this).where(ENGLISH_NAME.eq(englishName)).fetchAny() ?: return null
            return convert(record)
        }
    }

    fun updateApprovalStatusByUser(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        approvalStatus: Int,
        tipsStatus: Int
    ): Int {
        return with(TProjectApproval.T_PROJECT_APPROVAL) {
            dslContext.update(this)
                .set(APPROVAL_STATUS, approvalStatus)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(UPDATOR, userId)
                .set(TIPS_STATUS, tipsStatus)
                .where(ENGLISH_NAME.eq(projectId))
                .execute()
        }
    }

    fun updateApprovalStatusByCallback(
        dslContext: DSLContext,
        projectCode: String,
        approver: String,
        approvalStatus: Int,
        tipsStatus: Int
    ): Int {
        with(TProjectApproval.T_PROJECT_APPROVAL) {
            return dslContext.update(this)
                .set(APPROVAL_STATUS, approvalStatus)
                .set(APPROVER, approver)
                .set(APPROVAL_TIME, LocalDateTime.now())
                .set(TIPS_STATUS, tipsStatus)
                .where(ENGLISH_NAME.eq(projectCode))
                .execute()
        }
    }

    fun updateTipsStatus(
        dslContext: DSLContext,
        projectId: String,
        tipsStatus: Int
    ): Int {
        with(TProjectApproval.T_PROJECT_APPROVAL) {
            return dslContext.update(this)
                .set(TIPS_STATUS, tipsStatus)
                .where(ENGLISH_NAME.eq(projectId))
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
                businessLineId = businessLineId,
                businessLineName = businessLineName,
                deptId = deptId?.toString(),
                deptName = deptName,
                centerId = centerId?.toString(),
                centerName = centerName,
                creator = creator,
                createdAt = DateTimeUtil.toDateTime(createdAt, "yyyy-MM-dd"),
                updator = updator,
                updatedAt = updatedAt?.let { DateTimeUtil.toDateTime(it, "yyyy-MM-dd") },
                approvalStatus = approvalStatus,
                logoAddr = logoAddr,
                subjectScopes = subjectScopes?.let {
                    JsonUtil.to(it, object : TypeReference<ArrayList<SubjectScopeInfo>>() {})
                },
                authSecrecy = authSecrecy,
                approvalTime = approvalTime?.let { DateTimeUtil.toDateTime(it, "yyyy-MM-dd'T'HH:mm:ssZ") },
                approver = approver,
                tipsStatus = tipsStatus,
                projectType = projectType,
                productId = productId,
                productName = productName,
                properties = properties?.let { JsonUtil.to(it, ProjectProperties::class.java) }
            )
        }
    }
}
