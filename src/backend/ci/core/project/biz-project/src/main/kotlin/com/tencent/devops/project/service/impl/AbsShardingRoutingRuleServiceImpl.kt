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
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.project.dao.ShardingRoutingRuleDao
import com.tencent.devops.project.service.ShardingRoutingRuleService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

abstract class AbsShardingRoutingRuleServiceImpl @Autowired constructor(
    val dslContext: DSLContext,
    val redisOperation: RedisOperation,
    private val shardingRoutingRuleDao: ShardingRoutingRuleDao
) : ShardingRoutingRuleService {

    /**
     * 添加分片路由规则
     * @param userId 用户ID
     * @param shardingRoutingRule 片路由规则
     * @return 布尔值
     */
    override fun addShardingRoutingRule(userId: String, shardingRoutingRule: ShardingRoutingRule): Boolean {
        val routingName = shardingRoutingRule.routingName
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = CommonUtils.getDbClusterName(),
            moduleCode = shardingRoutingRule.moduleCode.name,
            ruleType = shardingRoutingRule.type.name,
            routingName = routingName,
            tableName = shardingRoutingRule.tableName
        )
        val lock = RedisLock(redisOperation, "$key:add", 10)
        try {
            lock.lock()
            val nameCount = shardingRoutingRuleDao.countByName(
                dslContext = dslContext,
                clusterName = shardingRoutingRule.clusterName,
                moduleCode = shardingRoutingRule.moduleCode,
                type = shardingRoutingRule.type,
                routingName = routingName,
                tableName = shardingRoutingRule.tableName
            )
            if (nameCount > 0) {
                // 已添加则无需重复添加
                return true
            }
            // 规则入库
            shardingRoutingRuleDao.add(dslContext, userId, shardingRoutingRule)
            // 规则写入redis缓存
            redisOperation.set(
                key = key,
                value = shardingRoutingRule.routingRule,
                expired = false
            )
        } finally {
            lock.unlock()
        }
        return true
    }

    /**
     * 删除分片路由规则
     * @param userId 用户ID
     * @param id 规则ID
     * @return 布尔值
     */
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
                routingName = routingName,
                tableName = shardingRoutingRuleRecord.tableName
            )
            redisOperation.delete(key)
        }
        return true
    }

    /**
     * 更新分片路由规则
     * @param userId 用户ID
     * @param id 规则ID
     * @param shardingRoutingRule 片路由规则
     * @return 布尔值
     */
    override fun updateShardingRoutingRule(
        userId: String,
        id: String,
        shardingRoutingRule: ShardingRoutingRule
    ): Boolean {
        val routingName = shardingRoutingRule.routingName
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = CommonUtils.getDbClusterName(),
            moduleCode = shardingRoutingRule.moduleCode.name,
            ruleType = shardingRoutingRule.type.name,
            routingName = routingName,
            tableName = shardingRoutingRule.tableName
        )
        val lock = RedisLock(redisOperation, "$key:update", 10)
        try {
            lock.lock()
            val nameCount = shardingRoutingRuleDao.countByName(
                dslContext = dslContext,
                clusterName = shardingRoutingRule.clusterName,
                moduleCode = shardingRoutingRule.moduleCode,
                type = shardingRoutingRule.type,
                routingName = routingName,
                tableName = shardingRoutingRule.tableName
            )
            if (nameCount > 0) {
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
            redisOperation.set(
                key = key,
                value = shardingRoutingRule.routingRule,
                expired = false
            )
        } finally {
            lock.unlock()
        }
        return true
    }

    /**
     * 根据ID查找分片路由规则
     * @param id 规则ID
     * @return 分片路由规则信息
     */
    override fun getShardingRoutingRuleById(id: String): ShardingRoutingRule? {
        val record = shardingRoutingRuleDao.getById(dslContext, id)
        return if (record != null) {
            ShardingRoutingRule(
                clusterName = record.clusterName,
                moduleCode = SystemModuleEnum.valueOf(record.moduleCode),
                dataSourceName = record.dataSourceName,
                tableName = record.tableName,
                type = ShardingRuleTypeEnum.valueOf(record.type),
                routingName = record.routingName,
                routingRule = record.routingRule
            )
        } else {
            null
        }
    }

    /**
     * 根据规则名称查找分片路由规则
     * @param moduleCode 模块标识
     * @param ruleType 规则类型
     * @param routingName 规则名称
     * @param tableName 数据库表名称
     * @return 分片路由规则信息
     */
    override fun getShardingRoutingRuleByName(
        moduleCode: SystemModuleEnum,
        ruleType: ShardingRuleTypeEnum,
        routingName: String,
        tableName: String?
    ): ShardingRoutingRule? {
        // 获取集群名称
        val clusterName = CommonUtils.getDbClusterName()
        // 从redis缓存中获取规则信息
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ruleType.name,
            routingName = routingName,
            tableName = tableName
        )
        val routingRule = redisOperation.get(key)
        return if (routingRule.isNullOrBlank()) {
            // redis缓存中未取到规则信息则从db查
            val record = shardingRoutingRuleDao.get(
                dslContext = dslContext,
                clusterName = clusterName,
                moduleCode = moduleCode,
                type = ruleType,
                routingName = routingName,
                tableName = tableName
            )
            if (record != null) {
                // 更新redis缓存规则信息
                redisOperation.set(
                    key = key,
                    value = record.routingRule,
                    expired = false
                )
                ShardingRoutingRule(
                    clusterName = record.clusterName ?: "",
                    moduleCode = moduleCode,
                    dataSourceName = record.dataSourceName ?: "",
                    tableName = record.tableName,
                    type = ruleType,
                    routingName = routingName,
                    routingRule = record.routingRule
                )
            } else {
                null
            }
        } else {
            if (tableName.isNullOrBlank()) {
                // 生成db的分片规则
                ShardingRoutingRule(
                    clusterName = clusterName,
                    moduleCode = moduleCode,
                    dataSourceName = routingRule,
                    type = ruleType,
                    routingName = routingName,
                    routingRule = routingRule
                )
            } else {
                // 生成数据库表的分片规则
                val dbShardingRoutingRule = getShardingRoutingRuleByName(
                    moduleCode = moduleCode,
                    ruleType = ShardingRuleTypeEnum.DB,
                    routingName = routingName
                )
                dbShardingRoutingRule?.let {
                    ShardingRoutingRule(
                        clusterName = clusterName,
                        moduleCode = moduleCode,
                        dataSourceName = it.dataSourceName,
                        tableName = tableName,
                        type = ruleType,
                        routingName = routingName,
                        routingRule = routingRule
                    )
                }
            }
        }
    }
}
