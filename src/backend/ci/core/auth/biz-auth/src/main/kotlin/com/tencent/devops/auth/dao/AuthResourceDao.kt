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

import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.model.auth.tables.TAuthResource
import com.tencent.devops.model.auth.tables.records.TAuthResourceRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@SuppressWarnings("LongParameterList")
class AuthResourceDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        enable: Boolean,
        relationId: String
    ): Int {
        val now = LocalDateTime.now()
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.insertInto(
                this,
                PROJECT_CODE,
                RESOURCE_TYPE,
                RESOURCE_CODE,
                RESOURCE_NAME,
                ENABLE,
                RELATION_ID,
                CREATE_TIME,
                CREATE_USER,
                UPDATE_TIME,
                UPDATE_USER
            ).values(
                projectCode,
                resourceType,
                resourceCode,
                resourceName,
                enable,
                relationId,
                now,
                userId,
                now,
                userId
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Int {
        val now = LocalDateTime.now()
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.update(this)
                .set(RESOURCE_NAME, resourceName)
                .set(UPDATE_TIME, now)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_CODE.eq(resourceCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ) {
        with(TAuthResource.T_AUTH_RESOURCE) {
            dslContext.delete(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_CODE.eq(resourceCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): TAuthResourceRecord? {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(RESOURCE_CODE.eq(resourceCode))
                .fetchOne()
        }
    }

    fun enable(
        dslContext: DSLContext,
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.update(this)
                .set(ENABLE, true)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(UPDATE_USER, userId)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(RESOURCE_CODE.eq(resourceCode))
                .execute() == 1
        }
    }

    fun disable(
        dslContext: DSLContext,
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.update(this)
                .set(ENABLE, false)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(UPDATE_USER, userId)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(RESOURCE_CODE.eq(resourceCode))
                .execute() == 1
        }
    }

    fun list(
        dslContext: DSLContext,
        projectCode: String,
        resourceName: String?,
        resourceType: String,
        offset: Int,
        limit: Int
    ): Result<TAuthResourceRecord> {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .let { if (resourceName == null) it else it.and(RESOURCE_NAME.like("%$resourceName%")) }
                .limit(limit)
                .offset(offset)
                .fetch()
        }
    }

    fun listByProjectAndType(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String
    ): List<String> {
        return with(TAuthResource.T_AUTH_RESOURCE) {
            dslContext.select(RESOURCE_CODE).from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .fetch(0, String::class.java)
        }
    }

    fun convert(recode: TAuthResourceRecord): AuthResourceInfo {
        with(recode) {
            return AuthResourceInfo(
                id = id,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                enable = enable,
                relationId = relationId
            )
        }
    }
}
