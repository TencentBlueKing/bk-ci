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

package com.tencent.devops.auth.dao

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.model.auth.tables.TAuthResourceAuthorization
import com.tencent.devops.model.auth.tables.TAuthResourceGroupMember
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupMemberRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Table
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
            dslContext.batch(groupMembers.map {
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
            }).execute()
        }
    }

    fun batchUpdate(dslContext: DSLContext, groupMembers: List<AuthResourceGroupMember>) {
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.batch(groupMembers.map {
                dslContext.update(this)
                    .set(MEMBER_NAME, it.memberName)
                    .set(EXPIRED_TIME, it.expiredTime)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(PROJECT_CODE.eq(it.projectCode))
                    .and(IAM_GROUP_ID.eq(it.iamGroupId))
                    .and(MEMBER_ID.eq(it.memberId))
            }).execute()
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
        memberId: String? = null,
        memberName: String? = null,
        memberType: String? = null,
        iamGroupId: Int? = null
    ): List<AuthResourceGroupMember> {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            val select = dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
            resourceType?.let { select.and(RESOURCE_TYPE.eq(resourceType)) }
            memberId?.let { select.and(MEMBER_ID.eq(memberId)) }
            memberName?.let { select.and(MEMBER_NAME.eq(memberName)) }
            memberType?.let { select.and(MEMBER_TYPE.eq(memberType)) }
            iamGroupId?.let { select.and(IAM_GROUP_ID.eq(iamGroupId)) }
            select.fetch().map { convert(it) }
        }
    }

    fun listProjectMember(
        dslContext: DSLContext,
        projectCode: String,
        memberType: String?,
        userName: String?,
        deptName: String?,
        offset: Int,
        limit: Int
    ): List<ResourceMemberInfo> {
        val resourceMemberUnionAuthorizationMember = createResourceMemberUnionAuthorizationMember(
            dslContext = dslContext,
            projectCode = projectCode
        )

        return dslContext
            .select(
                field(MEMBER_ID, String::class.java),
                field(MEMBER_NAME, String::class.java),
                field(MEMBER_TYPE, String::class.java)
            )
            .from(resourceMemberUnionAuthorizationMember)
            .where(
                buildResourceMemberConditions(
                    memberType = memberType,
                    userName = userName,
                    deptName = deptName
                )
            )
            .groupBy(
                field(MEMBER_ID)
            )
            .orderBy(field(MEMBER_ID))
            .offset(offset).limit(limit)
            .fetch().map {
                ResourceMemberInfo(id = it.value1(), name = it.value2(), type = it.value3())
            }
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
            .and(tResourceGroupMember.MEMBER_TYPE.notEqual(ManagerScopesEnum.getType(ManagerScopesEnum.TEMPLATE)))
            .unionAll(
                dslContext.select(
                    tResourceAuthorization.HANDOVER_FROM.`as`("MEMBER_ID"),
                    tResourceAuthorization.HANDOVER_FROM_CN_NAME.`as`("MEMBER_NAME"),
                    inline("user").`as`("MEMBER_TYPE")
                )
                    .from(tResourceAuthorization)
                    .where(tResourceAuthorization.PROJECT_CODE.eq(projectCode))
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
            conditions.add(memberTypeField.eq(ManagerScopesEnum.getType(ManagerScopesEnum.USER)))
            conditions.add(memberId.like("%$userName%").or(memberName.like("%$userName%")))
        }
        if (deptName != null) {
            conditions.add(memberTypeField.eq(ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)))
            conditions.add(memberName.like("%$deptName%"))
        }
        return conditions
    }

    /**
     * 查询组下所有成员
     */
    fun listGroupMember(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int
    ): List<AuthResourceGroupMember> {
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.eq(iamGroupId))
                .fetch().map {
                    convert(it)
                }
        }
    }

    /**
     * 获取成员按资源类型分组数量
     */
    fun countMemberGroup(
        dslContext: DSLContext,
        projectCode: String,
        memberId: String,
        iamTemplateIds: List<String>,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null
    ): Map<String, Long> {
        val conditions = buildMemberGroupCondition(
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds
        )
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            val select = dslContext.select(RESOURCE_TYPE, count())
                .from(this)
                .where(conditions)
            select.groupBy(RESOURCE_TYPE)
            select.fetch().map { Pair(it.value1(), it.value2().toLong()) }.toMap()
        }
    }

    /**
     * 获取成员下用户组列表
     */
    fun listMemberGroupDetail(
        dslContext: DSLContext,
        projectCode: String,
        memberId: String,
        iamTemplateIds: List<String>,
        resourceType: String?,
        iamGroupIds: List<Int>? = null,
        offset: Int?,
        limit: Int?
    ): List<AuthResourceGroupMember> {
        val conditions = buildMemberGroupCondition(
            projectCode = projectCode,
            memberId = memberId,
            iamTemplateIds = iamTemplateIds,
            resourceType = resourceType,
            iamGroupIds = iamGroupIds
        )
        return with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(IAM_GROUP_ID)
                .let { if (offset != null && limit != null) it.offset(offset).limit(limit) else it }
                .fetch()
                .map { convert(it) }
        }
    }

    private fun buildMemberGroupCondition(
        projectCode: String,
        memberId: String,
        iamTemplateIds: List<String>,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        with(TAuthResourceGroupMember.T_AUTH_RESOURCE_GROUP_MEMBER) {
            conditions.add(PROJECT_CODE.eq(projectCode))
            conditions.add(
                (MEMBER_ID.eq(memberId).and(
                    MEMBER_TYPE.`in`(
                        listOf(
                            ManagerScopesEnum.getType(ManagerScopesEnum.USER),
                            ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)
                        )
                    )
                ))
                    .or(
                        MEMBER_ID.`in`(iamTemplateIds)
                            .and(MEMBER_TYPE.eq(ManagerScopesEnum.getType(ManagerScopesEnum.TEMPLATE)))
                    )
            )
            resourceType?.let { conditions.add(RESOURCE_TYPE.eq(resourceType)) }
            iamGroupIds?.let { conditions.add(IAM_GROUP_ID.`in`(iamGroupIds)) }
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
                .and(IAM_GROUP_ID.`in`(iamGroupIds))
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
    }
}
