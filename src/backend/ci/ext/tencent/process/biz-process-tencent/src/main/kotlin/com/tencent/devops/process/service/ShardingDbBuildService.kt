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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.lambda.api.service.ServiceBkDataResource
import com.tencent.devops.lambda.pojo.bkdata.BkDataQueryParam
import com.tencent.devops.process.pojo.constant.PROCESS_SHARDING_DB_BUILD_INFO_REDIS_KEY
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
            // 1、从数据平台获取各分区库最近七天的构建数据
            val currentDate = LocalDateTime.now()
            val endDateStr = DateTimeUtil.toDateTime(currentDate, "yyyyMMdd")
            val startDate = DateTimeUtil.getFutureDate(
                localDateTime = currentDate,
                unit = Calendar.DAY_OF_MONTH,
                timeSpan = -6
            )
            val startDateStr = DateTimeUtil.formatDate(startDate, "yyyyMMdd")
            val bkDataQueryParam = BkDataQueryParam("SELECT ROUTING_RULE, TOTAL_BUILD_NUM, thedate\n" +
                "FROM 100205_T_SHARDING_DB_BUILD_DETAIL\n" +
                "WHERE thedate>='$startDateStr' AND thedate<='$endDateStr'")
            val bkDataQueryResult = client.get(ServiceBkDataResource::class).queryData(bkDataQueryParam)
            if (!bkDataQueryResult.result) {
                // 同步出错打印告警日志
                logger.warn("[$bkDataQueryParam] queryData fail, bkDataQueryResult:$bkDataQueryResult")
                return
            }
            val bkDataQueryData = bkDataQueryResult.data
            // 2、统计各分区库一周的总构建量
            val shardingDbBuildInfoMap = mutableMapOf<String, Long>()
            bkDataQueryData?.list?.forEach { dataMap ->
                val routingRule = dataMap["ROUTING_RULE"] as String  // 路由规则（实为分区库别名）
                val totalBuildNum = dataMap["TOTAL_BUILD_NUM"]?.toLong() ?: 0L // 路由规则对应项目的总构建量
                if (shardingDbBuildInfoMap.containsKey(routingRule)) {
                    // 该规则如果已经存在则追加构建量
                    val value = shardingDbBuildInfoMap[routingRule] ?: 0L
                    shardingDbBuildInfoMap[routingRule] = value + totalBuildNum
                } else {
                    shardingDbBuildInfoMap[routingRule] = totalBuildNum
                }
            }
            // 3、将各分区库最近一周的总构建量存入redis中
            shardingDbBuildInfoMap.forEach { (routingRule, totalBuildNum) ->
                redisOperation.hset(PROCESS_SHARDING_DB_BUILD_INFO_REDIS_KEY, routingRule, totalBuildNum.toString())
            }
        } catch (t: Throwable) {
            logger.warn("syncShardingDbBuildInfo failed", t)
        } finally {
            // 释放锁
            lock.unlock()
        }
    }
}
