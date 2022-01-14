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

package com.tencent.devops.lambda.sharding

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.DEFAULT_DATA_SOURCE_NAME
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.BkShardingRoutingCacheUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.project.api.service.ServiceShardingRoutingRuleResource
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm

class BkProcessDatabaseShardingAlgorithm : StandardShardingAlgorithm<String> {

    override fun doSharding(
        availableTargetNames: MutableCollection<String>,
        shardingValue: PreciseShardingValue<String>
    ): String {
        val routingName = shardingValue.value
        // 从本地缓存获取路由规则
        var routingRule = BkShardingRoutingCacheUtil.getIfPresent(routingName)
        if (routingRule == null) {
            val redisOperation: RedisOperation = SpringContextUtil.getBean(RedisOperation::class.java)
            // 获取没有配置规则时的操作开关 --临时代码，规则全切换后删除
            val noShardingRuleSwitch = redisOperation.get("noShardingRuleSwitch")?.toBoolean() ?: false
            if (noShardingRuleSwitch) {
                // 先从redis去取规则
                val cacheRoutingRuleValue = redisOperation.get("SHARDING_ROUTING_RULE:$routingName")
                if (cacheRoutingRuleValue?.isBlank() == true) {
                    // redis中的规则为空字符串则路由到默认数据源
                    return DEFAULT_DATA_SOURCE_NAME
                }
            }
            // 本地缓存没有查到路由规则信息则调接口去db实时查
            val client = SpringContextUtil.getBean(Client::class.java)
            val ruleObj = client.get(ServiceShardingRoutingRuleResource::class)
                .getShardingRoutingRuleByName(routingName).data
            if (ruleObj != null) {
                routingRule = ruleObj.routingRule
                // 将路由规则信息放入本地缓存
                BkShardingRoutingCacheUtil.put(routingName, routingRule)
            } else {
                if (noShardingRuleSwitch) {
                    // 规则写入redis缓存，30分钟后失效 --临时代码，规则全切换后删除
                    redisOperation.set(
                        key = "SHARDING_ROUTING_RULE:$routingName",
                        value = "",
                        expiredInSecond = 1800
                    )
                }
            }
        }
        if (routingRule.isNullOrBlank() || !availableTargetNames.contains(routingRule)) {
            // 没有配置路由规则则路由到默认数据源
            return DEFAULT_DATA_SOURCE_NAME
        }
        return routingRule
    }

    override fun doSharding(
        availableTargetNames: MutableCollection<String>,
        shardingValue: RangeShardingValue<String>
    ): MutableCollection<String> {
        return availableTargetNames
    }

    override fun getType(): String? {
        return null
    }

    override fun init() = Unit
}
