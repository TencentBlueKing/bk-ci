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
 *
 */

package com.tencent.devops.auth.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.auth.tables.TAuthResourceGroupConfig
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupConfigRecord
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class AuthResourceGroupConfigDao {

    fun getByGroupCode(
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
        resourceType: String,
        createMode: Boolean? = null,
        groupType: Int? = null
    ): Result<TAuthResourceGroupConfigRecord> {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.selectFrom(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .let { if (createMode == null) it else it.and(CREATE_MODE.eq(createMode)) }
                .let { if (groupType == null) it else it.and(GROUP_TYPE.eq(groupType)) }
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
                .skipCheck()
                .fetch()
        }
    }

    fun listAll(
        dslContext: DSLContext
    ): Result<TAuthResourceGroupConfigRecord> {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.selectFrom(this)
                .orderBy(ID.asc())
                .skipCheck()
                .fetch()
        }
    }

    fun getMaxId(dslContext: DSLContext): Long {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.select(ID.max())
                .from(this)
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun create(
        dslContext: DSLContext,
        resourceType: String,
        groupCode: String,
        groupName: String,
        description: String?,
        createMode: Boolean,
        groupType: Int,
        actions: String,
        authorizationScopes: String?
    ): Long {
        val now = LocalDateTime.now()
        val newId = getMaxId(dslContext) + 1
        with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.insertInto(
                this,
                ID,
                RESOURCE_TYPE,
                GROUP_CODE,
                GROUP_NAME,
                DESCRIPTION,
                CREATE_MODE,
                GROUP_TYPE,
                ACTIONS,
                AUTHORIZATION_SCOPES,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                newId,
                resourceType,
                groupCode,
                groupName,
                description,
                createMode,
                groupType,
                actions,
                authorizationScopes,
                now,
                now
            ).execute()
        }
        return newId
    }

    fun batchCreate(
        dslContext: DSLContext,
        configs: List<TAuthResourceGroupConfigRecord>
    ): Int {
        if (configs.isEmpty()) {
            return 0
        }
        val now = LocalDateTime.now()
        var currentMaxId = getMaxId(dslContext)
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            var count = 0
            configs.forEach { record ->
                currentMaxId++
                val result = dslContext.insertInto(
                    this,
                    ID,
                    RESOURCE_TYPE,
                    GROUP_CODE,
                    GROUP_NAME,
                    DESCRIPTION,
                    CREATE_MODE,
                    GROUP_TYPE,
                    ACTIONS,
                    AUTHORIZATION_SCOPES,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    currentMaxId,
                    record.resourceType,
                    record.groupCode,
                    record.groupName,
                    record.description,
                    record.createMode,
                    record.groupType,
                    record.actions,
                    record.authorizationScopes,
                    now,
                    now
                ).onDuplicateKeyUpdate()
                    .set(GROUP_NAME, record.groupName)
                    .set(DESCRIPTION, record.description)
                    .set(ACTIONS, record.actions)
                    .set(AUTHORIZATION_SCOPES, record.authorizationScopes)
                    .set(UPDATE_TIME, now)
                    .execute()
                count += result
            }
            count
        }
    }

    fun updateAuthorizationScopes(
        dslContext: DSLContext,
        id: Long,
        authorizationScopes: String
    ): Boolean {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.update(this)
                .set(AUTHORIZATION_SCOPES, authorizationScopes)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute() > 0
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ): Boolean {
        return with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute() > 0
        }
    }

    fun batchUpdateAuthResourceGroupConfig(
        dslContext: DSLContext,
        authAuthResourceGroupConfigs: List<TAuthResourceGroupConfigRecord>
    ) {
        if (authAuthResourceGroupConfigs.isEmpty()) {
            return
        }
        with(TAuthResourceGroupConfig.T_AUTH_RESOURCE_GROUP_CONFIG) {
            authAuthResourceGroupConfigs.forEach {
                dslContext.update(this)
                    .set(GROUP_NAME, it.groupName)
                    .set(DESCRIPTION, it.description)
                    .where(ID.eq(it.id))
                    .execute()
            }
        }
    }
}
