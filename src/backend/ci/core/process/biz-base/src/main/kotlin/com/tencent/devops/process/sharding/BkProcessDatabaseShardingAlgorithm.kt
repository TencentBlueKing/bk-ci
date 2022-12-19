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

package com.tencent.devops.process.sharding

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.ShardingUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.DEFAULT_DATA_SOURCE_NAME
import com.tencent.devops.common.service.utils.BkShardingRoutingCacheUtil
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.project.api.service.ServiceShardingRoutingRuleResource
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm
import java.util.Properties

class BkProcessDatabaseShardingAlgorithm : StandardShardingAlgorithm<String> {

    /**
     * 分片路由算法
     * @param availableTargetNames 可用的数据源列表
     * @param shardingValue 分片规则名称
     * @return 分片规则值（数据源名称）
     */
    override fun doSharding(
        availableTargetNames: MutableCollection<String>,
        shardingValue: PreciseShardingValue<String>
    ): String {
        val routingName = shardingValue.value
        if (routingName.isNullOrBlank()) {
            // 如果分片键为空则路由到默认数据源
            return DEFAULT_DATA_SOURCE_NAME
        }
        // 获取路由规则在缓存中的key值
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = CommonUtils.getDbClusterName(),
            moduleCode = SystemModuleEnum.PROCESS.name,
            ruleType = ShardingRuleTypeEnum.DB.name,
            routingName = routingName
        )
        // 从本地缓存获取路由规则
        var routingRule = BkShardingRoutingCacheUtil.getIfPresent(key)
        if (routingRule.isNullOrBlank()) {
            // 本地缓存没有查到路由规则信息则调接口去db实时查
            val client = SpringContextUtil.getBean(Client::class.java)
            val ruleObj = client.get(ServiceShardingRoutingRuleResource::class)
                .getShardingRoutingRuleByName(
                    routingName = routingName,
                    moduleCode = SystemModuleEnum.PROCESS,
                    ruleType = ShardingRuleTypeEnum.DB
                ).data
            if (ruleObj != null) {
                routingRule = ruleObj.routingRule
                // 将路由规则信息放入本地缓存
                BkShardingRoutingCacheUtil.put(key, routingRule)
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

    override fun init(props: Properties?) = Unit

    override fun getProps(): Properties? {
        return null
    }
}
