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

package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthResourceGroup
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("LongParameterList", "TooManyFunctions")
class AuthResourceGroupDao {

    fun create(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        iamResourceCode: String,
        groupCode: String,
        groupName: String,
        defaultGroup: Boolean,
        relationId: String
    ): Int {
        val now = LocalDateTime.now()
        with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            return dslContext.insertInto(
                this,
                PROJECT_CODE,
                RESOURCE_TYPE,
                RESOURCE_CODE,
                RESOURCE_NAME,
                IAM_RESOURCE_CODE,
                GROUP_CODE,
                GROUP_NAME,
                DEFAULT_GROUP,
                RELATION_ID,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectCode,
                resourceType,
                resourceCode,
                resourceName,
                iamResourceCode,
                groupCode,
                groupName,
                defaultGroup,
                relationId,
                now,
                now
            ).onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        groupCode: String,
        groupName: String
    ): Int {
        val now = LocalDateTime.now()
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.update(this)
                .set(GROUP_NAME, groupName)
                .set(RESOURCE_NAME, resourceName)
                .set(UPDATE_TIME, now)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_CODE.eq(resourceCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(GROUP_CODE.eq(groupCode))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String?
    ): TAuthResourceGroupRecord? {
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.selectFrom(this).where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_CODE.eq(resourceCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .let { if (groupCode == null) it else it.and(GROUP_CODE.eq(groupCode)) }
                .fetchOne()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ) {
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_CODE.eq(resourceCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .execute()
        }
    }

    fun deleteByIds(
        dslContext: DSLContext,
        ids: List<Long>
    ) {
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.deleteFrom(this).where(ID.`in`(ids)).execute()
        }
    }

    fun getByRelationId(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: String
    ): TAuthResourceGroupRecord? {
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RELATION_ID.eq(iamGroupId))
                .fetchOne()
        }
    }

    fun listByRelationId(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupIds: List<String>
    ): Result<TAuthResourceGroupRecord> {
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RELATION_ID.`in`(iamGroupIds))
                .fetch()
        }
    }

    fun getByGroupName(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupName: String
    ): TAuthResourceGroupRecord? {
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.selectFrom(this).where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_CODE.eq(resourceCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(GROUP_NAME.eq(groupName))
                .fetchOne()
        }
    }

    fun getByResourceCode(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): List<TAuthResourceGroupRecord> {
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.selectFrom(this).where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_CODE.eq(resourceCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .fetch()
        }
    }

    fun countByResourceCode(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Int {
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.selectCount()
                .from(this).where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_CODE.eq(resourceCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByResourceType(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String
    ): Int {
        return with(TAuthResourceGroup.T_AUTH_RESOURCE_GROUP) {
            dslContext.selectCount()
                .from(this).where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .fetchOne(0, Int::class.java)!!
        }
    }
}
