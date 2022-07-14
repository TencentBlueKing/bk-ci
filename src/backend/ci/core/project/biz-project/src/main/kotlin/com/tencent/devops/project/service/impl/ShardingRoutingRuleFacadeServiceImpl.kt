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
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.project.service.ShardingRoutingRuleAssignService
import com.tencent.devops.project.service.ShardingRoutingRuleFacadeService
import com.tencent.devops.project.service.ShardingRoutingRuleService
import com.tencent.devops.project.service.TableShardingConfigService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ShardingRoutingRuleFacadeServiceImpl @Autowired constructor(
    private val tableShardingConfigService: TableShardingConfigService,
    private val shardingRoutingRuleService: ShardingRoutingRuleService,
    private val shardingRoutingRuleAssignService: ShardingRoutingRuleAssignService
) : ShardingRoutingRuleFacadeService {

    override fun getShardingRoutingRuleByName(
        moduleCode: SystemModuleEnum,
        ruleType: ShardingRuleTypeEnum,
        routingName: String,
        tableName: String?
    ): ShardingRoutingRule? {
        var shardingRoutingRule = shardingRoutingRuleService.getShardingRoutingRuleByName(
            moduleCode = moduleCode,
            ruleType = ruleType,
            routingName = routingName,
            tableName = tableName
        )
        if (shardingRoutingRule != null) {
            return shardingRoutingRule
        }
        // 兼容历史存量项目没有分配规则的情况，如果没有规则则主动分配规则
        if (tableName.isNullOrBlank()) {
            // 分配DB分片规则
            shardingRoutingRule = shardingRoutingRuleAssignService.assignDbShardingRoutingRule(moduleCode, routingName)
        } else {
            val clusterName = CommonUtils.getDbClusterName()
            // 获取数据库表分片规则
            val tableShardingConfig = tableShardingConfigService.getTableShardingConfigByName(
                clusterName = clusterName,
                moduleCode = moduleCode,
                tableName = tableName
            )
            tableShardingConfig?.let {
                // 查找该分片规则对应的数据源
                val dbShardingRoutingRule = shardingRoutingRuleService.getShardingRoutingRuleByName(
                    moduleCode = moduleCode,
                    ruleType = ShardingRuleTypeEnum.DB,
                    routingName = routingName
                )
                if (dbShardingRoutingRule != null) {
                    // 分片数据库表分片规则
                    shardingRoutingRule = shardingRoutingRuleAssignService.assignTableShardingRoutingRule(
                        tableShardingConfig = tableShardingConfig,
                        dataSourceName = dbShardingRoutingRule.dataSourceName,
                        routingName = routingName
                    )
                }
            }
        }
        return shardingRoutingRule
    }
}
