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

import com.tencent.devops.model.auth.tables.TAuthResourceGroupConfig
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupConfigRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class AuthResourceGroupConfigDao {

    fun get(
        dslContext: DSLContext,
        resourceType: String,
        groupCode: String
    ): TAuthResourceGroupConfigRecord? {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.selectFrom(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .and(GROUP_CODE.eq(groupCode))
                .fetchOne()
        }
    }

    fun get(
        dslContext: DSLContext,
        resourceType: String
    ): Result<TAuthResourceGroupConfigRecord> {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.selectFrom(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .fetch()
        }
    }

    fun get(
        dslContext: DSLContext,
        resourceType: String,
        createMode: Boolean
    ): Result<TAuthResourceGroupConfigRecord> {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.selectFrom(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .and(CREATE_MODE.eq(createMode))
                .fetch()
        }
    }

    fun getByName(
        dslContext: DSLContext,
        resourceType: String,
        groupName: String
    ): TAuthResourceGroupConfigRecord? {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.selectFrom(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .and(GROUP_NAME.eq(groupName))
                .fetchOne()
        }
    }

    fun getById(
        dslContext: DSLContext,
        id: Long
    ): TAuthResourceGroupConfigRecord? {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun countByResourceType(
        dslContext: DSLContext,
        resourceType: String
    ): Int {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.selectCount()
                .from(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .fetchOne(0, Int::class.java)!!
        }
    }
    fun list(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TAuthResourceGroupConfigRecord> {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.selectFrom(this)
                .orderBy(CREATE_TIME.desc(), RESOURCE_TYPE, GROUP_CODE)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun batchUpdateAuthResourceGroupConfig(
        dslContext: DSLContext,
        authAuthResourceGroupConfigs: List<TAuthResourceGroupConfigRecord>
    ) {
        if (authAuthResourceGroupConfigs.isEmpty()) {
            return
        }
        dslContext.batchUpdate(authAuthResourceGroupConfigs).execute()
    }
}
