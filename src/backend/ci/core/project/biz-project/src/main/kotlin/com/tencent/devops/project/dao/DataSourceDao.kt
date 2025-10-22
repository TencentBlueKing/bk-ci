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

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.project.tables.TDataSource
import com.tencent.devops.model.project.tables.records.TDataSourceRecord
import com.tencent.devops.project.pojo.DataSource
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DataSourceDao {

    fun add(dslContext: DSLContext, userId: String, dataSource: DataSource) {
        with(TDataSource.T_DATA_SOURCE) {
            dslContext.insertInto(
                this,
                ID,
                CLUSTER_NAME,
                MODULE_CODE,
                DATA_SOURCE_NAME,
                FULL_FLAG,
                DS_URL,
                TAG,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    dataSource.clusterName,
                    dataSource.moduleCode.name,
                    dataSource.dataSourceName,
                    dataSource.fullFlag,
                    dataSource.dsUrl,
                    dataSource.dataTag,
                    userId,
                    userId
                ).onDuplicateKeyUpdate()
                .set(FULL_FLAG, dataSource.fullFlag)
                .set(DS_URL, dataSource.dsUrl)
                .set(TAG, dataSource.dataTag)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun countByName(dslContext: DSLContext, clusterName: String, moduleCode: String, dataSourceName: String): Int {
        with(TDataSource.T_DATA_SOURCE) {
            return dslContext.selectCount().from(this)
                .where(
                    CLUSTER_NAME.eq(clusterName)
                        .and(MODULE_CODE.eq(moduleCode))
                        .and(DATA_SOURCE_NAME.eq(dataSourceName))
                )
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TDataSource.T_DATA_SOURCE) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getById(dslContext: DSLContext, id: String): TDataSourceRecord? {
        return with(TDataSource.T_DATA_SOURCE) {
            dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    @Suppress("LongParameterList")
    fun listByModule(
        dslContext: DSLContext,
        clusterName: String,
        moduleCode: SystemModuleEnum,
        ruleType: ShardingRuleTypeEnum = ShardingRuleTypeEnum.DB,
        fullFlag: Boolean? = false,
        dataTag: String? = null
    ): Result<TDataSourceRecord>? {
        return with(TDataSource.T_DATA_SOURCE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(CLUSTER_NAME.eq(clusterName))
            conditions.add(MODULE_CODE.eq(moduleCode.name))
            conditions.add(TYPE.eq(ruleType.name))
            if (fullFlag != null) {
                conditions.add(FULL_FLAG.eq(fullFlag))
            }
            if (dataTag != null) {
                conditions.add(TAG.eq(dataTag))
            } else {
                conditions.add(TAG.isNull)
            }
            dslContext.selectFrom(this).where(conditions).orderBy(DATA_SOURCE_NAME.asc()).fetch()
        }
    }

    fun update(dslContext: DSLContext, id: String, dataSource: DataSource) {
        with(TDataSource.T_DATA_SOURCE) {
            dslContext.update(this)
                .set(CLUSTER_NAME, dataSource.clusterName)
                .set(MODULE_CODE, dataSource.moduleCode.name)
                .set(DATA_SOURCE_NAME, dataSource.dataSourceName)
                .set(FULL_FLAG, dataSource.fullFlag)
                .set(DS_URL, dataSource.dsUrl)
                .set(TAG, dataSource.dataTag)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getDataBasePiecewiseById(
        dslContext: DSLContext,
        moduleCode: SystemModuleEnum,
        clusterName: String,
        routingRule: String
    ): TDataSourceRecord? {
        with(TDataSource.T_DATA_SOURCE) {
            return dslContext.selectFrom(this)
                .where(MODULE_CODE.eq(moduleCode.name))
                .and(DATA_SOURCE_NAME.eq(routingRule))
                .and(CLUSTER_NAME.eq(clusterName)).fetchOne()
        }
    }
}
