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

package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.ProjectMembersQueryConditionDTO
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.auth.tables.TAuthResourceAuthorization
import com.tencent.devops.model.auth.tables.TAuthResourceGroupMember
import com.tencent.devops.model.auth.tables.TUserInfo
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupMemberRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.countDistinct
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.inline
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("LongParameterList")
class AuthResourceGroupMemberDao {
    fun create(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
        iamGroupId: Int,
        memberId: String,
        memberName: String,
        memberType: String,
        expiredTime: LocalDateTime
    ) {
        val now = LocalDateTime.now()
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.insertInto(
                this,
                PROJECT_CODE,
                RESOURCE_TYPE,
                RESOURCE_CODE,
                GROUP_CODE,
                IAM_GROUP_ID,
                MEMBER_ID,
                MEMBER_NAME,
                MEMBER_TYPE,
                EXPIRED_TIME,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectCode,
                resourceType,
                resourceCode,
                groupCode,
                iamGroupId,
                memberId,
                memberName,
                memberType,
                expiredTime,
                now,
                now
            ).onDuplicateKeyUpdate()
                .set(MEMBER_NAME, memberName)
                .set(EXPIRED_TIME, expiredTime)
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int,
        memberId: String,
        memberName: String? = null,
        expiredTime: LocalDateTime
    ) {
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.update(this)
                .let { if (memberName != null) it.set(MEMBER_NAME, memberName) else it }
                .set(EXPIRED_TIME, expiredTime)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.eq(iamGroupId))
                .and(MEMBER_ID.eq(memberId))
                .execute()
        }
    }

    fun batchCreate(dslContext: DSLContext, groupMembers: List<AuthResourceGroupMember>) {
        val now = LocalDateTime.now()
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            groupMembers.forEach {
                dslContext.insertInto(
                    this,
                    PROJECT_CODE,
                    RESOURCE_TYPE,
                    RESOURCE_CODE,
                    GROUP_CODE,
                    IAM_GROUP_ID,
                    MEMBER_ID,
                    MEMBER_NAME,
                    MEMBER_TYPE,
                    EXPIRED_TIME,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    it.projectCode,
                    it.resourceType,
                    it.resourceCode,
                    it.groupCode,
                    it.iamGroupId,
                    it.memberId,
                    it.memberName,
                    it.memberType,
                    it.expiredTime,
                    now,
                    now
                ).onDuplicateKeyUpdate()
                    .set(MEMBER_NAME, it.memberName)
                    .set(EXPIRED_TIME, it.expiredTime)
                    .execute()
            }
        }
    }

    fun batchUpdate(dslContext: DSLContext, groupMembers: List<AuthResourceGroupMember>) {
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            groupMembers.forEach {
                dslContext.update(this)
                    .set(MEMBER_NAME, it.memberName)
                    .set(EXPIRED_TIME, it.expiredTime)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(PROJECT_CODE.eq(it.projectCode))
                    .and(IAM_GROUP_ID.eq(it.iamGroupId))
                    .and(MEMBER_ID.eq(it.memberId))
                    .execute()
            }
        }
    }

    fun batchDelete(dslContext: DSLContext, ids: Set<Long>) {
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.delete(this)
                .where(ID.`in`(ids))
                .execute()
        }
    }

    fun batchDeleteGroupMembers(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int,
        memberIds: List<String>
    ) {
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.delete(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.eq(iamGroupId))
                .and(MEMBER_ID.`in`(memberIds))
                .execute()
        }
    }

    fun deleteByIamGroupId(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int
    ) {
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.delete(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.eq(iamGroupId))
                .execute()
        }
    }

    fun deleteByIamGroupIds(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupIds: List<Int>
    ) {
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.delete(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.`in`(iamGroupIds))
                .execute()
        }
    }

    fun deleteByResource(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ) {
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.delete(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(RESOURCE_CODE.eq(resourceCode))
                .execute()
        }
    }

    fun isMemberInGroup(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int,
        memberId: String
    ): Boolean {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.selectCount()
                .from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.eq(iamGroupId))
                .and(MEMBER_ID.eq(memberId))
                .fetchOne(0, Int::class.java) != 0
        }
    }

    fun isMembersInProject(
        dslContext: DSLContext,
        projectCode: String,
        memberNames: List<String>,
        memberType: String
    ): List<ResourceMemberInfo> {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.select(MEMBER_ID, MEMBER_NAME, MEMBER_TYPE)
                .from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(MEMBER_NAME.`in`(memberNames))
                .and(MEMBER_TYPE.eq(memberType))
                .groupBy(MEMBER_NAME, MEMBER_ID, MEMBER_TYPE)
                .fetch().map {
                    ResourceMemberInfo(
                        id = it.value1(),
                        name = it.value2(),
                        type = it.value3()
                    )
                }
        }
    }

    fun isMemberInProject(
        dslContext: DSLContext,
        projectCode: String,
        userId: String,
        iamTemplateIds: List<String>,
        memberDeptInfos: List<String>?
    ): Boolean {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.selectCount()
                .from(this)
                .where(
                    buildMemberGroupCondition(
                        projectCode = projectCode,
                        memberId = userId,
                        iamTemplateIds = iamTemplateIds,
                        memberDeptInfos = memberDeptInfos,
                        minExpiredAt = LocalDateTime.now()
                    )
                )
                .fetchOne(0, Int::class.java)!! > 0
        }
    }

    fun handoverGroupMembers(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int,
        handoverFrom: ResourceMemberInfo,
        handoverTo: ResourceMemberInfo,
        expiredTime: LocalDateTime
    ) {
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.update(this)
                .set(MEMBER_ID, handoverTo.id)
                .set(MEMBER_NAME, handoverTo.name)
                .set(MEMBER_TYPE, handoverTo.type)
                .set(EXPIRED_TIME, expiredTime)
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.eq(iamGroupId))
                .and(MEMBER_ID.eq(handoverFrom.id))
                .execute()
        }
    }

    fun listResourceGroupMember(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String? = null,
        resourceCode: String? = null,
        excludeResourceType: String? = null,
        memberId: String? = null,
        memberIds: List<String>? = null,
        memberName: String? = null,
        memberType: String? = null,
        iamGroupId: Int? = null,
        iamGroupIds: List<Int>? = null,
        maxExpiredTime: LocalDateTime? = null,
        minExpiredTime: LocalDateTime? = null,
        groupCode: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<AuthResourceGroupMember> {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
            resourceType?.let { select.and(RESOURCE_TYPE.eq(resourceType)) }
            excludeResourceType?.let { select.and(RESOURCE_TYPE.notEqual(excludeResourceType)) }
            memberId?.let { select.and(MEMBER_ID.eq(memberId)) }
            if (!memberIds.isNullOrEmpty()) {
                select.and(MEMBER_ID.`in`(memberIds))
            }
            if (!iamGroupIds.isNullOrEmpty()) {
                select.and(IAM_GROUP_ID.`in`(iamGroupIds))
            }
            memberName?.let { select.and(MEMBER_NAME.eq(memberName)) }
            memberType?.let { select.and(MEMBER_TYPE.eq(memberType)) }
            iamGroupId?.let { select.and(IAM_GROUP_ID.eq(iamGroupId)) }
            maxExpiredTime?.let { select.and(EXPIRED_TIME.le(maxExpiredTime)) }
            minExpiredTime?.let { select.and(EXPIRED_TIME.ge(minExpiredTime)) }
            resourceCode?.let { select.and(RESOURCE_CODE.eq(resourceCode)) }
            groupCode?.let { select.and(GROUP_CODE.eq(groupCode)) }
            select.let {
                if (limit != null && offset != null) {
                    it.limit(limit).offset(offset)
                } else {
                    it
                }
            }.fetch().map { convert(it) }
        }
    }

    fun listProjectGroups(
        dslContext: DSLContext,
        projectCode: String,
        offset: Int,
        limit: Int
    ): List<Int> {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.select(IAM_GROUP_ID).from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .groupBy(IAM_GROUP_ID)
                .orderBy(CREATE_TIME.desc())
                .offset(offset).limit(limit)
                .fetch().map { it.value1() }
        }
    }

    fun listProjectMembers(
        dslContext: DSLContext,
        projectCode: String,
        memberType: String?,
        userName: String?,
        deptName: String?,
        offset: Int?,
        limit: Int?
    ): List<ResourceMemberInfo> {
        val tUserInfo = TUserInfo.T_USER_INFO

        val resourceMemberUnionAuthorizationMember = createResourceMemberUnionAuthorizationMember(
            dslContext = dslContext,
            projectCode = projectCode
        )

        val memberIdField = field(MEMBER_ID, String::class.java)
        val memberNameField = field(MEMBER_NAME, String::class.java)
        val memberTypeField = field(MEMBER_TYPE, String::class.java)

        return dslContext
            .select(
                memberIdField,
                memberNameField,
                memberTypeField,
                DSL.`when`(memberTypeField.eq(MemberType.DEPARTMENT.type), false)
                    .otherwise(coalesce(tUserInfo.DEPARTED, true))
                    .`as`(IS_DEPARTED)
            )
            .from(resourceMemberUnionAuthorizationMember)
            .leftJoin(tUserInfo).on(memberIdField.eq(tUserInfo.USER_ID))
            .where(
                buildResourceMemberConditions(
                    memberType = memberType,
                    userName = userName,
                    deptName = deptName
                )
            )
            .groupBy(memberIdField, memberNameField, memberTypeField)
            // 排序逻辑：离职的在前，然后按ID排序
            .orderBy(field(IS_DEPARTED).desc(), memberIdField.asc())
            .let {
                if (offset != null && limit != null) {
                    it.offset(offset).limit(limit)
                } else {
                    it
                }
            }
            .skipCheck()
            .fetch().map {
                ResourceMemberInfo(
                    id = it.value1(),
                    name = it.value2(),
                    type = it.value3(),
                    departed = it.value4()
                )
            }
    }

    fun listProjectMembersByComplexConditions(
        dslContext: DSLContext,
        conditionDTO: ProjectMembersQueryConditionDTO
    ): List<ResourceMemberInfo> {
        val tUserInfo = TUserInfo.T_USER_INFO

        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.select(
                MEMBER_ID,
                MEMBER_NAME,
                MEMBER_TYPE,
                DSL.`when`(MEMBER_TYPE.eq(MemberType.DEPARTMENT.type), false)
                    .otherwise(coalesce(tUserInfo.DEPARTED, true))
                    .`as`(IS_DEPARTED)
            ).from(this)
                .leftJoin(tUserInfo).on(MEMBER_ID.eq(tUserInfo.USER_ID))
                .where(buildProjectMembersByComplexConditions(conditionDTO))
                .groupBy(MEMBER_ID, MEMBER_NAME, MEMBER_TYPE)
                .orderBy(DSL.field(IS_DEPARTED).desc(), MEMBER_ID)
                .let {
                    if (conditionDTO.limit != null && conditionDTO.offset != null) {
                        it.offset(conditionDTO.offset).limit(conditionDTO.limit)
                    } else {
                        it
                    }
                }
                .fetch().map {
                    ResourceMemberInfo(
                        id = it.value1(),
                        name = it.value2(),
                        type = it.value3(),
                        departed = it.value4()
                    )
                }
        }
    }

    fun countProjectMembersByComplexConditions(
        dslContext: DSLContext,
        conditionDTO: ProjectMembersQueryConditionDTO
    ): Long {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.select(countDistinct(MEMBER_ID)).from(this)
                .where(buildProjectMembersByComplexConditions(conditionDTO))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun buildProjectMembersByComplexConditions(
        projectMembersQueryConditionDTO: ProjectMembersQueryConditionDTO
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            with(projectMembersQueryConditionDTO) {
                conditions.add(PROJECT_CODE.eq(projectCode))
                if (queryTemplate == false) {
                    conditions.add(MEMBER_TYPE.notEqual(MemberType.TEMPLATE.type))
                } else {
                    conditions.add(MEMBER_TYPE.eq(MemberType.TEMPLATE.type))
                }
                memberType?.let { type -> conditions.add(MEMBER_TYPE.eq(type)) }
                userName?.let { name ->
                    conditions.add(MEMBER_TYPE.eq(MemberType.USER.type))
                    conditions.add(MEMBER_ID.like("%$name%").or(MEMBER_NAME.like("%$name%")))
                }
                deptName?.let { name ->
                    conditions.add(MEMBER_TYPE.eq(MemberType.DEPARTMENT.type))
                    conditions.add(MEMBER_NAME.like("%$name%"))
                }
                minExpiredTime?.let { minTime -> conditions.add(EXPIRED_TIME.ge(minTime)) }
                maxExpiredTime?.let { maxTime -> conditions.add(EXPIRED_TIME.le(maxTime)) }
                if (!iamGroupIds.isNullOrEmpty()) {
                    conditions.add(IAM_GROUP_ID.`in`(iamGroupIds))
                }
            }
        }
        return conditions
    }

    fun countProjectMember(
        dslContext: DSLContext,
        projectCode: String
    ): Map<String, Int> {
        val resourceMemberUnionAuthorizationMember = createResourceMemberUnionAuthorizationMember(
            dslContext = dslContext,
            projectCode = projectCode
        )

        return dslContext.select(
            field(MEMBER_TYPE, String::class.java),
            countDistinct(field(MEMBER_ID, Long::class.java))
        ).from(resourceMemberUnionAuthorizationMember)
            .groupBy(field(MEMBER_TYPE, Long::class.java))
            .skipCheck()
            .fetch().map { Pair(it.value1(), it.value2()) }.toMap()
    }

    fun countProjectMember(
        dslContext: DSLContext,
        projectCode: String,
        memberType: String?,
        userName: String?,
        deptName: String?
    ): Long {
        val resourceMemberUnionAuthorizationMember = createResourceMemberUnionAuthorizationMember(
            dslContext = dslContext,
            projectCode = projectCode
        )
        return dslContext
            .select(countDistinct(field(MEMBER_ID, Long::class.java)))
            .from(resourceMemberUnionAuthorizationMember)
            .where(
                buildResourceMemberConditions(
                    memberType = memberType,
                    userName = userName,
                    deptName = deptName
                )
            )
            .skipCheck()
            .fetchOne(0, Long::class.java) ?: 0L
    }

    fun createResourceMemberUnionAuthorizationMember(dslContext: DSLContext, projectCode: String): Table<*> {
        val tResourceGroupMember = TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER
        val tResourceAuthorization = TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION

        return dslContext
            .select(
                tResourceGroupMember.MEMBER_ID,
                tResourceGroupMember.MEMBER_NAME,
                tResourceGroupMember.MEMBER_TYPE
            )
            .from(tResourceGroupMember)
            .where(tResourceGroupMember.PROJECT_CODE.eq(projectCode))
            .and(tResourceGroupMember.MEMBER_TYPE.notEqual(MemberType.TEMPLATE.type))
            .groupBy(tResourceGroupMember.MEMBER_ID)
            .unionAll(
                dslContext.select(
                    tResourceAuthorization.HANDOVER_FROM.`as`("MEMBER_ID"),
                    tResourceAuthorization.HANDOVER_FROM_CN_NAME.`as`("MEMBER_NAME"),
                    inline("user").`as`("MEMBER_TYPE")
                )
                    .from(tResourceAuthorization)
                    .where(tResourceAuthorization.PROJECT_CODE.eq(projectCode))
                    .groupBy(tResourceAuthorization.HANDOVER_FROM)
            )
            .asTable(TABLE_NAME)
    }

    fun buildResourceMemberConditions(
        memberType: String?,
        userName: String?,
        deptName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        val memberId = field(MEMBER_ID, String::class.java)
        val memberTypeField = field(MEMBER_TYPE, String::class.java)
        val memberName = field(MEMBER_NAME, String::class.java)

        if (memberType != null) {
            conditions.add(memberTypeField.eq(memberType))
        }
        if (userName != null) {
            conditions.add(memberTypeField.eq(MemberType.USER.type))
            conditions.add(memberId.like("%$userName%").or(memberName.like("%$userName%")))
        }
        if (deptName != null) {
            conditions.add(memberTypeField.eq(MemberType.DEPARTMENT.type))
            conditions.add(memberName.like("%$deptName%"))
        }
        return conditions
    }

    /**
     * 获取成员按资源类型分组数量
     */
    fun countMemberGroupOfResourceType(
        dslContext: DSLContext,
        projectCode: String,
        memberId: String,
        iamTemplateIds: List<String>,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        minExpiredAt: LocalDateTime? = null,
        maxExpiredAt: LocalDateTime? = null,
        memberDeptInfos: List<String>? = null
    ): Map<String, Long> {
        val conditions = buildMemberGroupCondition(
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds,
            minExpiredAt = minExpiredAt,
            maxExpiredAt = maxExpiredAt,
            memberDeptInfos = memberDeptInfos
        )
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            val select = dslContext.select(RESOURCE_TYPE, count())
                .from(this)
                .where(conditions)
            select.groupBy(RESOURCE_TYPE)
            select.fetch().map { Pair(it.value1(), it.value2().toLong()) }.toMap()
        }
    }

    fun countMemberGroup(
        dslContext: DSLContext,
        projectCode: String,
        memberId: String,
        iamTemplateIds: List<String>,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        excludeIamGroupIds: List<Int>? = null,
        minExpiredAt: LocalDateTime? = null,
        maxExpiredAt: LocalDateTime? = null,
        memberDeptInfos: List<String>? = null,
        filterMemberType: MemberType? = null,
        onlyExcludeUserDirectlyJoined: Boolean? = false
    ): Long {
        val conditions = buildMemberGroupCondition(
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds,
            minExpiredAt = minExpiredAt,
            maxExpiredAt = maxExpiredAt,
            memberDeptInfos = memberDeptInfos,
            filterMemberType = filterMemberType
        )
        val excludeConditions = buildExcludeMemberGroupCondition(
            excludeIamGroupIds = excludeIamGroupIds,
            onlyExcludeUserDirectlyJoined = onlyExcludeUserDirectlyJoined
        )
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.select(count())
                .from(this)
                .where(conditions)
                .let {
                    excludeConditions.forEach { excludeCondition ->
                        it.andNot(excludeCondition)
                    }
                    it
                }
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun buildExcludeMemberGroupCondition(
        excludeIamGroupIds: List<Int>?,
        // 仅排除用户直接加入的组
        onlyExcludeUserDirectlyJoined: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            if (!excludeIamGroupIds.isNullOrEmpty()) {
                // 仅排除用户直接加入的用户组
                if (onlyExcludeUserDirectlyJoined == true) {
                    conditions.add(IAM_GROUP_ID.`in`(excludeIamGroupIds).and(MEMBER_TYPE.eq(MemberType.USER.type)))
                } else {
                    // 会把组织/用户/模板加入的組都排除
                    conditions.add(IAM_GROUP_ID.`in`(excludeIamGroupIds))
                }
            }
        }
        return conditions
    }

    fun checkResourceManager(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        memberId: String
    ): Boolean {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.selectCount()
                .from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(RESOURCE_CODE.eq(resourceCode))
                .and(MEMBER_ID.eq(memberId))
                .and(GROUP_CODE.eq(DefaultGroupType.MANAGER.value))
                .and(EXPIRED_TIME.gt(LocalDateTime.now()))
                .fetchOne(0, Int::class.java) != 0
        }
    }

    fun listMemberGroupIdsInProject(
        dslContext: DSLContext,
        projectCode: String,
        memberId: String,
        iamTemplateIds: List<String>,
        memberDeptInfos: List<String>,
    ): List<Int> {
        val conditions = buildMemberGroupCondition(
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            memberDeptInfos = memberDeptInfos,
            minExpiredAt = LocalDateTime.now()
        )
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.select(IAM_GROUP_ID).from(this)
                .where(conditions)
                .fetch().map { it.value1() }
        }
    }

    /**
     * 获取成员下用户组列表
     */
    fun listMemberGroupDetail(
        dslContext: DSLContext,
        projectCode: String,
        memberId: String,
        iamTemplateIds: List<String>? = emptyList(),
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        excludeIamGroupIds: List<Int>? = null,
        minExpiredAt: LocalDateTime? = null,
        maxExpiredAt: LocalDateTime? = null,
        memberDeptInfos: List<String>? = null,
        filterMemberType: MemberType? = null,
        onlyExcludeUserDirectlyJoined: Boolean? = false,
        offset: Int? = null,
        limit: Int? = null
    ): List<AuthResourceGroupMember> {
        val conditions = buildMemberGroupCondition(
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds,
            minExpiredAt = minExpiredAt,
            maxExpiredAt = maxExpiredAt,
            memberDeptInfos = memberDeptInfos,
            filterMemberType = filterMemberType
        )
        val excludeConditions = buildExcludeMemberGroupCondition(
            excludeIamGroupIds = excludeIamGroupIds,
            onlyExcludeUserDirectlyJoined = onlyExcludeUserDirectlyJoined
        )
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.selectFrom(this)
                .where(conditions)
                .let {
                    excludeConditions.forEach { excludeCondition ->
                        it.andNot(excludeCondition)
                    }
                    it
                }
                .orderBy(IAM_GROUP_ID.desc())
                .let { if (offset != null && limit != null) it.offset(offset).limit(limit) else it }
                .fetch()
                .map { convert(it) }
        }
    }

    private fun buildMemberGroupCondition(
        projectCode: String,
        memberId: String,
        iamTemplateIds: List<String>? = emptyList(),
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        minExpiredAt: LocalDateTime? = null,
        maxExpiredAt: LocalDateTime? = null,
        memberDeptInfos: List<String>? = null,
        filterMemberType: MemberType? = null
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            conditions.add(PROJECT_CODE.eq(projectCode))
            conditions.add(
                // 获取直接加入
                (MEMBER_ID.eq(memberId).and(
                    MEMBER_TYPE.`in`(listOf(MemberType.USER.type, MemberType.DEPARTMENT.type))
                )).let {
                    // 获取模板加入
                    if (!iamTemplateIds.isNullOrEmpty()) {
                        it.or(MEMBER_ID.`in`(iamTemplateIds).and(MEMBER_TYPE.eq(MemberType.TEMPLATE.type)))
                    } else {
                        it
                    }
                }.let {
                    // 获取组织加入
                    if (!memberDeptInfos.isNullOrEmpty()) {
                        it.or(MEMBER_ID.`in`(memberDeptInfos).and(MEMBER_TYPE.eq(MemberType.DEPARTMENT.type)))
                    } else {
                        it
                    }
                })
            filterMemberType?.let { conditions.add(MEMBER_TYPE.eq(filterMemberType.type)) }
            resourceType?.let { conditions.add(RESOURCE_TYPE.eq(resourceType)) }
            minExpiredAt?.let { conditions.add(EXPIRED_TIME.ge(minExpiredAt)) }
            maxExpiredAt?.let { conditions.add(EXPIRED_TIME.le(maxExpiredAt)) }
            if (!iamGroupIds.isNullOrEmpty()) {
                conditions.add(IAM_GROUP_ID.`in`(iamGroupIds))
            }
        }
        return conditions
    }

    fun listProjectUniqueManagerGroups(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupIds: List<Int>
    ): List<Int> {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.select(IAM_GROUP_ID)
                .from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(GROUP_CODE.eq(BkAuthGroup.MANAGER.value))
                .let { if (iamGroupIds.isEmpty()) it else it.and(IAM_GROUP_ID.`in`(iamGroupIds)) }
                .groupBy(IAM_GROUP_ID)
                .having(count(MEMBER_ID).eq(1))
                .fetch().map { it.value1() }
        }
    }

    fun convert(record: TAuthResourceGroupMemberRecord): AuthResourceGroupMember {
        return with(record) {
            AuthResourceGroupMember(
                id = id,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                groupCode = groupCode,
                iamGroupId = iamGroupId,
                memberId = memberId,
                memberName = memberName,
                memberType = memberType,
                expiredTime = expiredTime
            )
        }
    }

    companion object {
        private const val TABLE_NAME = "resourceMemberUnionAuthorizationMember"
        private const val MEMBER_ID = "$TABLE_NAME.MEMBER_ID"
        private const val MEMBER_NAME = "$TABLE_NAME.MEMBER_NAME"
        private const val MEMBER_TYPE = "$TABLE_NAME.MEMBER_TYPE"
        private const val IS_DEPARTED = "is_departed"
    }
}
