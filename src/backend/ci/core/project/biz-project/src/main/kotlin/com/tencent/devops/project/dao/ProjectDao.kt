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
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.PaasProject
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.enums.ApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
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

    fun existByEnglishName(dslContext: DSLContext, englishName: String, projectId: String?): Boolean {
        with(TProject.T_PROJECT) {
            val step = dslContext.selectFrom(this)
                .where(ENGLISH_NAME.eq(englishName))

            if (!projectId.isNullOrBlank()) {
                step.and(PROJECT_ID.ne(projectId))
            }
            return step.fetchOne() != null
        }
    }

    fun checkEnglishName(dslContext: DSLContext, englishName: String): Boolean {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(ENGLISH_NAME.eq(englishName)).fetchOne() != null
        }
    }

    fun existByProjectName(dslContext: DSLContext, projectName: String, projectId: String?): Boolean {
        with(TProject.T_PROJECT) {
            val step = dslContext.selectFrom(this)
                .where(PROJECT_NAME.eq(projectName))
            if (!projectId.isNullOrBlank()) {
                step.and(ENGLISH_NAME.ne(projectId))
            }
            return step.fetchOne() != null
        }
    }

    fun checkProjectNameByEnglishName(dslContext: DSLContext, projectName: String, englishName: String?): Boolean {
        with(TProject.T_PROJECT) {
            val step = dslContext.selectFrom(this)
                .where(PROJECT_NAME.eq(projectName))
            if (!englishName.isNullOrBlank()) {
                step.and(ENGLISH_NAME.ne(englishName))
            }
            return step.fetchOne() != null
        }
    }

    fun listCCProjects(dslContext: DSLContext): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(CC_APP_ID.ne(0)).fetch()
        }
    }

    fun listProjectCodes(dslContext: DSLContext): List<String> {
        return with(TProject.T_PROJECT) {
            dslContext.select(ENGLISH_NAME).from(this).fetch(ENGLISH_NAME, String::class.java)
        }
    }

    fun list(dslContext: DSLContext, projectIdList: Set<String>): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this).where(PROJECT_ID.`in`(projectIdList)).fetch()
        }
    }

    fun getAllProject(dslContext: DSLContext): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this).fetch()
        }
    }

    fun list(dslContext: DSLContext, limit: Int, offset: Int): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this).where(ENABLED.eq(true)).limit(limit).offset(offset).fetch()
        }
    }

    fun getCount(dslContext: DSLContext): Long {
        return with(TProject.T_PROJECT) {
            dslContext.selectCount().from(this).where(ENABLED.eq(true)).fetchOne(0, Long::class.java)
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

    fun get(dslContext: DSLContext, projectId: String): TProjectRecord? {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).fetchOne()
        }
    }

    /**
     * 根据组织架构来查询name
     */
    fun listByGroup(
        dslContext: DSLContext,
        bgName: String?,
        deptName: String?,
        centerName: String?
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            if (!bgName.isNullOrBlank()) {
                conditions.add(BG_NAME.like("%${URLDecoder.decode(bgName, "UTF-8")}%"))
            }
            if (!deptName.isNullOrBlank()) {
                conditions.add(DEPT_NAME.like("%${URLDecoder.decode(deptName, "UTF-8")}%"))
            }
            if (!centerName.isNullOrBlank()) {
                conditions.add(CENTER_NAME.like("%${URLDecoder.decode(centerName, "UTF-8")}%"))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    /**
     * 根据组织架构来查询name
     */
    fun listByGroupId(
        dslContext: DSLContext,
        bgId: Long?,
        deptId: Long?,
        centerId: Long?
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            if (bgId != null) {
                conditions.add(BG_ID.eq(bgId))
            }
            if (deptId != null) {
                conditions.add(DEPT_ID.eq(deptId))
            }
            if (centerId != null) {
                conditions.add(CENTER_ID.eq(centerId))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    /**
     * 根据deptId+centerName来查询
     */
    fun listByOrganization(dslContext: DSLContext, deptId: Long?, centerName: String?): Result<TProjectRecord>? {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            if (deptId != null) {
                conditions.add(DEPT_ID.eq(deptId))
            }
            if (!centerName.isNullOrBlank()) {
                conditions.add(CENTER_NAME.like("%${URLDecoder.decode(centerName, "UTF-8")}%"))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    /**
     * 根据bgId+deptName+centerName来查询
     */
    fun listByOrganization(
        dslContext: DSLContext,
        bgId: Long?,
        deptName: String?,
        centerName: String?
    ): Result<TProjectRecord>? {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            if (bgId != null) {
                conditions.add(BG_ID.eq(bgId))
            }
            if (!deptName.isNullOrBlank()) {
                conditions.add(DEPT_NAME.like("%${URLDecoder.decode(deptName, "UTF-8")}%"))
            }
            if (!centerName.isNullOrBlank()) {
                conditions.add(CENTER_NAME.like("%${URLDecoder.decode(centerName, "UTF-8")}%"))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun updateAppName(dslContext: DSLContext, projectId: String, appName: String): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this).set(CC_APP_NAME, appName).where(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun batchUpdateAppName(dslContext: DSLContext, projects: Map<String, String>): Int {
        with(TProject.T_PROJECT) {
            val sets = ArrayList<UpdateConditionStep<TProjectRecord>>()
            projects.forEach { (projectId, ccAppName) ->
                sets.add(dslContext.update(this).set(CC_APP_NAME, ccAppName).where(PROJECT_ID.eq(projectId)))
            }
            if (sets.isNotEmpty()) {
                return dslContext.batch(sets).execute().size
            }
            return 0
        }
    }

    fun getByEnglishName(dslContext: DSLContext, englishName: String): TProjectRecord? {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(ENGLISH_NAME.eq(englishName)).fetchOne()
        }
    }

    fun create(dslContext: DSLContext, paasProject: PaasProject): Int {
        with(TProject.T_PROJECT) {
            return dslContext.insertInto(
                this,
                APPROVAL_STATUS,
                APPROVAL_TIME,
                APPROVER,
                BG_ID,
                BG_NAME,
                CC_APP_ID,
                CENTER_ID,
                CENTER_NAME,
                CREATED_AT,
                CREATOR,
                DATA_ID,
                DEPLOY_TYPE,
                DEPT_ID,
                DEPT_NAME,
                DESCRIPTION,
                ENGLISH_NAME,
                EXTRA,
                IS_OFFLINED,
                IS_SECRECY,
                KIND,
                LOGO_ADDR,
                PROJECT_ID,
                PROJECT_NAME,
                PROJECT_TYPE,
                REMARK,
                UPDATED_AT,
                USE_BK,
                APPROVAL_STATUS,
                ENABLED
            )
                .values(
                    paasProject.approval_status,
                    paasProject.approval_time,
                    paasProject.approver,
                    paasProject.bg_id,
                    paasProject.bg_name,
                    paasProject.cc_app_id,
                    paasProject.center_id,
                    paasProject.center_name,
                    paasProject.created_at.time,
                    paasProject.creator,
                    paasProject.data_id,
                    paasProject.deploy_type,
                    paasProject.dept_id,
                    paasProject.dept_name,
                    paasProject.description,
                    paasProject.english_name,
                    paasProject.extra,
                    paasProject.is_offlined,
                    paasProject.is_secrecy,
                    paasProject.kind,
                    paasProject.logo_addr,
                    paasProject.project_id,
                    paasProject.project_name,
                    paasProject.project_type,
                    paasProject.remark,
                    paasProject.updated_at?.time,
                    paasProject.use_bk,
                    ApproveStatus.APPROVED.status,
                    true
                )
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.delete(this).where(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun create(
        dslContext: DSLContext,
        userId: String,
        logoAddress: String,
        projectCreateInfo: ProjectCreateInfo,
        userDeptDetail: UserDeptDetail,
        projectId: String,
        channelCode: ProjectChannelCode = ProjectChannelCode.BS
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
                CREATOR_CENTER_NAME,
                CHANNEL,
                ENABLED
            ).values(
                projectCreateInfo.projectName,
                projectId,
                projectCreateInfo.englishName,
                projectCreateInfo.description,
                projectCreateInfo.bgId,
                projectCreateInfo.bgName,
                projectCreateInfo.deptId,
                projectCreateInfo.deptName,
                projectCreateInfo.centerId,
                projectCreateInfo.centerName,
                projectCreateInfo.secrecy,
                projectCreateInfo.kind,
                userId,
                LocalDateTime.now(),
                projectCreateInfo.projectType,
                ApproveStatus.APPROVED.status,
                logoAddress,
                userDeptDetail.bgName,
                userDeptDetail.deptName,
                userDeptDetail.centerName,
                channelCode.name,
                true
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

    fun updateUsableStatus(dslContext: DSLContext, userId: String, projectId: String, enabled: Boolean): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(ENABLED, enabled)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(UPDATOR, userId)
                .where(PROJECT_ID.eq(projectId))
                .execute()
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
        if (!StringUtils.isEmpty(projectName))
            conditions.add(PROJECT_NAME.like("%${URLDecoder.decode(projectName, "UTF-8")}%"))
        if (!StringUtils.isEmpty(englishName))
            conditions.add(ENGLISH_NAME.like("%${URLDecoder.decode(englishName, "UTF-8")}%"))
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

    private fun TProject.generateQueryProjectCondition(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        grayFlag: Boolean,
        repoGrayFlag: Boolean,
        grayNames: Set<String>?,
        repoGrayNames: Set<String>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (!StringUtils.isEmpty(projectName))
            conditions.add(PROJECT_NAME.like("%${URLDecoder.decode(projectName, "UTF-8")}%"))
        if (!StringUtils.isEmpty(englishName))
            conditions.add(ENGLISH_NAME.like("%${URLDecoder.decode(englishName, "UTF-8")}%"))
        if (!StringUtils.isEmpty(projectType)) conditions.add(PROJECT_TYPE.eq(projectType))
        if (!StringUtils.isEmpty(isSecrecy)) conditions.add(IS_SECRECY.eq(isSecrecy))
        if (!StringUtils.isEmpty(creator)) conditions.add(CREATOR.eq(creator))
        if (!StringUtils.isEmpty(approver)) conditions.add(APPROVER.eq(approver))
        if (!StringUtils.isEmpty(approvalStatus)) conditions.add(APPROVAL_STATUS.eq(approvalStatus))
        if (grayFlag) {
            if (grayNames != null) {
                conditions.add(ENGLISH_NAME.`in`(grayNames))
            }
        }

        if (repoGrayFlag) {
            if (repoGrayNames != null) {
                conditions.add(ENGLISH_NAME.`in`(repoGrayNames))
            }
        }
        return conditions
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
        codeCCGrayFlag: Boolean,
        repoGrayFlag: Boolean,
        macosGrayFlag: Boolean,
        grayNames: Set<String>?,
        codeCCGrayNames: Set<String>?,
        repoGrayNames: Set<String>?,
        macosGrayNames: Set<String>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (!StringUtils.isEmpty(projectName))
            conditions.add(PROJECT_NAME.like("%${URLDecoder.decode(projectName, "UTF-8")}%"))
        if (!StringUtils.isEmpty(englishName))
            conditions.add(ENGLISH_NAME.like("%${URLDecoder.decode(englishName, "UTF-8")}%"))
        if (!StringUtils.isEmpty(projectType)) conditions.add(PROJECT_TYPE.eq(projectType))
        if (!StringUtils.isEmpty(isSecrecy)) conditions.add(IS_SECRECY.eq(isSecrecy))
        if (!StringUtils.isEmpty(creator)) conditions.add(CREATOR.eq(creator))
        if (!StringUtils.isEmpty(approver)) conditions.add(APPROVER.eq(approver))
        if (!StringUtils.isEmpty(approvalStatus)) conditions.add(APPROVAL_STATUS.eq(approvalStatus))
        if (grayFlag) {
            if (grayNames != null) {
                conditions.add(ENGLISH_NAME.`in`(grayNames))
            }
        }

        if (repoGrayFlag) {
            if (repoGrayNames != null) {
                conditions.add(ENGLISH_NAME.`in`(repoGrayNames))
            }
        }
        if (codeCCGrayFlag) {
            if (codeCCGrayNames != null) {
                conditions.add(ENGLISH_NAME.`in`(codeCCGrayNames))
            }
        }
        if (macosGrayFlag) {
            if (macosGrayNames != null) {
                conditions.add(ENGLISH_NAME.`in`(macosGrayNames))
            }
        }
        return conditions
    }

    // 项目灰度项目列表
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
                projectName = projectName,
                englishName = englishName,
                projectType = projectType,
                isSecrecy = isSecrecy,
                creator = creator,
                approver = approver,
                approvalStatus = approvalStatus,
                grayFlag = grayFlag,
                englishNames = englishNames
            )
            return dslContext.selectFrom(this).where(conditions).orderBy(CREATED_AT.desc()).limit(offset, limit).fetch()
        }
    }

    // repo灰度项目列表
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
        codeCCGrayFlag: Boolean,
        repoGrayFlag: Boolean,
        grayNames: Set<String>?,
        codeCCGrayNames: Set<String>?,
        repoGrayNames: Set<String>?
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            val conditions = generateQueryProjectCondition(
                projectName = projectName,
                englishName = englishName,
                projectType = projectType,
                isSecrecy = isSecrecy,
                creator = creator,
                approver = approver,
                approvalStatus = approvalStatus,
                grayFlag = grayFlag,
                repoGrayFlag = repoGrayFlag,
                grayNames = grayNames,
                repoGrayNames = repoGrayNames
            )
            return dslContext.selectFrom(this).where(conditions).orderBy(CREATED_AT.desc()).limit(offset, limit).fetch()
        }
    }

    // macos灰度项目列表
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
        codeCCGrayFlag: Boolean,
        repoGrayFlag: Boolean,
        macosGrayFlag: Boolean,
        grayNames: Set<String>?,
        codeCCGrayNames: Set<String>?,
        repoGrayNames: Set<String>?,
        macosGrayNames: Set<String>?
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            val conditions = generateQueryProjectCondition(
                projectName = projectName,
                englishName = englishName,
                projectType = projectType,
                isSecrecy = isSecrecy,
                creator = creator,
                approver = approver,
                approvalStatus = approvalStatus,
                grayFlag = grayFlag,
                codeCCGrayFlag = codeCCGrayFlag,
                repoGrayFlag = repoGrayFlag,
                macosGrayFlag = macosGrayFlag,
                grayNames = grayNames,
                codeCCGrayNames = codeCCGrayNames,
                repoGrayNames = repoGrayNames,
                macosGrayNames = macosGrayNames
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

    fun listByEnglishName(dslContext: DSLContext, englishNameList: List<String>): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(APPROVAL_STATUS.eq(2)).and(ENGLISH_NAME.`in`(englishNameList))
                .and(IS_OFFLINED.eq(false)).fetch()
        }
    }

    fun updateProjectFromOp(dslContext: DSLContext, projectInfoRequest: OpProjectUpdateInfoRequest) {
        with(TProject.T_PROJECT) {
            val step = dslContext.update(this)
                .set(PROJECT_NAME, projectInfoRequest.projectName)
                .set(BG_ID, projectInfoRequest.bgId)
                .set(BG_NAME, projectInfoRequest.bgName)
                .set(DEPT_ID, projectInfoRequest.deptId)
                .set(DEPT_NAME, projectInfoRequest.deptName)
                .set(CENTER_ID, projectInfoRequest.centerId)
                .set(CENTER_NAME, projectInfoRequest.centerName)
                .set(PROJECT_TYPE, projectInfoRequest.projectType)
                .set(IS_SECRECY, projectInfoRequest.secrecyFlag)
                .set(APPROVAL_STATUS, projectInfoRequest.approvalStatus)
                .set(APPROVAL_TIME, projectInfoRequest.approvalTime?.let { java.sql.Timestamp(it).toLocalDateTime() })
                .set(APPROVER, projectInfoRequest.approver)
                .set(USE_BK, projectInfoRequest.useBk)
                .set(CC_APP_ID, projectInfoRequest.ccAppId)
                .set(CC_APP_NAME, projectInfoRequest.cc_app_name ?: "")
                .set(UPDATOR, projectInfoRequest.updator)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(KIND, projectInfoRequest.kind)
                .set(ENABLED, projectInfoRequest.enabled)
                .set(PIPELINE_LIMIT, projectInfoRequest.pipelineLimit)

            if (projectInfoRequest.hybridCCAppId != null) {
                step.set(HYBRID_CC_APP_ID, projectInfoRequest.hybridCCAppId)
            }
            if (projectInfoRequest.enableExternal != null) {
                step.set(ENABLE_EXTERNAL, projectInfoRequest.enableExternal)
            }
            if (projectInfoRequest.enableIdc != null) {
                step.set(ENABLE_IDC, projectInfoRequest.enableIdc)
            }

            step.where(PROJECT_ID.eq(projectInfoRequest.projectId))
                .execute()
        }
    }

    fun getProjectCount(
        dslContext: DSLContext,
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        grayFlag: Boolean,
        englishNames: Set<String>?
    ): Int {
        with(TProject.T_PROJECT) {
            val conditions = generateQueryProjectCondition(
                projectName = projectName,
                englishName = englishName,
                projectType = projectType,
                isSecrecy = isSecrecy,
                creator = creator,
                approver = approver,
                approvalStatus = approvalStatus,
                grayFlag = grayFlag, englishNames = englishNames
            )
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, kotlin.Int::class.java)
        }
    }

    fun getProjectCount(
        dslContext: DSLContext,
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        grayFlag: Boolean,
        codeCCGrayFlag: Boolean,
        repoGrayFlag: Boolean,
        grayNames: Set<String>?,
        codeCCGrayNames: Set<String>?,
        repoGrayNames: Set<String>?
    ): Int {
        with(TProject.T_PROJECT) {
            val conditions = generateQueryProjectCondition(
                projectName = projectName,
                englishName = englishName,
                projectType = projectType,
                isSecrecy = isSecrecy,
                creator = creator,
                approver = approver,
                approvalStatus = approvalStatus,
                grayFlag = grayFlag,
                repoGrayFlag = repoGrayFlag,
                grayNames = grayNames,
                repoGrayNames = repoGrayNames
            )
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, kotlin.Int::class.java)
        }
    }

    fun getProjectCount(
        dslContext: DSLContext,
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        grayFlag: Boolean,
        codeCCGrayFlag: Boolean,
        repoGrayFlag: Boolean,
        macosGrayFlag: Boolean,
        grayNames: Set<String>?,
        codeCCGrayNames: Set<String>?,
        repoGrayNames: Set<String>?,
        macosGrayNames: Set<String>?
    ): Int {
        with(TProject.T_PROJECT) {
            val conditions = generateQueryProjectCondition(
                projectName = projectName,
                englishName = englishName,
                projectType = projectType,
                isSecrecy = isSecrecy,
                creator = creator,
                approver = approver,
                approvalStatus = approvalStatus,
                grayFlag = grayFlag,
                codeCCGrayFlag = codeCCGrayFlag,
                repoGrayFlag = repoGrayFlag,
                macosGrayFlag = macosGrayFlag,
                grayNames = grayNames,
                codeCCGrayNames = codeCCGrayNames,
                repoGrayNames = repoGrayNames,
                macosGrayNames = macosGrayNames
            )
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, kotlin.Int::class.java)
        }
    }
}