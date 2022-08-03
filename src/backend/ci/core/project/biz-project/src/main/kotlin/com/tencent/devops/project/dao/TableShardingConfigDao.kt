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

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.project.tables.TTableShardingConfig
import com.tencent.devops.model.project.tables.records.TTableShardingConfigRecord
import com.tencent.devops.project.pojo.TableShardingConfig
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TableShardingConfigDao {

    fun add(dslContext: DSLContext, userId: String, tableShardingConfig: TableShardingConfig) {
        with(TTableShardingConfig.T_TABLE_SHARDING_CONFIG) {
            dslContext.insertInto(
                this,
                ID,
                CLUSTER_NAME,
                MODULE_CODE,
                TABLE_NAME,
                SHARDING_NUM,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    tableShardingConfig.clusterName,
                    tableShardingConfig.moduleCode.name,
                    tableShardingConfig.tableName,
                    tableShardingConfig.shardingNum,
                    userId,
                    userId
                ).onDuplicateKeyUpdate()
                .set(SHARDING_NUM, tableShardingConfig.shardingNum)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .execute()
        }
    }

    fun countByName(
        dslContext: DSLContext,
        clusterName: String,
        moduleCode: SystemModuleEnum,
        tableName: String
    ): Int {
        with(TTableShardingConfig.T_TABLE_SHARDING_CONFIG) {
            return dslContext.selectCount().from(this)
                .where(
                    CLUSTER_NAME.eq(clusterName)
                        .and(MODULE_CODE.eq(moduleCode.name))
                        .and(TABLE_NAME.eq(tableName))
                )
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TTableShardingConfig.T_TABLE_SHARDING_CONFIG) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getById(dslContext: DSLContext, id: String): TTableShardingConfigRecord? {
        return with(TTableShardingConfig.T_TABLE_SHARDING_CONFIG) {
            dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getByName(
        dslContext: DSLContext,
        clusterName: String,
        moduleCode: SystemModuleEnum,
        tableName: String
    ): TTableShardingConfigRecord? {
        return with(TTableShardingConfig.T_TABLE_SHARDING_CONFIG) {
            dslContext.selectFrom(this)
                .where(
                    CLUSTER_NAME.eq(clusterName)
                        .and(MODULE_CODE.eq(moduleCode.name))
                        .and(TABLE_NAME.eq(tableName))
                )
                .limit(1)
                .fetchOne()
        }
    }

    fun listByModule(
        dslContext: DSLContext,
        clusterName: String,
        moduleCode: SystemModuleEnum
    ): Result<TTableShardingConfigRecord>? {
        return with(TTableShardingConfig.T_TABLE_SHARDING_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(CLUSTER_NAME.eq(clusterName))
            conditions.add(MODULE_CODE.eq(moduleCode.name))
            dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: String,
        userId: String,
        tableShardingConfig: TableShardingConfig
    ) {
        with(TTableShardingConfig.T_TABLE_SHARDING_CONFIG) {
            dslContext.update(this)
                .set(CLUSTER_NAME, tableShardingConfig.clusterName)
                .set(MODULE_CODE, tableShardingConfig.moduleCode.name)
                .set(SHARDING_NUM, tableShardingConfig.shardingNum)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }
}
