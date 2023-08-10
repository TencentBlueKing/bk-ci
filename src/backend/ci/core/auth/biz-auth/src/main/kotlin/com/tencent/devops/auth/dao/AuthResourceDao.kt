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
@SuppressWarnings("LongParameterList", "TooManyFunctions")
class AuthResourceDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        iamResourceCode: String,
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
                IAM_RESOURCE_CODE,
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
                iamResourceCode,
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

    fun getByIamCode(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        iamResourceCode: String
    ): TAuthResourceRecord? {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(IAM_RESOURCE_CODE.eq(iamResourceCode))
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

    fun list(
        dslContext: DSLContext,
        resourceType: String,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        offset: Int,
        limit: Int
    ): Result<TAuthResourceRecord> {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .let {
                    if (startTime != null && endTime != null)
                        it.and(UPDATE_TIME.between(startTime, endTime)) else it
                }
                .orderBy(UPDATE_TIME)
                .limit(limit)
                .offset(offset)
                .fetch()
        }
    }

    fun count(
        dslContext: DSLContext,
        projectCode: String,
        resourceName: String?,
        resourceType: String
    ): Long {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .let { if (resourceName == null) it else it.and(RESOURCE_NAME.like("%$resourceName%")) }
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getResourceCodeByIamCodes(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        iamResourceCodes: List<String>
    ): List<String> {
        return with(TAuthResource.T_AUTH_RESOURCE) {
            dslContext.select(RESOURCE_CODE).from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(IAM_RESOURCE_CODE.`in`(iamResourceCodes))
                .fetch(0, String::class.java)
        }
    }

    fun getIamCodeByResourceCodes(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCodes: List<String>
    ): List<String> {
        return with(TAuthResource.T_AUTH_RESOURCE) {
            dslContext.select(RESOURCE_CODE).from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(RESOURCE_CODE.`in`(resourceCodes))
                .fetch(0, String::class.java)
        }
    }

    fun listByByIamCodes(
        dslContext: DSLContext,
        resourceType: String,
        iamResourceCodes: List<String>
    ): Result<TAuthResourceRecord> {
        return with(TAuthResource.T_AUTH_RESOURCE) {
            dslContext.selectFrom(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .and(IAM_RESOURCE_CODE.`in`(iamResourceCodes))
                .fetch()
        }
    }

    fun getResourceCodeByType(
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

    fun getAllResourceCode(
        dslContext: DSLContext,
        resourceType: String
    ): List<String> {
        return with(TAuthResource.T_AUTH_RESOURCE) {
            dslContext.select(RESOURCE_CODE).from(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .fetch(0, String::class.java)
        }
    }

    fun countResourceByType(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String
    ): Int {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countResourceByUpdateTime(
        dslContext: DSLContext,
        resourceType: String,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null
    ): Long {
        with(TAuthResource.T_AUTH_RESOURCE) {
            return dslContext.selectCount()
                .from(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .let {
                    if (startTime != null && endTime != null)
                        it.and(UPDATE_TIME.between(startTime, endTime)) else it
                }
                .fetchOne(0, Long::class.java)!!
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
                iamResourceCode = iamResourceCode,
                enable = enable,
                relationId = relationId,
                createUser = createUser,
                updateUser = updateUser,
                createTime = createTime,
                updateTime = updateTime
            )
        }
    }
}
