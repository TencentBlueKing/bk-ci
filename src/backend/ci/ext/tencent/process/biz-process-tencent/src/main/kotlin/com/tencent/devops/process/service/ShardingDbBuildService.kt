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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.ShardingUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.lambda.api.service.ServiceBkDataResource
import com.tencent.devops.lambda.pojo.bkdata.BkDataQueryData
import com.tencent.devops.lambda.pojo.bkdata.BkDataQueryParam
import com.tencent.devops.lambda.pojo.bkdata.BkDataResult
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_TABLE_BUILD_NUM_REDIS_KEY
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_TABLE_BUILD_PROJECT_NUM_REDIS_KEY
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Calendar

@Service
class ShardingDbBuildService @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ShardingDbBuildService::class.java)
        private const val LOCK_KEY = "syncShardingDbBuildInfo"
        private const val MODULE_CODE = "MODULE_CODE"
        private const val TYPE = "TYPE"
        private const val DATA_SOURCE_NAME = "DATA_SOURCE_NAME"
        private const val TABLE_NAME = "TABLE_NAME"
        private const val ROUTING_RULE = "ROUTING_RULE"
        private const val TOTAL_BUILD_NUM = "TOTAL_BUILD_NUM"
        private const val TOTAL_PROJECT_NUM = "TOTAL_PROJECT_NUM"
    }

    @Value("\${sharding.maxBkDataSyncNum:3000}")
    private var maxBkDataSyncNum: Int = 3000

    /**
     * 同步分区库和分区表构建信息
     */
    @Scheduled(cron = "0 0 2 * * ?")
    fun syncShardingDbBuildInfo() {
        val lock = RedisLock(redisOperation, LOCK_KEY, 3000)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            val currentDate = LocalDateTime.now()
            val endDateStr = DateTimeUtil.toDateTime(currentDate, DateTimeUtil.YYYYMMDD)
            val startDate = DateTimeUtil.getFutureDate(
                localDateTime = currentDate,
                unit = Calendar.DAY_OF_MONTH,
                timeSpan = -7
            )
            val startDateStr = DateTimeUtil.formatDate(startDate, DateTimeUtil.YYYYMMDD)
            // 同步各分区库和分区表最近构建量数据
            syncShardingDbBuildNum(startDateStr, endDateStr)
            // 同步各分区库和分区表最近构建项目数量数据
            syncShardingDbBuildProjectNum(startDateStr, endDateStr)
        } catch (t: Throwable) {
            logger.warn("syncShardingDbBuildInfo failed", t)
        } finally {
            // 释放锁
            lock.unlock()
        }
    }

    /**
     * 同步各分区库最近构建量数据
     * @param startDateStr 开始时间
     * @param endDateStr 结束时间
     */
    private fun syncShardingDbBuildNum(startDateStr: String, endDateStr: String) {
        // 1、从数据平台获取各分区库、各分区表最近一段时间的构建数据
        val clusterName = CommonUtils.getDbClusterName()
        val bkDataQueryParam = BkDataQueryParam("SELECT $MODULE_CODE, $TYPE, $DATA_SOURCE_NAME, $TABLE_NAME, " +
            "$ROUTING_RULE, $TOTAL_BUILD_NUM, thedate FROM 100205_T_SHARDING_DB_BUILD_DETAIL\n" +
            "WHERE CLUSTER_NAME='$clusterName' AND thedate>='$startDateStr' AND thedate<'$endDateStr' " +
            "LIMIT $maxBkDataSyncNum")
        val bkDataQueryData = queryBkData(bkDataQueryParam).data
        // 2、统计各分区库、各分区表最近总构建量
        val shardingBuildInfoMap = mutableMapOf<String, Long>()
        val shardingRuleInfoMap = mutableMapOf<String, String>()
        bkDataQueryData?.list?.forEach { dataMap ->
            val moduleCode = dataMap[MODULE_CODE] as String // 模块代码
            val ruleType = dataMap[TYPE] as String // 分片类型
            val dataSourceName = dataMap[DATA_SOURCE_NAME] as String // 数据源名称
            val tableName = dataMap[TABLE_NAME] // 表名
            val routingRule = dataMap[ROUTING_RULE] as String // 路由规则
            val totalBuildNum = dataMap[TOTAL_BUILD_NUM]?.toLong() ?: 0L // 路由规则对应的总构建量
            val buildNumRedisKey = if (ruleType == ShardingRuleTypeEnum.DB.name) {
                ShardingUtil.getShardingRoutingRuleKey(
                    clusterName = clusterName,
                    moduleCode = moduleCode,
                    ruleType = ruleType,
                    routingName = PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY
                )
            } else {
                ShardingUtil.getShardingRoutingRuleKey(
                    clusterName = clusterName,
                    moduleCode = moduleCode,
                    ruleType = ruleType,
                    routingName = "$dataSourceName:$PROCESS_SHARDING_TABLE_BUILD_NUM_REDIS_KEY",
                    tableName = tableName
                )
            }
            shardingRuleInfoMap[buildNumRedisKey] = routingRule
            if (shardingBuildInfoMap.containsKey(buildNumRedisKey)) {
                // 该规则如果已经存在则追加数量
                val value = shardingBuildInfoMap[buildNumRedisKey] ?: 0L
                shardingBuildInfoMap[buildNumRedisKey] = value + totalBuildNum
            } else {
                shardingBuildInfoMap[buildNumRedisKey] = totalBuildNum
            }
        }
        // 3、将各分区库、各分区表最近总构建量存入redis中
        shardingBuildInfoMap.forEach { (buildNumRedisKey, totalBuildNum) ->
            redisOperation.hset(
                key = buildNumRedisKey,
                hashKey = shardingRuleInfoMap[buildNumRedisKey]!!,
                values = totalBuildNum.toString()
            )
        }
    }

    /**
     * 同步各分区库和各分区表最近构建项目数量数据
     * @param startDateStr 开始时间
     * @param endDateStr 结束时间
     */
    private fun syncShardingDbBuildProjectNum(startDateStr: String, endDateStr: String) {
        // 1、从数据平台获取各分区库、各分区表最近一段时间的构建数据
        val clusterName = CommonUtils.getDbClusterName()
        val bkDataQueryParam = BkDataQueryParam("SELECT $MODULE_CODE, $TYPE, $DATA_SOURCE_NAME, $TABLE_NAME, " +
            "$ROUTING_RULE, $TOTAL_PROJECT_NUM, thedate FROM 100205_T_SHARDING_DB_ACTIVE_PROJECT_DETAIL\n" +
            "WHERE CLUSTER_NAME='$clusterName' AND thedate>='$startDateStr' AND thedate<'$endDateStr' " +
            "LIMIT $maxBkDataSyncNum")
        val bkDataQueryData = queryBkData(bkDataQueryParam).data
        // 2、统计各分区库、各分区表最近总构建项目数量
        val shardingBuildProjectInfoMap = mutableMapOf<String, Long>()
        val shardingRuleInfoMap = mutableMapOf<String, String>()
        bkDataQueryData?.list?.forEach { dataMap ->
            val moduleCode = dataMap[MODULE_CODE] as String // 模块代码
            val ruleType = dataMap[TYPE] as String // 分片类型
            val dataSourceName = dataMap[DATA_SOURCE_NAME] as String // 数据源名称
            val tableName = dataMap[TABLE_NAME] // 表名
            val routingRule = dataMap[ROUTING_RULE] as String // 路由规则
            val totalProjectNum = dataMap[TOTAL_PROJECT_NUM]?.toLong() ?: 0L // 路由规则对应的总构建项目数量
            val buildProjectNumRedisKey = if (ruleType == ShardingRuleTypeEnum.DB.name) {
                ShardingUtil.getShardingRoutingRuleKey(
                    clusterName = clusterName,
                    moduleCode = moduleCode,
                    ruleType = ruleType,
                    routingName = PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY
                )
            } else {
                ShardingUtil.getShardingRoutingRuleKey(
                    clusterName = clusterName,
                    moduleCode = moduleCode,
                    ruleType = ruleType,
                    routingName = "$dataSourceName:$PROCESS_SHARDING_TABLE_BUILD_PROJECT_NUM_REDIS_KEY",
                    tableName = tableName
                )
            }
            shardingRuleInfoMap[buildProjectNumRedisKey] = routingRule
            if (shardingBuildProjectInfoMap.containsKey(buildProjectNumRedisKey)) {
                // 该规则如果已经存在则追加数量
                val value = shardingBuildProjectInfoMap[buildProjectNumRedisKey] ?: 0L
                shardingBuildProjectInfoMap[buildProjectNumRedisKey] = value + totalProjectNum
            } else {
                shardingBuildProjectInfoMap[buildProjectNumRedisKey] = totalProjectNum
            }
        }
        // 3、将各分区库、各分区表最近总构建项目数量存入redis中
        shardingBuildProjectInfoMap.forEach { (buildNumRedisKey, totalProjectNum) ->
            redisOperation.hset(
                key = buildNumRedisKey,
                hashKey = shardingRuleInfoMap[buildNumRedisKey]!!,
                values = totalProjectNum.toString()
            )
        }
    }

    /**
     * 从数据平台查询数据
     * @param bkDataQueryParam 查询参数
     * @return BkDataResult响应结果对象
     */
    private fun queryBkData(bkDataQueryParam: BkDataQueryParam): BkDataResult<BkDataQueryData> {
        val bkDataQueryResult = client.get(ServiceBkDataResource::class).queryData(bkDataQueryParam)
        if (!bkDataQueryResult.result) {
            // 查询接口出错打印告警日志
            logger.warn("[$bkDataQueryParam] queryData fail, bkDataQueryResult:$bkDataQueryResult")
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_REST_EXCEPTION_COMMON_TIP)
        }
        return bkDataQueryResult
    }
}
