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
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.lambda.api.service.ServiceBkDataResource
import com.tencent.devops.lambda.pojo.bkdata.BkDataQueryData
import com.tencent.devops.lambda.pojo.bkdata.BkDataQueryParam
import com.tencent.devops.lambda.pojo.bkdata.BkDataResult
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
    }

    /**
     * 同步分区库构建信息
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
            // 同步各分区库最近构建量数据
            syncShardingDbBuildNum(startDateStr, endDateStr)
            // 同步各分区库最近构建项目数量数据
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
        // 1、从数据平台获取各分区库最近一段时间的构建数据
        val keyName = "ROUTING_RULE"
        val valueName = "TOTAL_BUILD_NUM"
        val bkDataQueryParam = BkDataQueryParam("SELECT $keyName, $valueName, thedate\n" +
            "FROM 100205_T_SHARDING_DB_BUILD_DETAIL\n" +
            "WHERE thedate>='$startDateStr' AND thedate<'$endDateStr'")
        val bkDataQueryData = queryBkData(bkDataQueryParam).data
        // 2、统计各分区库最近总构建量
        val shardingDbBuildNumMap = generateShardingDbBuildInfoMap(bkDataQueryData, keyName, valueName)
        // 3、将各分区库最近总构建量存入redis中
        shardingDbBuildNumMap.forEach { (routingRule, totalBuildNum) ->
            redisOperation.hset(
                key = PROCESS_SHARDING_DB_BUILD_NUM_REDIS_KEY,
                hashKey = routingRule,
                values = totalBuildNum.toString(),
                isDistinguishCluster = true
            )
        }
    }

    /**
     * 同步各分区库最近构建项目数量数据
     * @param startDateStr 开始时间
     * @param endDateStr 结束时间
     */
    private fun syncShardingDbBuildProjectNum(startDateStr: String, endDateStr: String) {
        // 1、从数据平台获取各分区库最近一段时间的构建数据
        val keyName = "ROUTING_RULE"
        val valueName = "TOTAL_PROJECT_NUM"
        val bkDataQueryParam = BkDataQueryParam("SELECT $keyName, $valueName, thedate\n" +
            "FROM 100205_T_SHARDING_DB_ACTIVE_PROJECT_DETAIL\n" +
            "WHERE thedate>='$startDateStr' AND thedate<'$endDateStr'")
        val bkDataQueryData = queryBkData(bkDataQueryParam).data
        // 2、统计各分区库最近构建项目总数量
        val shardingDbBuildProjectNumMap = generateShardingDbBuildInfoMap(bkDataQueryData, keyName, valueName)
        // 3、将各分区库最近构建项目总数量存入redis中
        shardingDbBuildProjectNumMap.forEach { (routingRule, totalBuildProjectNum) ->
            redisOperation.hset(
                key = PROCESS_SHARDING_DB_BUILD_PROJECT_NUM_REDIS_KEY,
                hashKey = routingRule,
                values = totalBuildProjectNum.toString(),
                isDistinguishCluster = true
            )
        }
    }

    /**
     * 根据查询数据生成构建信息map集合
     * @param bkDataQueryData 数据平台查询结果数据
     * @param keyName 构建信息map集合key名称
     * @param valueName 构建信息map集合value名称
     * @return 构建信息map集合
     */
    private fun generateShardingDbBuildInfoMap(
        bkDataQueryData: BkDataQueryData?,
        keyName: String,
        valueName: String
    ): MutableMap<String, Long> {
        val shardingDbBuildInfoMap = mutableMapOf<String, Long>()
        bkDataQueryData?.list?.forEach { dataMap ->
            val routingRule = dataMap[keyName] as String  // 路由规则（实为分区库别名）
            val totalNum = dataMap[valueName]?.toLong() ?: 0L // 路由规则对应的总数量
            if (shardingDbBuildInfoMap.containsKey(routingRule)) {
                // 该规则如果已经存在则追加构建项目数量
                val value = shardingDbBuildInfoMap[routingRule] ?: 0L
                shardingDbBuildInfoMap[routingRule] = value + totalNum
            } else {
                shardingDbBuildInfoMap[routingRule] = totalNum
            }
        }
        return shardingDbBuildInfoMap
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
