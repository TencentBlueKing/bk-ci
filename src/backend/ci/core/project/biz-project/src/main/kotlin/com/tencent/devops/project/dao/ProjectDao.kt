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

package com.tencent.devops.project.dao

import com.tencent.devops.common.service.utils.JooqUtils
import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.PaasProject
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.enums.ApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.util.ProjectUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime

@Suppress("ALL")
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

    fun listProjectCodes(dslContext: DSLContext): List<String> {
        return with(TProject.T_PROJECT) {
            dslContext.select(ENGLISH_NAME).from(this).fetch(ENGLISH_NAME, String::class.java)
        }
    }

    fun list(dslContext: DSLContext, projectIdList: Set<String>, enabled: Boolean? = null): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.`in`(projectIdList))
            if (enabled != null) {
                conditions.add(ENABLED.eq(enabled))
            }
            dslContext.selectFrom(this).where(conditions).fetch()
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

    fun listByChannel(
        dslContext: DSLContext,
        limit: Int,
        offset: Int,
        channelCode: ProjectChannelCode
    ): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this).where(ENABLED.eq(true).and(CHANNEL.eq(channelCode.name)))
                .limit(limit).offset(offset).fetch()
        }
    }

    fun getCount(dslContext: DSLContext): Long {
        return with(TProject.T_PROJECT) {
            dslContext.selectCount().from(this).where(ENABLED.eq(true)).fetchOne(0, Long::class.java)!!
        }
    }

    /**
     * 根据英文名称(projectCode)查询name
     */
    fun listByCodes(dslContext: DSLContext, projectCodeList: Set<String>, enabled: Boolean?): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(ENGLISH_NAME.`in`(projectCodeList))
                .let { if (null == enabled) it else it.and(ENABLED.eq(enabled)) }
                .limit(10000).fetch() // 硬限制10000保护
        }
    }

    fun get(dslContext: DSLContext, projectId: String): TProjectRecord? {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).fetchOne()
        }
    }

    /**
     * 根据bgId+centerId+deptName+centerName来查询
     */
    fun listByOrganization(
        dslContext: DSLContext,
        bgId: Long? = null,
        deptId: Long? = null,
        centerId: Long? = null,
        bgName: String? = null,
        deptName: String? = null,
        centerName: String? = null,
        enabled: Boolean?
    ): Result<TProjectRecord>? {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()

            bgId?.let { conditions.add(BG_ID.eq(bgId)) }
            deptId?.let { conditions.add(DEPT_ID.eq(deptId)) }
            centerId?.let { conditions.add(CENTER_ID.eq(centerId)) }

            if (!bgName.isNullOrBlank()) conditions.add(BG_NAME.like("%${URLDecoder.decode(bgName, "UTF-8")}%"))
            if (!deptName.isNullOrBlank()) conditions.add(DEPT_NAME.like("%${URLDecoder.decode(deptName, "UTF-8")}%"))
            if (!centerName.isNullOrBlank()) {
                conditions.add(CENTER_NAME.like("%${URLDecoder.decode(centerName, "UTF-8")}%"))
            }
            enabled?.let { conditions.add(ENABLED.eq(enabled)) }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun getByEnglishName(dslContext: DSLContext, englishName: String): TProjectRecord? {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(ENGLISH_NAME.eq(englishName)).fetchAny()
        }
    }

    fun getByCnName(dslContext: DSLContext, projectName: String): TProjectRecord? {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(PROJECT_NAME.eq(projectName)).fetchAny()
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

    fun delete(dslContext: DSLContext, projectId: String): Int {
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
        channelCode: ProjectChannelCode? = ProjectChannelCode.BS
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
                channelCode!!.name,
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

    fun updateProjectName(dslContext: DSLContext, projectCode: String, projectName: String): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(PROJECT_NAME, projectName)
                .where(ENGLISH_NAME.eq(projectCode))
                .execute()
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
        routerTag: String?,
        otherRouterTagMaps: Map<String, String>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (!projectName.isNullOrBlank()) {
            conditions.add(PROJECT_NAME.like("%${URLDecoder.decode(projectName, "UTF-8")}%"))
        }
        if (!englishName.isNullOrBlank()) {
            conditions.add(ENGLISH_NAME.like("%${URLDecoder.decode(englishName, "UTF-8")}%"))
        }
        projectType?.let { conditions.add(PROJECT_TYPE.eq(projectType)) }
        isSecrecy?.let { conditions.add(IS_SECRECY.eq(isSecrecy)) }
        if (!creator.isNullOrBlank()) conditions.add(CREATOR.eq(creator))
        if (!approver.isNullOrBlank()) conditions.add(APPROVER.eq(approver))
        approvalStatus?.let { conditions.add(APPROVAL_STATUS.eq(approvalStatus)) }

        if (!routerTag.isNullOrBlank()) conditions.add(ROUTER_TAG.eq(routerTag))

        if (!otherRouterTagMaps.isNullOrEmpty()) {
            otherRouterTagMaps.forEach { (jk, jv) ->
                conditions.add(JooqUtils.jsonExtract(OTHER_ROUTER_TAGS, "\$.$jk").eq(jv))
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
        routerTag: String? = null,
        otherRouterTagMaps: Map<String, String>? = null
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
                routerTag = routerTag,
                otherRouterTagMaps = otherRouterTagMaps
            )
            return dslContext.selectFrom(this).where(conditions).orderBy(CREATED_AT.desc()).limit(offset, limit).fetch()
        }
    }

    fun listByEnglishName(
        dslContext: DSLContext,
        englishNameList: List<String>,
        offset: Int? = null,
        limit: Int? = null,
        searchName: String? = null,
        enabled: Boolean? = null
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(APPROVAL_STATUS.eq(2))
                .and(ENGLISH_NAME.`in`(englishNameList))
                .and(IS_OFFLINED.eq(false))
                .let { if (null == searchName) it else it.and(PROJECT_NAME.like("%$searchName%")) }
                .let { if (null == enabled) it else it.and(ENABLED.eq(enabled)) }
                .let { if (null == offset || null == limit) it else it.limit(offset, limit) }
                .fetch()
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
        routerTag: String? = null,
        otherRouterTagMaps: Map<String, String>? = null
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
                routerTag = routerTag,
                otherRouterTagMaps = otherRouterTagMaps
            )
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
        }
    }

    fun countByEnglishName(
        dslContext: DSLContext,
        englishNameList: List<String>,
        searchName: String? = null
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.selectCount().from(this)
                .where(APPROVAL_STATUS.eq(2))
                .and(ENGLISH_NAME.`in`(englishNameList))
                .and(IS_OFFLINED.eq(false))
                .let { if (null == searchName) it else it.and(PROJECT_NAME.like("%$searchName%")) }
                .fetchOne()!!.value1()
        }
    }

    fun getMinId(dslContext: DSLContext): Long {
        with(TProject.T_PROJECT) {
            return dslContext.select(DSL.min(ID)).from(this).fetchOne(0, Long::class.java)!!
        }
    }

    fun getMaxId(dslContext: DSLContext): Long {
        with(TProject.T_PROJECT) {
            return dslContext.select(DSL.max(ID)).from(this).fetchOne(0, Long::class.java)!!
        }
    }

    fun getProjectListById(
        dslContext: DSLContext,
        minId: Long,
        maxId: Long
    ): Result<out Record>? {
        with(TProject.T_PROJECT) {
            return dslContext.select(ID.`as`("ID"), ENGLISH_NAME.`as`("ENGLISH_NAME"))
                .from(this)
                .where(ID.ge(minId).and(ID.le(maxId)))
                .fetch()
        }
    }

    fun searchByProjectName(
        dslContext: DSLContext,
        projectName: String,
        limit: Int,
        offset: Int
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_NAME.like("%$projectName%"))
                .limit(limit).offset(offset).fetch()
        }
    }

    fun countByProjectName(dslContext: DSLContext, projectName: String): Int {
        with(TProject.T_PROJECT) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_NAME.like("%$projectName%"))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun updateRelationByCode(dslContext: DSLContext, projectCode: String, relationId: String): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(RELATION_ID, relationId).where(ENGLISH_NAME.eq(projectCode))
                .execute()
        }
    }

    fun listSecrecyProject(dslContext: DSLContext): Result<Record1<String>>? {
        with(TProject.T_PROJECT) {
            return dslContext.select(ENGLISH_NAME)
                .from(this)
                .where(IS_SECRECY.eq(true))
                .fetch()
        }
    }

    fun getProjectByName(dslContext: DSLContext, projectName: String): ProjectVO? {
        with(TProject.T_PROJECT) {
            val record = dslContext.selectFrom(this).where(PROJECT_NAME.eq(projectName)).fetchAny()
                ?: return null
            return ProjectUtils.packagingBean(record)
        }
    }

    fun listProjectEnglishNameByRouteTag(dslContext: DSLContext, routeTag: String): List<String> {
        with(TProject.T_PROJECT) {
            return dslContext.select(ENGLISH_NAME)
                .from(this)
                .where(ROUTER_TAG.eq(routeTag))
                .fetch(ENGLISH_NAME, String::class.java)
        }
    }
}
