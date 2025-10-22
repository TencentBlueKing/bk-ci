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

package com.tencent.devops.common.db.service

import com.tencent.devops.common.api.enums.CrudEnum
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.BkServiceUtil
import com.tencent.devops.common.service.utils.BkShardingRoutingCacheUtil
import com.tencent.devops.common.service.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class ShardingRoutingRuleManageService @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ShardingRoutingRuleManageService::class.java)
        private const val DEFAULT_RULE_ACTION_REDIS_CACHE_TIME = 14L // 分片规则操作信息在redis默认缓存时间，单位：天
    }

    /**
     * 处理分片路由规则本地缓存
     * @param routingName 路由规则名称
     * @param routingRule 路由规则值
     * @param actionType 操作类型
     * @return 布尔值
     */
    fun handleShardingRoutingRuleLocalCache(
        routingName: String,
        routingRule: String? = null,
        actionType: CrudEnum
    ): Boolean {
        val ip = CommonUtils.getInnerIP()
        when (actionType) {
            CrudEnum.CREATAE -> {
                routingRule?.let {
                    BkShardingRoutingCacheUtil.put(routingName, routingRule)
                }
                logger.info(
                    "[host[$ip] add shardingRoutingRule localCache success，" +
                        "params[$routingName|$routingRule]"
                )
            }

            CrudEnum.DELETE -> {
                BkShardingRoutingCacheUtil.invalidate(routingName)
                logger.info(
                    "[host[$ip] delete shardingRoutingRule localCache success，" +
                        "params[$routingName|$routingRule]"
                )
            }

            CrudEnum.UPDATE -> {
                routingRule?.let {
                    BkShardingRoutingCacheUtil.put(routingName, routingRule)
                }
                refreshShardingRoutingRuleRedisCache(routingName, actionType, ip)
                logger.info(
                    "[host[$ip] update shardingRoutingRule localCache success，" +
                        "params[$routingName|$routingRule]"
                )
            }

            else -> {}
        }
        return true
    }

    @Suppress("SpreadOperator")
    private fun refreshShardingRoutingRuleRedisCache(
        routingName: String,
        actionType: CrudEnum,
        ip: String
    ) {
        val serviceName = BkServiceUtil.findServiceName()
        val key = BkServiceUtil.getServiceRoutingRuleActionFinishKey(serviceName, routingName, actionType)
        // 获取当前微服务的IP列表
        val serviceHostKey = BkServiceUtil.getServiceHostKey(serviceName)
        val serviceIps = redisOperation.getSetMembers(serviceHostKey)
        val finishServiceIps = redisOperation.getSetMembers(key)?.toMutableSet()
        // 移除redis中当前微服务的历史IP列表
        serviceIps?.let { finishServiceIps?.removeAll(serviceIps) }
        if (!finishServiceIps.isNullOrEmpty()) {
            redisOperation.sremove(key, *finishServiceIps.toTypedArray())
        }
        // 将缓存操作完成的IP写入redis
        redisOperation.sadd(key, ip)
        // 为key设置超时时间
        redisOperation.expire(key, TimeUnit.DAYS.toSeconds(DEFAULT_RULE_ACTION_REDIS_CACHE_TIME))
    }
}
