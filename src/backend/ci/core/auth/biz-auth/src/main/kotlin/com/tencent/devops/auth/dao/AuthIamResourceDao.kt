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

import com.tencent.devops.auth.entity.AuthIamResourceInfo
import com.tencent.devops.model.auth.tables.TAuthIamResource
import com.tencent.devops.model.auth.tables.records.TAuthIamResourceRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthIamResourceDao {

    @SuppressWarnings("LongParameterList")
    fun create(
        dslContext: DSLContext,
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        gradeManagerId: String,
        subsetGradeManagerId: String
    ): Int {
        val now = LocalDateTime.now()
        with(TAuthIamResource.T_AUTH_IAM_RESOURCE) {
            return dslContext.insertInto(
                this,
                PROJECT_CODE,
                RESOURCE_TYPE,
                RESOURCE_CODE,
                RESOURCE_NAME,
                GRADE_MANAGER_ID,
                SUBSET_GRADE_MANAGER_ID,
                CREATE_TIME,
                CREATE_USER,
                UPDATE_TIME,
                UPDATE_USER
            ).values(
                projectCode,
                resourceType,
                resourceCode,
                resourceName,
                gradeManagerId,
                subsetGradeManagerId,
                now,
                userId,
                now,
                userId
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): TAuthIamResourceRecord? {
        with(TAuthIamResource.T_AUTH_IAM_RESOURCE) {
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
        with(TAuthIamResource.T_AUTH_IAM_RESOURCE) {
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
        with(TAuthIamResource.T_AUTH_IAM_RESOURCE) {
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

    fun convert(recode: TAuthIamResourceRecord): AuthIamResourceInfo {
        with(recode) {
            return AuthIamResourceInfo(
                id = id,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                gradeManagerId = gradeManagerId,
                subsetGradeManagerId = subsetGradeManagerId
            )
        }
    }
}
