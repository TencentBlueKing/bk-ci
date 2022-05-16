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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.ShardingUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.project.dao.ShardingRoutingRuleDao
import com.tencent.devops.project.service.ShardingRoutingRuleService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ShardingRoutingRuleServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val shardingRoutingRuleDao: ShardingRoutingRuleDao,
    private val redisOperation: RedisOperation
) : ShardingRoutingRuleService {

    override fun addShardingRoutingRule(userId: String, shardingRoutingRule: ShardingRoutingRule): Boolean {
        val routingName = shardingRoutingRule.routingName
        val nameCount = shardingRoutingRuleDao.countByName(
            dslContext = dslContext,
            clusterName = shardingRoutingRule.clusterName,
            moduleCode = shardingRoutingRule.moduleCode,
            type = shardingRoutingRule.type,
            routingName = routingName
        )
        if (nameCount > 0) {
            // 抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(routingName)
            )
        }
        // 规则入库
        shardingRoutingRuleDao.add(dslContext, userId, shardingRoutingRule)
        // 规则写入redis缓存
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = CommonUtils.getDbClusterName(),
            moduleCode = shardingRoutingRule.moduleCode.name,
            ruleType = shardingRoutingRule.type.name,
            routingName = routingName
        )
        redisOperation.set(
            key = key,
            value = shardingRoutingRule.routingRule,
            expired = false
        )
        return true
    }

    override fun deleteShardingRoutingRule(userId: String, id: String): Boolean {
        val shardingRoutingRuleRecord = shardingRoutingRuleDao.getById(dslContext, id)
        if (shardingRoutingRuleRecord != null) {
            // 删除db中规则信息
            shardingRoutingRuleDao.delete(dslContext, id)
            val routingName = shardingRoutingRuleRecord.routingName
            // 删除redis中规则信息
            val key = ShardingUtil.getShardingRoutingRuleKey(
                clusterName = CommonUtils.getDbClusterName(),
                moduleCode = shardingRoutingRuleRecord.moduleCode,
                ruleType = shardingRoutingRuleRecord.type,
                routingName = routingName
            )
            redisOperation.delete(key)
        }
        return true
    }

    override fun updateShardingRoutingRule(
        userId: String,
        id: String,
        shardingRoutingRule: ShardingRoutingRule
    ): Boolean {
        val routingName = shardingRoutingRule.routingName
        val nameCount = shardingRoutingRuleDao.countByName(
            dslContext = dslContext,
            clusterName = shardingRoutingRule.clusterName,
            moduleCode = shardingRoutingRule.moduleCode,
            type = shardingRoutingRule.type,
            routingName = routingName
        )
        if (nameCount > 0) {
            // 判断更新的名称是否属于自已
            val rule = shardingRoutingRuleDao.getById(dslContext, id)
            if (null != rule && routingName != rule.routingName) {
                // 抛出错误提示
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(routingName)
                )
            }
        }
        // 更新db中规则信息
        shardingRoutingRuleDao.update(dslContext, id, shardingRoutingRule)
        // 更新redis缓存规则信息
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = CommonUtils.getDbClusterName(),
            moduleCode = shardingRoutingRule.moduleCode.name,
            ruleType = shardingRoutingRule.type.name,
            routingName = routingName
        )
        redisOperation.set(
            key = key,
            value = shardingRoutingRule.routingRule,
            expired = false
        )
        return true
    }

    override fun getShardingRoutingRuleById(id: String): ShardingRoutingRule? {
        val record = shardingRoutingRuleDao.getById(dslContext, id)
        return if (record != null) {
            ShardingRoutingRule(
                clusterName = record.clusterName,
                moduleCode = SystemModuleEnum.valueOf(record.moduleCode),
                type = ShardingRuleTypeEnum.valueOf(record.type),
                routingName = record.routingName,
                routingRule = record.routingRule
            )
        } else {
            null
        }
    }

    override fun getShardingRoutingRuleByName(
        moduleCode: SystemModuleEnum,
        ruleType: ShardingRuleTypeEnum,
        routingName: String
    ): ShardingRoutingRule? {
        // 从redis缓存中获取规则信息
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = CommonUtils.getDbClusterName(),
            moduleCode = moduleCode.name,
            ruleType = ruleType.name,
            routingName = routingName
        )
        val routingRule = redisOperation.get(key)
        // 获取集群名称
        val clusterName = CommonUtils.getDbClusterName()
        return if (routingRule.isNullOrBlank()) {
            // redis缓存中未取到规则信息则从db查
            val record = shardingRoutingRuleDao.getByName(dslContext, routingName)
            if (record != null) {
                // 更新redis缓存规则信息
                redisOperation.set(
                    key = key,
                    value = record.routingRule,
                    expired = false
                )
                ShardingRoutingRule(
                    clusterName = record.clusterName,
                    moduleCode = moduleCode,
                    type = ruleType,
                    routingName = routingName,
                    routingRule = record.routingRule
                )
            } else {
                null
            }
        } else {
            ShardingRoutingRule(
                clusterName = clusterName,
                moduleCode = moduleCode,
                type = ruleType,
                routingName = routingName,
                routingRule = routingRule
            )
        }
    }
}
