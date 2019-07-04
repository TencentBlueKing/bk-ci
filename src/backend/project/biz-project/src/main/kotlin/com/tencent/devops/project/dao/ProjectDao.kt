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

package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.enums.ApproveStatus
import com.tencent.devops.project.pojo.user.UserDeptDetail
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.UpdateConditionStep
import org.springframework.stereotype.Repository
import org.springframework.util.StringUtils
import java.net.URLDecoder
import java.time.LocalDateTime

@Repository
class ProjectDao {

    fun existByEnglishName(
        dslContext: DSLContext,
        englishName: String,
        projectId: String?
    ): Boolean {
        with(TProject.T_PROJECT) {
            val step = dslContext.selectFrom(this)
                .where(ENGLISH_NAME.eq(englishName))

            if (!projectId.isNullOrBlank()) {
                step.and(PROJECT_ID.ne(projectId))
            }
            return step.fetchOne() != null
        }
    }

    fun existByProjectName(
        dslContext: DSLContext,
        projectName: String,
        projectId: String?
    ): Boolean {
        with(TProject.T_PROJECT) {
            val step = dslContext.selectFrom(this)
                .where(PROJECT_NAME.eq(projectName))
            if (!projectId.isNullOrBlank()) {
                step.and(PROJECT_ID.ne(projectId))
            }
            return step.fetchOne() != null
        }
    }

