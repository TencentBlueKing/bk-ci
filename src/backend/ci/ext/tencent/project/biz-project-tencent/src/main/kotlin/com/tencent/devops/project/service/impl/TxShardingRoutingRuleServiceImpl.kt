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

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.ShardingUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_TABLE_BUILD_NUM_REDIS_KEY
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_TABLE_BUILD_PROJECT_NUM_REDIS_KEY
import com.tencent.devops.project.dao.ShardingRoutingRuleDao
import com.tencent.devops.project.dispatch.ShardingRoutingRuleDispatcher
import com.tencent.devops.project.pojo.TableShardingConfig
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TxShardingRoutingRuleServiceImpl(
    dslContext: DSLContext,
    redisOperation: RedisOperation,
    shardingRoutingRuleDao: ShardingRoutingRuleDao,
    shardingRoutingRuleDispatcher: ShardingRoutingRuleDispatcher
) : AbsShardingRoutingRuleServiceImpl(
    dslContext,
    redisOperation,
    shardingRoutingRuleDao,
    shardingRoutingRuleDispatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TxShardingRoutingRuleServiceImpl::class.java)
        private const val DB_LOCK_KEY = "getValidDataSourceName"
        private const val TABLE_LOCK_KEY = "getValidTableName"
    }

    /**
     * 获取可用数据源名称
     * @param clusterName db集群名称
     * @param moduleCode 模块代码
     * @param ruleType 规则类型
     * @param dataSourceNames 数据源名称集合
     * @return 可用数据源名称
     */
    override fun getValidDataSourceName(
        clusterName: String,
        moduleCode: SystemModuleEnum,
        ruleType: ShardingRuleTypeEnum,
        dataSourceNames: List<String>
    ): String {
        val dbBuildNumRedisKey = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ruleType.name,
            routingName = PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY
        )
        val dbBuildProjectNumRedisKey = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ruleType.name,
            routingName = PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY
        )
        if (!redisOperation.hasKey(dbBuildNumRedisKey) || !redisOperation.hasKey(dbBuildProjectNumRedisKey)) {
            logger.info("load shardingDbBuildCache fail,randomly select an available dataSource")
            // 如果没有成功同步构建数据则从可用的数据源中随机选择一个分配给该项目
            return dataSourceNames.random()
        }
        // 锁按被更新的redis key收敛，避免不同集群、模块的规则分配相互阻塞
        val lock = RedisLock(redisOperation, "$DB_LOCK_KEY:$dbBuildNumRedisKey", 10)
        try {
            lock.lock()
            // 获取最小构建量数据源名称，同步的构建数据不可用时退化为随机分配
            return getMinBuildNumComponentName(
                componentNames = dataSourceNames,
                buildNumRedisKey = dbBuildNumRedisKey,
                buildProjectNumRedisKey = dbBuildProjectNumRedisKey
            ) ?: dataSourceNames.random()
        } finally {
            // 释放锁
            lock.unlock()
        }
    }

    /**
     * 获取可用数据库表名称
     * @param ruleType 规则类型
     * @param dataSourceName 数据源名称
     * @param tableShardingConfig 分表配置
     * @return 可用数据库表名称
     */
    override fun getValidTableName(
        ruleType: ShardingRuleTypeEnum,
        dataSourceName: String,
        tableShardingConfig: TableShardingConfig
    ): String {
        val clusterName = CommonUtils.getDbClusterName()
        val moduleCode = tableShardingConfig.moduleCode
        val tableName = tableShardingConfig.tableName
        val shardingNum = tableShardingConfig.shardingNum
        if (shardingNum < 1) {
            // 分表数量配置异常，降级到默认分表，与分片算法取不到规则时的兜底行为保持一致
            logger.warn("invalid shardingNum($shardingNum)|$clusterName|$moduleCode|$tableName")
            return "${tableName}_0"
        }
        val tableNames = (0 until shardingNum).map { "${tableName}_$it" }
        val tableBuildNumRedisKey = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ruleType.name,
            routingName = "$dataSourceName:$PROCESS_SHARDING_TABLE_BUILD_NUM_REDIS_KEY",
            tableName = tableName
        )
        val tableBuildProjectNumRedisKey = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ruleType.name,
            routingName = "$dataSourceName:$PROCESS_SHARDING_TABLE_BUILD_PROJECT_NUM_REDIS_KEY",
            tableName = tableName
        )
        if (!redisOperation.hasKey(tableBuildNumRedisKey) || !redisOperation.hasKey(tableBuildProjectNumRedisKey)) {
            logger.info("load shardingDbBuildCache fail,randomly select an available table")
            // 如果没有成功同步构建数据则从可用的数据库表中随机选择一个分配给该项目
            return tableNames.random()
        }
        // 锁按被更新的redis key收敛，避免不同数据源、分表的规则分配相互阻塞
        val lock = RedisLock(redisOperation, "$TABLE_LOCK_KEY:$tableBuildNumRedisKey", 10)
        try {
            lock.lock()
            // 获取最小构建量数据库表名称，同步的构建数据不可用时退化为随机分配
            return getMinBuildNumComponentName(
                componentNames = tableNames,
                buildNumRedisKey = tableBuildNumRedisKey,
                buildProjectNumRedisKey = tableBuildProjectNumRedisKey
            ) ?: tableNames.random()
        } finally {
            // 释放锁
            lock.unlock()
        }
    }

    /**
     * 获取最小构建量组件名称
     * @param componentNames 组件名称集合
     * @param buildNumRedisKey 构建量redis缓存key值
     * @param buildProjectNumRedisKey 构建项目数量redis缓存key值
     * @return 最小构建量组件名称，同步的构建数据不足以计算时返回null
     */
    private fun getMinBuildNumComponentName(
        componentNames: List<String>,
        buildNumRedisKey: String,
        buildProjectNumRedisKey: String
    ): String? {
        if (componentNames.isEmpty()) {
            return null
        }
        // 一次性取回整个hash，避免在锁内按组件逐个查询redis
        val buildNumMap = redisOperation.hentries(buildNumRedisKey)
        val buildProjectNumMap = redisOperation.hentries(buildProjectNumRedisKey)
        // 1、找出最小构建量组件，构建量相同时取集合中靠前的组件
        var minBuildNumComponentName = componentNames[0]
        var minBuildNumComponentBuildNum = buildNumMap.numOf(minBuildNumComponentName)
        var totalBuildNum = 0L // 总构建量
        var totalBuildProjectNum = 0L // 总构建项目数量
        componentNames.forEach { componentName ->
            val componentBuildNum = buildNumMap.numOf(componentName)
            if (componentBuildNum < minBuildNumComponentBuildNum) {
                // 构建量小于当前最小构建量组件的构建量，则需要重新赋值调整当前最小构建量组件的数据
                minBuildNumComponentName = componentName
                minBuildNumComponentBuildNum = componentBuildNum
            }
            totalBuildNum += componentBuildNum
            totalBuildProjectNum += buildProjectNumMap.numOf(componentName)
        }
        if (totalBuildProjectNum < 1) {
            // hasKey只能判断缓存存在，无法保证缓存里有当前组件的数据（如分表数量调整后规则名称对不上）
            // 此时无法计算项目平均构建量，交由调用方退化为随机分配，避免除零异常导致规则分配整体失败
            logger.warn("no available buildProjectNum data|$buildProjectNumRedisKey|$componentNames")
            return null
        }
        // 更新redis缓存数据以便能为下一个项目分配最近一段时间构建量最低的组件
        val projectAvgBuildNum = totalBuildNum / totalBuildProjectNum
        // 2、刷新redis中最小构建量组件的总构建量（总构建量加上项目的平均构建量）
        redisOperation.hset(
            key = buildNumRedisKey,
            hashKey = minBuildNumComponentName,
            values = (minBuildNumComponentBuildNum + projectAvgBuildNum).toString()
        )
        // 3、刷新redis中最小构建量组件的总构建项目量（总构建项目量加上1）
        redisOperation.hset(
            key = buildProjectNumRedisKey,
            hashKey = minBuildNumComponentName,
            values = (buildProjectNumMap.numOf(minBuildNumComponentName) + 1).toString()
        )
        return minBuildNumComponentName
    }

    /**
     * 获取组件在缓存数据中对应的数量，数据缺失或格式非法时按0处理
     */
    private fun Map<String, String>.numOf(componentName: String): Long {
        return this[componentName]?.toLongOrNull() ?: 0L
    }
}
