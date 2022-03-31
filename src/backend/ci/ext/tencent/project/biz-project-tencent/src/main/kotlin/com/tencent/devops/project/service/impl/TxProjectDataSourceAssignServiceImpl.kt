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

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY
import com.tencent.devops.project.dao.DataSourceDao
import com.tencent.devops.project.service.ShardingRoutingRuleService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TxProjectDataSourceAssignServiceImpl(
    dslContext: DSLContext,
    dataSourceDao: DataSourceDao,
    shardingRoutingRuleService: ShardingRoutingRuleService,
    private val redisOperation: RedisOperation
) : AbsProjectDataSourceAssignServiceImpl(dslContext, dataSourceDao, shardingRoutingRuleService) {

    companion object {
        private val logger = LoggerFactory.getLogger(TxProjectDataSourceAssignServiceImpl::class.java)
        private const val LOCK_KEY = "getValidDataSourceName"
    }

    /**
     * 获取可用数据源名称
     * @param clusterName db集群名称
     * @param dataSourceNames 数据源名称集合
     * @return 可用数据源名称
     */
    override fun getValidDataSourceName(clusterName: String, dataSourceNames: List<String>): String {
        if (
            !redisOperation.hasKey(getKeyByClusterName(PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY, clusterName)) ||
            !redisOperation.hasKey(getKeyByClusterName(PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY, clusterName))
        ) {
            logger.info("load shardingDbBuildCache fail,randomly select an available dataSource")
            // 如果没有成功同步构建数据则从可用的数据源中随机选择一个分配给该项目
            val maxSizeIndex = dataSourceNames.size - 1
            val randomIndex = (0..maxSizeIndex).random()
            return dataSourceNames[randomIndex]
        }
        val lock = RedisLock(redisOperation, LOCK_KEY, 10)
        try {
            lock.lock()
            // 获取最小构建量数据源名称
            return getMinBuildNumDataSourceName(clusterName, dataSourceNames)
        } finally {
            // 释放锁
            lock.unlock()
        }
    }

    /**
     * 获取最小构建量数据源名称
     * @param clusterName db集群名称
     * @param dataSourceNames 数据源名称集合
     * @return 最小构建量数据源名称
     */
    private fun getMinBuildNumDataSourceName(clusterName: String, dataSourceNames: List<String>): String {
        var totalBuildNum = 0L // 总构建量
        var totalBuildProjectNum = 0L // 总构建项目数量
        // 1、找出低于平均构建量的最小构建量数据源(默认取第一个数据源作为最小数据源)
        var minBuildNumDataSourceName = dataSourceNames[0]
        var minBuildNumDataSourceBuildNum = redisOperation.hget(
            key = getKeyByClusterName(PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY, clusterName),
            hashKey = minBuildNumDataSourceName
        )?.toLong() ?: 0L
        var minBuildNumDataSourceBuildProjectNum = redisOperation.hget(
            key = getKeyByClusterName(PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY, clusterName),
            hashKey = minBuildNumDataSourceName
        )?.toLong() ?: 0L
        // 从redis中获取数据源构建数据和构建项目数据来得到最小构建量数据源
        dataSourceNames.forEach { dataSourceName ->
            var changeFlag = false // 最小构建量数据源是否需要调整标识
            val dataSourceBuildNum = if (dataSourceName != minBuildNumDataSourceName) {
                val buildNum = redisOperation.hget(
                    key = getKeyByClusterName(PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY, clusterName),
                    hashKey = dataSourceName
                )?.toLong() ?: 0L
                if (buildNum < minBuildNumDataSourceBuildNum) {
                    // 构建量小于当前最小构建量数据源的构建量，则需要重新赋值调整当前最小构建量数据源的数据
                    minBuildNumDataSourceName = dataSourceName
                    minBuildNumDataSourceBuildNum = buildNum
                    changeFlag = true
                }
                buildNum
            } else {
                minBuildNumDataSourceBuildNum
            }
            totalBuildNum += dataSourceBuildNum
            val dataSourceBuildProjectNum = if (dataSourceName != minBuildNumDataSourceName) {
                val buildProjectNum = redisOperation.hget(
                    key = getKeyByClusterName(PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY, clusterName),
                    hashKey = dataSourceName
                )?.toLong() ?: 0L
                if (changeFlag) {
                    // 最小构建量数据源构建项目数量需要重新赋值调整
                    minBuildNumDataSourceBuildProjectNum = buildProjectNum
                }
                buildProjectNum
            } else {
                minBuildNumDataSourceBuildProjectNum
            }
            totalBuildProjectNum += dataSourceBuildProjectNum
        }
        // 更新数据源redis缓存数据以便能为下一个项目分配最近一段时间构建量最低的数据源
        updateDataSourceRedisCache(
            clusterName = clusterName,
            totalBuildNum = totalBuildNum,
            totalBuildProjectNum = totalBuildProjectNum,
            minBuildNumDataSourceName = minBuildNumDataSourceName,
            minBuildNumDataSourceBuildNum = minBuildNumDataSourceBuildNum,
            minBuildNumDataSourceBuildProjectNum = minBuildNumDataSourceBuildProjectNum
        )
        return minBuildNumDataSourceName
    }

    /**
     * 更新数据源redis缓存数据
     * @param clusterName db集群名称
     * @param totalBuildNum 总构建量
     * @param totalBuildProjectNum 总构建项目数量
     * @param minBuildNumDataSourceName 最小构建量数据源名称
     * @param minBuildNumDataSourceBuildNum 最小构建量数据源构建量
     * @param minBuildNumDataSourceBuildProjectNum 最小构建量数据源构建项目数量
     */
    private fun updateDataSourceRedisCache(
        clusterName: String,
        totalBuildNum: Long,
        totalBuildProjectNum: Long,
        minBuildNumDataSourceName: String,
        minBuildNumDataSourceBuildNum: Long,
        minBuildNumDataSourceBuildProjectNum: Long
    ) {
        // 计算最近一段时间每个项目平均构建量
        val projectAvgBuildNum = totalBuildNum / totalBuildProjectNum
        // 2、刷新redis中最小构建量数据源的总构建量（总构建量加上项目的平均构建量）
        redisOperation.hset(
            key = getKeyByClusterName(PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY, clusterName),
            hashKey = minBuildNumDataSourceName,
            values = (minBuildNumDataSourceBuildNum + projectAvgBuildNum).toString()
        )
        // 3、刷新redis中最小构建量数据源的总构建项目量（总构建项目量加上1）
        redisOperation.hset(
            key = getKeyByClusterName(PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY, clusterName),
            hashKey = minBuildNumDataSourceName,
            values = (minBuildNumDataSourceBuildProjectNum + 1).toString()
        )
    }

    /**
     * 根据db集群名称获取真实的key值
     * @param key 原始key
     * @return 真实的key值
     */
    private fun getKeyByClusterName(key: String, clusterName: String): String {
        return "$clusterName:$key"
    }
}