    fun listCCProjects(dslContext: DSLContext): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(CC_APP_ID.ne(0))
                .fetch()
        }
    }

    fun listProjectCodes(dslContext: DSLContext): List<String> {
        return with(TProject.T_PROJECT) {
            val query = dslContext.select(ENGLISH_NAME).from(this)
            query.fetch(ENGLISH_NAME, String::class.java)
        }
    }

    fun list(dslContext: DSLContext, projectIdList: Set<String>? = null): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            val query = dslContext.selectFrom(this)
            if (projectIdList != null && projectIdList.isNotEmpty())
                query.where(PROJECT_ID.`in`(projectIdList))
            query.fetch()
        }
    }

    /**
     * 根据英文名称(projectCode)查询name
     */
    fun listByCodes(dslContext: DSLContext, projectCodeList: Set<String>): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(ENGLISH_NAME.`in`(projectCodeList)).fetch()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String
    ): TProjectRecord? {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId)).fetchOne()
        }
    }

    fun updateAppName(
        dslContext: DSLContext,
        projectId: String,
        appName: String
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(CC_APP_NAME, appName)
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun batchUpdateAppName(
        dslContext: DSLContext,
        projects: Map<String, String>
    ): Int {
        with(TProject.T_PROJECT) {
            val sets = ArrayList<UpdateConditionStep<TProjectRecord>>()
            projects.forEach { projectId, ccAppName ->
                sets.add(
                    dslContext.update(this)
                        .set(CC_APP_NAME, ccAppName)
                        .where(PROJECT_ID.eq(projectId))
                )
            }
            if (sets.isNotEmpty()) {
                return dslContext.batch(sets).execute().size
            }
            return 0
        }
    }

    fun getByEnglishName(
        dslContext: DSLContext,
        englishName: String
    ): TProjectRecord? {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(ENGLISH_NAME.eq(englishName)).fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        userId: String,
        logoAddress: String,
        projectCreateInfo: ProjectCreateInfo,
        userDeptDetail: UserDeptDetail,
        projectId: String
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.insertInto(
                this,
                PROJECT_NAME,
                PROJECT_ID,
                ENGLISH_NAME,
                DESCRIPTION,
                BG_ID,
                BG_NAME,
                DEPT_ID,
                DEPT_NAME,
                CENTER_ID,
                CENTER_NAME,
                IS_SECRECY,
                KIND,
                CREATOR,
                CREATED_AT,
                PROJECT_TYPE,
                APPROVAL_STATUS,
                LOGO_ADDR,
                CREATOR_BG_NAME,
                CREATOR_DEPT_NAME,
                CREATOR_CENTER_NAME
            ).values(
                projectCreateInfo.projectName,
                projectId,
                projectCreateInfo.englishName,
                projectCreateInfo.description,
                0,
                "",
                0,
                "",
                0,
                "",
                projectCreateInfo.secrecy,
                projectCreateInfo.kind,
                userId,
                LocalDateTime.now(),
                projectCreateInfo.projectType,
                ApproveStatus.APPROVED.status,
                logoAddress,
                userDeptDetail.bgName,
                userDeptDetail.deptName,
                userDeptDetail.centerName
            ).execute()
        }
    }

    fun update(dslContext: DSLContext, userId: String, projectId: String, projectUpdateInfo: ProjectUpdateInfo): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(PROJECT_NAME, projectUpdateInfo.projectName)
                .set(BG_ID, projectUpdateInfo.bgId)
                .set(BG_NAME, projectUpdateInfo.bgName)
                .set(CENTER_ID, projectUpdateInfo.centerId)
                .set(CENTER_NAME, projectUpdateInfo.centerName)
                .set(DEPT_ID, projectUpdateInfo.deptId)
                .set(DEPT_NAME, projectUpdateInfo.deptName)
                .set(DESCRIPTION, projectUpdateInfo.description)
                .set(ENGLISH_NAME, projectUpdateInfo.englishName)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(UPDATOR, userId)
                .where(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun updateLogoAddress(dslContext: DSLContext, userId: String, projectId: String, logoAddress: String): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(this.LOGO_ADDR, logoAddress)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(UPDATOR, userId)
                .where(PROJECT_ID.eq(projectId)).execute()
        }
    }

    private fun TProject.generateQueryProjectCondition(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        grayFlag: Boolean,
        englishNames: Set<String>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (!StringUtils.isEmpty(projectName)) conditions.add(
            PROJECT_NAME.like(
                "%" + URLDecoder.decode(
                    projectName,
                    "UTF-8"
                ) + "%"
            )
        )
        if (!StringUtils.isEmpty(englishName)) conditions.add(
            ENGLISH_NAME.like(
                "%" + URLDecoder.decode(
                    englishName,
                    "UTF-8"
                ) + "%"
            )
        )
        if (!StringUtils.isEmpty(projectType)) conditions.add(PROJECT_TYPE.eq(projectType))
        if (!StringUtils.isEmpty(isSecrecy)) conditions.add(IS_SECRECY.eq(isSecrecy))
        if (!StringUtils.isEmpty(creator)) conditions.add(CREATOR.eq(creator))
        if (!StringUtils.isEmpty(approver)) conditions.add(APPROVER.eq(approver))
        if (!StringUtils.isEmpty(approvalStatus)) conditions.add(APPROVAL_STATUS.eq(approvalStatus))
        if (grayFlag) {
            if (englishNames == null) {
                conditions.add(ENGLISH_NAME.`in`(setOf<String>()))
            } else {
                conditions.add(ENGLISH_NAME.`in`(englishNames))
            }
        }
        return conditions
    }

    fun getProjectList(
        dslContext: DSLContext,
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean,
        englishNames: Set<String>?
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            val conditions = generateQueryProjectCondition(
                projectName,
                englishName,
                projectType,
                isSecrecy,
                creator,
                approver,
                approvalStatus,
                grayFlag,
                englishNames
            )
            return dslContext.selectFrom(this).where(conditions).orderBy(CREATED_AT.desc()).limit(offset, limit).fetch()
        }
    }

    fun updateEnabled(dslContext: DSLContext, userId: String, projectId: String, enabled: Boolean) {
        with(TProject.T_PROJECT) {
            dslContext.update(this)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(UPDATOR, userId)
                .set(ENABLED, enabled)
                .where(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun listByEnglishName(
        dslContext: DSLContext,
        englishNameList: List<String?>?
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            val condition = dslContext.selectFrom(this)
                .where(APPROVAL_STATUS.eq(2))

            if (englishNameList != null && englishNameList.isNotEmpty()) {
                condition.and(ENGLISH_NAME.`in`(englishNameList))
            }
            return condition.and(IS_OFFLINED.eq(false)).fetch()
        }
    }
}