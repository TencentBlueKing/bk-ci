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

package com.tencent.devops.project.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.auth.enums.AuthSystemType
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.PaasProject
import com.tencent.devops.project.pojo.ProjectCollation
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectSortType
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectAuthSecrecyStatus
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.util.ProjectUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Record3
import org.jooq.Record4
import org.jooq.Result
import org.jooq.impl.DSL
import org.jooq.impl.DSL.lower
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime
import java.util.Locale

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
            dslContext.select(ENGLISH_NAME)
                .from(this)
                .where(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .fetch(ENGLISH_NAME, String::class.java)
        }
    }

    fun list(
        dslContext: DSLContext,
        englishNameList: Set<String>,
        enabled: Boolean? = null,
        routerTag: String? = null
    ): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this)
                .where(ENGLISH_NAME.`in`(englishNameList))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .let { if (enabled != null) it.and(ENABLED.eq(enabled)) else it }
                .let { if (routerTag != null) it.and(ROUTER_TAG.notLike("%$routerTag%").or(ROUTER_TAG.isNull)) else it }
                .fetch()
        }
    }

    fun getAllProject(dslContext: DSLContext): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this)
                .where(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .and(AUTH_SECRECY.eq(ProjectAuthSecrecyStatus.PUBLIC.value))
                .fetch()
        }
    }

    fun list(dslContext: DSLContext, limit: Int, offset: Int): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this)
                .where(ENABLED.eq(true))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .limit(limit).offset(offset).fetch()
        }
    }

    fun listProjectsByCondition(
        dslContext: DSLContext,
        projectConditionDTO: ProjectConditionDTO,
        limit: Int,
        offset: Int
    ): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this)
                .where(buildProjectCondition(projectConditionDTO))
                .orderBy(CREATED_AT.asc())
                .limit(limit)
                .offset(offset)
                .fetch()
        }
    }

    fun buildProjectCondition(
        projectConditionDTO: ProjectConditionDTO
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        with(TProject.T_PROJECT) {
            with(projectConditionDTO) {
                conditions.add(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                if (channelCode == null) {
                    conditions.add(
                        CHANNEL.eq(ProjectChannelCode.BS.name).or(CHANNEL.eq(ProjectChannelCode.PREBUILD.name))
                    )
                } else {
                    conditions.add(CHANNEL.eq(channelCode))
                }
                enabled?.let { conditions.add(ENABLED.eq(enabled)) }
                centerId?.let { conditions.add(CENTER_ID.eq(centerId)) }
                deptId?.let { conditions.add(DEPT_ID.eq(deptId)) }
                businessLineId?.let { conditions.add(BUSINESS_LINE_ID.eq(businessLineId)) }
                bgId?.let { conditions.add(BG_ID.eq(bgId)) }
                projectCreator?.let { conditions.add(CREATOR.eq(projectCreator)) }
                excludedProjectCodes?.let { conditions.add(ENGLISH_NAME.notIn(excludedProjectCodes)) }
                projectCodes?.let { conditions.add(ENGLISH_NAME.`in`(projectCodes)) }
                excludedCreateTime?.let {
                    val data = DateTimeUtil.stringToLocalDate(excludedCreateTime)!!.atStartOfDay()
                    conditions.add(CREATED_AT.lessThan(data))
                }
                bgIdList?.let {
                    conditions.add(BG_ID.`in`(bgIdList))
                }
                relatedProduct?.let {
                    when (relatedProduct) {
                        true -> conditions.add(PRODUCT_ID.isNotNull)
                        false -> conditions.add(PRODUCT_ID.isNull)
                        else -> {}
                    }
                }
                routerTag?.let {
                    if (routerTag == AuthSystemType.RBAC_AUTH_TYPE) {
                        conditions.add(
                            ROUTER_TAG.like("%${projectConditionDTO.routerTag!!.value}%")
                                .or(ROUTER_TAG.like("%devx%"))
                                .let { if (includeNullRouterTag == true) it.or(ROUTER_TAG.isNull()) else it }
                        )
                    } else {
                        conditions.add(
                            ROUTER_TAG.like("%${projectConditionDTO.routerTag!!.value}%")
                        )
                    }
                }
                if (queryRemoteDevFlag == true) {
                    conditions.add(JooqUtils.jsonExtractAny<Boolean>(PROPERTIES, "\$.remotedev").isTrue)
                }
                if (!projectName.isNullOrBlank()) {
                    conditions.add(PROJECT_NAME.like("%$projectName%"))
                }
                if (!englishName.isNullOrBlank()) {
                    conditions.add(ENGLISH_NAME.like("%$englishName%"))
                }
            }
        }
        return conditions
    }

    fun listByChannel(
        dslContext: DSLContext,
        limit: Int,
        offset: Int,
        channelCodes: List<String>
    ): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this)
                .where(ENABLED.eq(true).and(CHANNEL.`in`(channelCodes)))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .and(AUTH_SECRECY.eq(ProjectAuthSecrecyStatus.PUBLIC.value))
                .limit(limit).offset(offset).fetch()
        }
    }

    fun getCount(dslContext: DSLContext): Long {
        return with(TProject.T_PROJECT) {
            dslContext.selectCount().from(this).where(ENABLED.eq(true))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .and(AUTH_SECRECY.eq(ProjectAuthSecrecyStatus.PUBLIC.value))
                .fetchOne(0, Long::class.java)!!
        }
    }

    /**
     * 根据英文名称(projectCode)查询name
     */
    fun listByCodes(
        dslContext: DSLContext,
        projectCodeList: Set<String>,
        enabled: Boolean?
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(ENGLISH_NAME.`in`(projectCodeList))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .let { if (null == enabled) it else it.and(ENABLED.eq(enabled)) }
                .orderBy(PROJECT_NAME.asc())
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
            conditions.add(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
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
                    ProjectApproveStatus.APPROVED.status,
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
        logoAddress: String?,
        projectCreateInfo: ProjectCreateInfo,
        userDeptDetail: UserDeptDetail,
        projectId: String,
        channelCode: ProjectChannelCode? = ProjectChannelCode.BS,
        approvalStatus: Int,
        subjectScopesStr: String,
        properties: ProjectProperties?
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
                BUSINESS_LINE_ID,
                BUSINESS_LINE_NAME,
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
                ENABLED,
                PROPERTIES,
                SUBJECT_SCOPES,
                AUTH_SECRECY,
                PRODUCT_ID
            ).values(
                projectCreateInfo.projectName,
                projectId,
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
                projectCreateInfo.secrecy,
                projectCreateInfo.kind,
                userId,
                LocalDateTime.now(),
                projectCreateInfo.projectType,
                approvalStatus,
                logoAddress ?: "",
                userDeptDetail.bgName,
                userDeptDetail.deptName,
                userDeptDetail.centerName,
                channelCode!!.name,
                projectCreateInfo.enabled,
                properties?.let {
                    JsonUtil.toJson(it, false)
                },
                subjectScopesStr,
                projectCreateInfo.authSecrecy ?: ProjectAuthSecrecyStatus.PUBLIC.value,
                projectCreateInfo.productId
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        projectUpdateInfo: ProjectUpdateInfo,
        subjectScopesStr: String,
        logoAddress: String?,
        approvalStatus: Int = ProjectApproveStatus.APPROVED.status
    ): Int {
        with(TProject.T_PROJECT) {
            val update = dslContext.update(this)
                .set(PROJECT_NAME, projectUpdateInfo.projectName)
                .set(BG_ID, projectUpdateInfo.bgId)
                .set(BG_NAME, projectUpdateInfo.bgName)
                .set(BUSINESS_LINE_ID, projectUpdateInfo.businessLineId)
                .set(BUSINESS_LINE_NAME, projectUpdateInfo.businessLineName)
                .set(CENTER_ID, projectUpdateInfo.centerId)
                .set(CENTER_NAME, projectUpdateInfo.centerName)
                .set(DEPT_ID, projectUpdateInfo.deptId)
                .set(DEPT_NAME, projectUpdateInfo.deptName)
                .set(DESCRIPTION, projectUpdateInfo.description)
                .set(ENGLISH_NAME, projectUpdateInfo.englishName)
                .set(UPDATED_AT, LocalDateTime.now())
                .set(UPDATOR, userId)
                .set(APPROVAL_STATUS, approvalStatus)
                .set(APPROVER, userId)
                .set(SUBJECT_SCOPES, subjectScopesStr)
                .set(PROJECT_TYPE, projectUpdateInfo.projectType)
                .set(PRODUCT_ID, projectUpdateInfo.productId)
            projectUpdateInfo.authSecrecy?.let { update.set(AUTH_SECRECY, it) }
            logoAddress?.let { update.set(LOGO_ADDR, logoAddress) }
            projectUpdateInfo.properties?.let { update.set(PROPERTIES, JsonUtil.toJson(it, false)) }
            return update.where(PROJECT_ID.eq(projectId)).execute()
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

    fun updateUsableStatus(
        dslContext: DSLContext,
        userId: String?,
        projectId: String,
        enabled: Boolean
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(ENABLED, enabled)
                .set(UPDATED_AT, LocalDateTime.now())
                .let { if (userId == null) it else it.set(UPDATOR, userId) }
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
        otherRouterTagMaps: Map<String, String>?,
        remoteDevFlag: Boolean?,
        productId: Int?,
        channelCode: String?
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
        productId?.let { conditions.add(PRODUCT_ID.eq(productId)) }
        if (!creator.isNullOrBlank()) conditions.add(CREATOR.eq(creator))
        if (!approver.isNullOrBlank()) conditions.add(APPROVER.eq(approver))
        approvalStatus?.let { conditions.add(APPROVAL_STATUS.eq(approvalStatus)) }

        if (!routerTag.isNullOrBlank()) conditions.add(ROUTER_TAG.eq(routerTag))

        if (!otherRouterTagMaps.isNullOrEmpty()) {
            otherRouterTagMaps.forEach { (jk, jv) ->
                conditions.add(JooqUtils.jsonExtract(OTHER_ROUTER_TAGS, "\$.$jk").eq(jv))
            }
        }

        if (remoteDevFlag != null && remoteDevFlag) {
            conditions.add(CHANNEL.eq(ProjectChannelCode.BS.name))
            conditions.add(JooqUtils.jsonExtractAny<Boolean>(PROPERTIES, "\$.remotedev").isTrue)
        }
        channelCode?.let { conditions.add(CHANNEL.eq(channelCode)) }
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
        otherRouterTagMaps: Map<String, String>? = null,
        remoteDevFlag: Boolean? = null,
        productId: Int? = null,
        channelCode: String? = null
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
                otherRouterTagMaps = otherRouterTagMaps,
                remoteDevFlag = remoteDevFlag,
                productId = productId,
                channelCode = channelCode
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
        enabled: Boolean? = null,
        authSecrecyStatus: ProjectAuthSecrecyStatus? = null,
        sortType: ProjectSortType? = null,
        collation: ProjectCollation? = ProjectCollation.DEFAULT,
        productIds: Set<Int> = setOf(),
        channelCodes: Set<String> = setOf()
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .and(ENGLISH_NAME.`in`(englishNameList))
                .and(IS_OFFLINED.eq(false))
                .let { if (null == searchName) it else it.and(PROJECT_NAME.like("%$searchName%")) }
                .let { if (null == enabled) it else it.and(ENABLED.eq(enabled)) }
                .let { if (null == authSecrecyStatus) it else it.and(AUTH_SECRECY.eq(authSecrecyStatus.value)) }
                .let { if (productIds.isEmpty()) it else it.and(PRODUCT_ID.`in`(productIds)) }
                .let { if (channelCodes.isEmpty()) it else it.and(CHANNEL.`in`(channelCodes)) }
                .let {
                    if (sortType != null) {
                        when (sortType) {
                            ProjectSortType.PROJECT_NAME -> {
                                if (collation == ProjectCollation.DEFAULT || collation == ProjectCollation.ASC) {
                                    it.orderBy(DSL.field("CONVERT({0} USING GBK)", PROJECT_NAME).asc())
                                } else {
                                    it.orderBy(DSL.field("CONVERT({0} USING GBK)", PROJECT_NAME).desc())
                                }
                            }

                            ProjectSortType.ENGLISH_NAME -> {
                                if (collation == ProjectCollation.DEFAULT || collation == ProjectCollation.ASC) {
                                    it.orderBy(ENGLISH_NAME.asc())
                                } else {
                                    it.orderBy(ENGLISH_NAME.desc())
                                }
                            }

                            else -> {
                                it
                            }
                        }
                    } else {
                        it
                    }
                }
                .let { if (null == offset || null == limit) it else it.limit(offset, limit) }
                .fetch()
        }
    }

    // 拉取用户未审核通过的项目，即未在iam注册的项目
    fun listUnApprovedByUserId(
        dslContext: DSLContext,
        userId: String
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(
                    APPROVAL_STATUS.`in`(
                        listOf(
                            ProjectApproveStatus.CREATE_REJECT.status,
                            ProjectApproveStatus.CREATE_PENDING.status
                        )
                    )
                ).and(CREATOR.eq(userId))
                .fetch()
        }
    }

    fun listProjectsForApply(
        dslContext: DSLContext,
        projectName: String?,
        projectId: String?,
        authEnglishNameList: List<String>,
        offset: Int,
        limit: Int
    ): Result<Record3<String, String, String>> {
        return with(TProject.T_PROJECT) {
            dslContext.select(PROJECT_NAME, ENGLISH_NAME, ROUTER_TAG).from(this)
                .where(generateQueryProjectForApplyCondition())
                .and(AUTH_SECRECY.eq(ProjectAuthSecrecyStatus.PUBLIC.value))
                .or(
                    ID.`in`(
                        dslContext.select(ID).from(this)
                            .where(generateQueryProjectForApplyCondition())
                            .and(ENGLISH_NAME.`in`(authEnglishNameList))
                            .and(AUTH_SECRECY.eq(ProjectAuthSecrecyStatus.PRIVATE.value))
                    )
                )
                .let {
                    it.takeIf { projectName != null }?.and(
                        lower(PROJECT_NAME).like("%${projectName!!.trim().lowercase(Locale.getDefault())}%")
                    ) ?: it
                }
                .let { it.takeIf { projectId != null }?.and(ENGLISH_NAME.eq(projectId)) ?: it }
                .and(CHANNEL.eq(ProjectChannelCode.BS.name).or(CHANNEL.eq(ProjectChannelCode.PREBUILD.name)))
                .orderBy(CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetch()
        }
    }

    private fun TProject.generateQueryProjectForApplyCondition(): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(CHANNEL.eq(ProjectChannelCode.BS.name).or(CHANNEL.eq(ProjectChannelCode.PREBUILD.name)))
        conditions.add(IS_OFFLINED.eq(false))
        conditions.add(ENABLED.eq(true))
        conditions.add(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
        return conditions
    }

    fun updateProjectFromOp(dslContext: DSLContext, projectInfoRequest: OpProjectUpdateInfoRequest) {
        with(TProject.T_PROJECT) {
            val step = dslContext.update(this)
                .set(PROJECT_NAME, projectInfoRequest.projectName)
                .set(BG_ID, projectInfoRequest.bgId)
                .set(BG_NAME, projectInfoRequest.bgName)
                .set(BUSINESS_LINE_ID, projectInfoRequest.businessLineId)
                .set(BUSINESS_LINE_NAME, projectInfoRequest.businessLineName)
                .set(DEPT_ID, projectInfoRequest.deptId)
                .set(DEPT_NAME, projectInfoRequest.deptName)
                .set(CENTER_ID, projectInfoRequest.centerId)
                .set(CENTER_NAME, projectInfoRequest.centerName)
                .set(PROJECT_TYPE, projectInfoRequest.projectType)
                .set(IS_SECRECY, projectInfoRequest.secrecyFlag)
                .set(APPROVAL_STATUS, projectInfoRequest.approvalStatus)
                .set(APPROVAL_TIME, projectInfoRequest.approvalTime?.let { java.sql.Timestamp(it).toLocalDateTime() })
                .set(USE_BK, projectInfoRequest.useBk)
                .set(CC_APP_ID, projectInfoRequest.ccAppId)
                .set(CC_APP_NAME, projectInfoRequest.cc_app_name ?: "")
                .set(UPDATED_AT, LocalDateTime.now())
                .set(KIND, projectInfoRequest.kind)
                .set(ENABLED, projectInfoRequest.enabled)
                .set(PIPELINE_LIMIT, projectInfoRequest.pipelineLimit)
                .set(PROPERTIES, projectInfoRequest.properties?.let { JsonUtil.toJson(it, false) })
                .set(PRODUCT_ID, projectInfoRequest.productId)

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
        otherRouterTagMaps: Map<String, String>? = null,
        remoteDevFlag: Boolean? = null,
        productId: Int? = null,
        channelCode: String? = null
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
                otherRouterTagMaps = otherRouterTagMaps,
                remoteDevFlag = remoteDevFlag,
                productId = productId,
                channelCode = channelCode
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
                .where(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .and(ENGLISH_NAME.`in`(englishNameList))
                .and(IS_OFFLINED.eq(false))
                .let { if (null == searchName) it else it.and(PROJECT_NAME.like("%$searchName%")) }
                .fetchOne()!!.value1()
        }
    }

    fun getMinId(dslContext: DSLContext): Long {
        with(TProject.T_PROJECT) {
            return dslContext.select(DSL.min(ID)).from(this)
                .where(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getMaxId(dslContext: DSLContext): Long {
        with(TProject.T_PROJECT) {
            return dslContext.select(DSL.max(ID)).from(this)
                .where(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .fetchOne(0, Long::class.java)!!
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
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .fetch()
        }
    }

    fun getProjectListByProductId(
        dslContext: DSLContext,
        productId: Int
    ): Result<Record4<Long, String, String, Boolean>> {
        return with(TProject.T_PROJECT) {
            dslContext.select(ID, ENGLISH_NAME, PROJECT_NAME, ENABLED).from(this)
                .where(PRODUCT_ID.eq(productId)).fetch()
        }
    }

    fun searchByProjectName(
        dslContext: DSLContext,
        projectName: String,
        channelCodes: List<String>,
        limit: Int,
        offset: Int
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_NAME.like("%$projectName%"))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .and(AUTH_SECRECY.eq(ProjectAuthSecrecyStatus.PUBLIC.value))
                .and(CHANNEL.`in`(channelCodes))
                .limit(limit).offset(offset).fetch()
        }
    }

    fun countByProjectName(
        dslContext: DSLContext,
        projectName: String,
        channelCodes: List<String>
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_NAME.like("%$projectName%"))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .and(AUTH_SECRECY.eq(ProjectAuthSecrecyStatus.PUBLIC.value))
                .and(CHANNEL.`in`(channelCodes))
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

    fun updateSubjectScopesByCode(dslContext: DSLContext, projectCode: String, SubjectScopesStr: String): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(SUBJECT_SCOPES, SubjectScopesStr).where(ENGLISH_NAME.eq(projectCode))
                .execute()
        }
    }

    fun updateCreatorByCode(dslContext: DSLContext, projectCode: String, creator: String): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(CREATOR, creator).where(ENGLISH_NAME.eq(projectCode))
                .execute()
        }
    }

    fun updatePropertiesByCode(dslContext: DSLContext, projectCode: String, properties: ProjectProperties): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(PROPERTIES, JsonUtil.toJson(properties, false))
                .where(ENGLISH_NAME.eq(projectCode))
                .execute()
        }
    }

    fun listSecrecyProject(dslContext: DSLContext): Result<Record1<String>>? {
        with(TProject.T_PROJECT) {
            return dslContext.select(ENGLISH_NAME)
                .from(this)
                .where(IS_SECRECY.eq(true))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .fetch()
        }
    }

    fun getProjectByName(dslContext: DSLContext, projectName: String): ProjectVO? {
        with(TProject.T_PROJECT) {
            val record = dslContext.selectFrom(this)
                .where(PROJECT_NAME.eq(projectName))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .fetchAny()
                ?: return null
            return ProjectUtils.packagingBean(record)
        }
    }

    fun listProjectEnglishNameByRouteTag(dslContext: DSLContext, routeTag: String): List<String> {
        with(TProject.T_PROJECT) {
            return dslContext.select(ENGLISH_NAME)
                .from(this)
                .where(ROUTER_TAG.eq(routeTag))
                .and(APPROVAL_STATUS.notIn(UNSUCCESSFUL_CREATE_STATUS))
                .fetch(ENGLISH_NAME, String::class.java)
        }
    }

    fun updateProjectStatusByEnglishName(
        dslContext: DSLContext,
        userId: String,
        englishName: String,
        approvalStatus: Int
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(APPROVAL_STATUS, approvalStatus)
                .set(UPDATOR, userId)
                .set(UPDATED_AT, LocalDateTime.now())
                .where(ENGLISH_NAME.eq(englishName))
                .execute()
        }
    }

    fun updateApprovalStatus(
        dslContext: DSLContext,
        englishName: String,
        approver: String,
        approvalStatus: Int
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(APPROVAL_STATUS, approvalStatus)
                .set(APPROVER, approver)
                .set(APPROVAL_TIME, LocalDateTime.now())
                .where(ENGLISH_NAME.eq(englishName))
                .execute()
        }
    }

    fun updateAuthProjectId(
        dslContext: DSLContext,
        englishName: String,
        projectId: String
    ) {
        with(TProject.T_PROJECT) {
            dslContext.update(this)
                .set(PROJECT_ID, projectId)
                .where(ENGLISH_NAME.eq(englishName))
                .execute()
        }
    }

    fun updateSubjectScopes(
        dslContext: DSLContext,
        englishName: String,
        subjectScopesStr: String
    ) {
        with(TProject.T_PROJECT) {
            dslContext.update(this)
                .set(SUBJECT_SCOPES, subjectScopesStr)
                .where(ENGLISH_NAME.eq(englishName))
                .execute()
        }
    }

    fun updateProductId(
        dslContext: DSLContext,
        englishName: String,
        productId: Int
    ) {
        with(TProject.T_PROJECT) {
            dslContext.update(this)
                .set(PRODUCT_ID, productId)
                .where(ENGLISH_NAME.eq(englishName))
                .execute()
        }
    }

    fun batchUpdateProductId(
        dslContext: DSLContext,
        englishNames: List<String>,
        productId: Int
    ) {
        with(TProject.T_PROJECT) {
            dslContext.update(this)
                .set(PRODUCT_ID, productId)
                .where(ENGLISH_NAME.`in`(englishNames))
                .execute()
        }
    }

    fun updateOrganizationByEnglishName(
        dslContext: DSLContext,
        englishName: String,
        projectOrganizationInfo: ProjectOrganizationInfo
    ) {
        with(TProject.T_PROJECT) {
            dslContext.update(this)
                .set(BG_ID, projectOrganizationInfo.bgId)
                .set(BG_NAME, projectOrganizationInfo.bgName)
                .set(BUSINESS_LINE_ID, projectOrganizationInfo.businessLineId)
                .set(BUSINESS_LINE_NAME, projectOrganizationInfo.businessLineName)
                .set(DEPT_ID, projectOrganizationInfo.deptId)
                .set(DEPT_NAME, projectOrganizationInfo.deptName)
                .set(CENTER_ID, projectOrganizationInfo.centerId)
                .set(CENTER_NAME, projectOrganizationInfo.centerName)
                .where(ENGLISH_NAME.eq(englishName))
                .execute()
        }
    }

    fun getExistedEnglishName(dslContext: DSLContext, englishNameList: List<String>): List<String>? {
        with(TProject.T_PROJECT) {
            return dslContext.select(ENGLISH_NAME)
                .from(this)
                .where(ENGLISH_NAME.`in`(englishNameList))
                .fetch(ENGLISH_NAME, String::class.java)
        }
    }

    companion object {
        private val UNSUCCESSFUL_CREATE_STATUS = listOf(
            ProjectApproveStatus.CREATE_PENDING.status,
            ProjectApproveStatus.CREATE_REJECT.status
        )
    }
}
