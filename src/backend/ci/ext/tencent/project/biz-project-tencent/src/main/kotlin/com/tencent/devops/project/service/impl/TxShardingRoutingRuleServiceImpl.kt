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
import com.tencent.devops.project.pojo.TableShardingConfig
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TxShardingRoutingRuleServiceImpl(
    dslContext: DSLContext,
    redisOperation: RedisOperation,
    shardingRoutingRuleDao: ShardingRoutingRuleDao
) : AbsShardingRoutingRuleServiceImpl(
    dslContext,
    redisOperation,
    shardingRoutingRuleDao
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
     * @param dataSourceNames 数据源名称集合
     * @return 可用数据源名称
     */
    override fun getValidDataSourceName(
        clusterName: String,
        moduleCode: SystemModuleEnum,
        dataSourceNames: List<String>
    ): String {
        val dbBuildNumRedisKey = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ShardingRuleTypeEnum.DB.name,
            routingName = PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY
        )
        val dbBuildProjectNumRedisKey = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ShardingRuleTypeEnum.DB.name,
            routingName = PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY
        )
        if (!redisOperation.hasKey(dbBuildNumRedisKey) || !redisOperation.hasKey(dbBuildProjectNumRedisKey)) {
            logger.info("load shardingDbBuildCache fail,randomly select an available dataSource")
            // 如果没有成功同步构建数据则从可用的数据源中随机选择一个分配给该项目
            val maxSizeIndex = dataSourceNames.size - 1
            val randomIndex = (0..maxSizeIndex).random()
            return dataSourceNames[randomIndex]
        }
        val lock = RedisLock(redisOperation, DB_LOCK_KEY, 10)
        try {
            lock.lock()
            // 获取最小构建量数据源名称
            return getMinBuildNumComponentName(
                componentNames = dataSourceNames,
                buildNumRedisKey = dbBuildNumRedisKey,
                buildProjectNumRedisKey = dbBuildProjectNumRedisKey
            )
        } finally {
            // 释放锁
            lock.unlock()
        }
    }

    /**
     * 获取可用数据库表名称
     * @param dataSourceName 数据源名称
     * @param tableShardingConfig 分表配置
     * @return 可用数据库表名称
     */
    override fun getValidTableName(
        dataSourceName: String,
        tableShardingConfig: TableShardingConfig
    ): String {
        val clusterName = CommonUtils.getDbClusterName()
        val moduleCode = tableShardingConfig.moduleCode
        val tableName = tableShardingConfig.tableName
        val tableBuildNumRedisKey = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ShardingRuleTypeEnum.TABLE.name,
            routingName = "$dataSourceName:$PROCESS_SHARDING_TABLE_BUILD_NUM_REDIS_KEY",
            tableName = tableName
        )
        val tableBuildProjectNumRedisKey = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ShardingRuleTypeEnum.DB.name,
            routingName = "$dataSourceName:$PROCESS_SHARDING_TABLE_BUILD_PROJECT_NUM_REDIS_KEY",
            tableName = tableName
        )
        val shardingNum = tableShardingConfig.shardingNum
        val maxSizeIndex = shardingNum - 1
        if (!redisOperation.hasKey(tableBuildNumRedisKey) || !redisOperation.hasKey(tableBuildProjectNumRedisKey)) {
            logger.info("load shardingDbBuildCache fail,randomly select an available table")
            // 如果没有成功同步构建数据则从可用的数据库表中随机选择一个分配给该项目
            val randomIndex = (0..maxSizeIndex).random()
            return "${tableName}_$randomIndex"
        }
        val lock = RedisLock(redisOperation, TABLE_LOCK_KEY, 10)
        try {
            lock.lock()
            // 获取最小构建量数据库表名称
            val tableNames = mutableListOf<String>()
            for (index in 0..maxSizeIndex) {
                tableNames.add("${tableName}_$index")
            }
            return getMinBuildNumComponentName(
                componentNames = tableNames,
                buildNumRedisKey = tableBuildNumRedisKey,
                buildProjectNumRedisKey = tableBuildProjectNumRedisKey
            )
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
     * @return 最小构建量组件名称
     */
    private fun getMinBuildNumComponentName(
        componentNames: List<String>,
        buildNumRedisKey: String,
        buildProjectNumRedisKey: String
    ): String {
        // 1、找出低于平均构建量的最小构建量组件
        var minBuildNumComponentName = componentNames[0]
        var totalBuildNum = 0L // 总构建量
        var totalBuildProjectNum = 0L // 总构建项目数量
        var minBuildNumComponentBuildNum = redisOperation.hget(
            key = buildNumRedisKey,
            hashKey = minBuildNumComponentName
        )?.toLong() ?: 0L
        var minBuildNumComponentBuildProjectNum = redisOperation.hget(
            key = buildProjectNumRedisKey,
            hashKey = minBuildNumComponentName
        )?.toLong() ?: 0L
        // 从redis中获取组件构建数据和构建项目数据来得到最小构建量组件
        componentNames.forEach { componentName ->
            var changeFlag = false // 最小构建量组件是否需要调整标识
            val componentBuildNum = if (componentName != minBuildNumComponentName) {
                val buildNum = redisOperation.hget(
                    key = buildNumRedisKey,
                    hashKey = componentName
                )?.toLong() ?: 0L
                if (buildNum < minBuildNumComponentBuildNum) {
                    // 构建量小于当前最小构建量组件的构建量，则需要重新赋值调整当前最小构建量组件的数据
                    minBuildNumComponentBuildNum = buildNum
                    changeFlag = true
                }
                buildNum
            } else {
                minBuildNumComponentBuildNum
            }
            totalBuildNum += componentBuildNum
            val componentBuildProjectNum = if (componentName != minBuildNumComponentName) {
                val buildProjectNum = redisOperation.hget(
                    key = buildProjectNumRedisKey,
                    hashKey = componentName
                )?.toLong() ?: 0L
                if (changeFlag) {
                    // 最小构建量组件构建项目数量需要重新赋值调整
                    minBuildNumComponentName = componentName
                    minBuildNumComponentBuildProjectNum = buildProjectNum
                }
                buildProjectNum
            } else {
                minBuildNumComponentBuildProjectNum
            }
            totalBuildProjectNum += componentBuildProjectNum
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
            values = (minBuildNumComponentBuildProjectNum + 1).toString()
        )
        return minBuildNumComponentName
    }
}
