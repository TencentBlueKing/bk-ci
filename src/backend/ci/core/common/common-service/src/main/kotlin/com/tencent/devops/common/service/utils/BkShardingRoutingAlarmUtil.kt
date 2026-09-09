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

package com.tencent.devops.common.service.utils

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * DB分片路由异常告警工具
 *
 * 分片算法在路由规则不可用时会静默降级到默认节点，导致路由规则记录与数据实际存放位置不一致。
 * 该工具负责把这类异常打印出来，并做告警收敛，避免在SQL热路径上刷日志。
 */
object BkShardingRoutingAlarmUtil {

    private val logger = LoggerFactory.getLogger(BkShardingRoutingAlarmUtil::class.java)

    private const val ALARM_INTERVAL_MINUTES = 10L

    private const val ALARM_CACHE_MAX_SIZE = 10000L

    // 同一条异常路由规则在窗口期内只告警一次
    private val alarmedCache = Caffeine.newBuilder()
        .maximumSize(ALARM_CACHE_MAX_SIZE)
        .expireAfterWrite(ALARM_INTERVAL_MINUTES, TimeUnit.MINUTES)
        .build<String, Boolean>()

    /**
     * 路由规则已配置但不在实际可用节点列表中时告警
     *
     * 出现该告警说明分片配置与实际数据节点不一致（如分表数量配置大于实际生成的分表数量），
     * 分片算法此时会降级路由到默认节点，导致数据写入的位置和路由规则记录的位置不一致
     * @param key 路由规则在缓存中的key
     * @param routingRule 路由规则值
     * @param availableTargetNames 实际可用的数据节点列表
     */
    fun warnUnavailableRoutingRule(
        key: String,
        routingRule: String,
        availableTargetNames: Collection<String>
    ) {
        if (alarmedCache.asMap().putIfAbsent("$key:$routingRule", true) != null) {
            return
        }
        logger.warn(
            "sharding routing rule($routingRule) of key($key) is not in " +
                "availableTargetNames($availableTargetNames), routing has been degraded to the default target"
        )
    }
}
