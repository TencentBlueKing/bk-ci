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

import com.tencent.devops.model.project.tables.TShardingRoutingRule
import com.tencent.devops.model.project.tables.records.TShardingRoutingRuleRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class TxShardingRoutingRuleDao {

    fun getShardingRoutingRules(
        dslContext: DSLContext,
        type: String,
        clusterName: String? = null,
        moduleCode: String? = null,
        limit: Int,
        offset: Int
    ): Result<TShardingRoutingRuleRecord>? {
        return with(TShardingRoutingRule.T_SHARDING_ROUTING_RULE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(TYPE.eq(type))
            clusterName?.let { conditions.add(CLUSTER_NAME.eq(clusterName)) }
            moduleCode?.let { conditions.add(MODULE_CODE.eq(moduleCode)) }
            dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.asc(), ID.asc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateShardingRoutingRule(
        dslContext: DSLContext,
        id: String,
        type: String,
        clusterName: String,
        moduleCode: String,
        dataSourceName: String
    ) {
        with(TShardingRoutingRule.T_SHARDING_ROUTING_RULE) {
            dslContext.update(this)
                .set(TYPE, type)
                .set(CLUSTER_NAME, clusterName)
                .set(MODULE_CODE, moduleCode)
                .set(DATA_SOURCE_NAME, dataSourceName)
                .where(ID.eq(id))
                .execute()
        }
    }
}
