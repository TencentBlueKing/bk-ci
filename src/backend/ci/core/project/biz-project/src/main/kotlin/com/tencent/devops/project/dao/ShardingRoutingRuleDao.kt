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
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.project.tables.TShardingRoutingRule
import com.tencent.devops.model.project.tables.records.TShardingRoutingRuleRecord
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ShardingRoutingRuleDao {

    fun add(dslContext: DSLContext, userId: String, shardingRoutingRule: ShardingRoutingRule) {
        with(TShardingRoutingRule.T_SHARDING_ROUTING_RULE) {
            dslContext.insertInto(
                this,
                ID,
                CLUSTER_NAME,
                MODULE_CODE,
                TYPE,
                DATA_SOURCE_NAME,
                TABLE_NAME,
                ROUTING_NAME,
                ROUTING_RULE,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    shardingRoutingRule.clusterName,
                    shardingRoutingRule.moduleCode.name,
                    shardingRoutingRule.type.name,
                    shardingRoutingRule.dataSourceName,
                    shardingRoutingRule.tableName ?: "",
                    shardingRoutingRule.routingName,
                    shardingRoutingRule.routingRule,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun countByName(
        dslContext: DSLContext,
        clusterName: String,
        moduleCode: SystemModuleEnum,
        type: ShardingRuleTypeEnum,
        routingName: String,
        tableName: String? = null
    ): Int {
        with(TShardingRoutingRule.T_SHARDING_ROUTING_RULE) {
            val conditions = getQueryShardingRoutingRuleCondition(
                clusterName = clusterName,
                moduleCode = moduleCode,
                type = type,
                routingName = routingName,
                tableName = tableName
            )
            return dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TShardingRoutingRule.T_SHARDING_ROUTING_RULE) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        clusterName: String,
        moduleCode: SystemModuleEnum,
        type: ShardingRuleTypeEnum,
        routingName: String,
        tableName: String? = null
    ): TShardingRoutingRuleRecord? {
        with(TShardingRoutingRule.T_SHARDING_ROUTING_RULE) {
            val conditions = getQueryShardingRoutingRuleCondition(
                clusterName = clusterName,
                moduleCode = moduleCode,
                type = type,
                routingName = routingName,
                tableName = tableName
            )
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()
        }
    }

    fun getById(dslContext: DSLContext, id: String): TShardingRoutingRuleRecord? {
        return with(TShardingRoutingRule.T_SHARDING_ROUTING_RULE) {
            dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getByName(dslContext: DSLContext, routingName: String): TShardingRoutingRuleRecord? {
        return with(TShardingRoutingRule.T_SHARDING_ROUTING_RULE) {
            dslContext.selectFrom(this)
                .where(ROUTING_NAME.eq(routingName))
                .limit(1)
                .fetchOne()
        }
    }

    fun update(dslContext: DSLContext, id: String, shardingRoutingRule: ShardingRoutingRule) {
        with(TShardingRoutingRule.T_SHARDING_ROUTING_RULE) {
            dslContext.update(this)
                .set(TYPE, shardingRoutingRule.type.name)
                .set(CLUSTER_NAME, shardingRoutingRule.clusterName)
                .set(MODULE_CODE, shardingRoutingRule.moduleCode.name)
                .set(DATA_SOURCE_NAME, shardingRoutingRule.dataSourceName)
                .set(ROUTING_NAME, shardingRoutingRule.routingName)
                .set(ROUTING_RULE, shardingRoutingRule.routingRule)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    private fun TShardingRoutingRule.getQueryShardingRoutingRuleCondition(
        clusterName: String,
        moduleCode: SystemModuleEnum,
        type: ShardingRuleTypeEnum,
        routingName: String,
        tableName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(CLUSTER_NAME.eq(clusterName))
        conditions.add(MODULE_CODE.eq(moduleCode.name))
        conditions.add(TYPE.eq(type.name))
        conditions.add(ROUTING_NAME.eq(routingName))
        if (!tableName.isNullOrBlank()) {
            conditions.add(TABLE_NAME.eq(tableName))
        }
        return conditions
    }
}
