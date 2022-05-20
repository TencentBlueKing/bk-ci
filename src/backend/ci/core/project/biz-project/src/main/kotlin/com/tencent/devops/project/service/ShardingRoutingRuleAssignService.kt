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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.project.pojo.TableShardingConfig
import com.tencent.devops.project.pojo.enums.ProjectChannelCode

interface ShardingRoutingRuleAssignService {

    /**
     * 分配分片路由规则
     * @param channelCode 渠道代码
     * @param routingName 规则名称
     * @param moduleCodes 模块代码列表
     * @return 布尔值
     */
    fun assignShardingRoutingRule(
        channelCode: ProjectChannelCode,
        routingName: String,
        moduleCodes: List<SystemModuleEnum>
    ): Boolean

    /**
     * 分配DB分片路由规则
     * @param moduleCode 模块代码
     * @param routingName 规则名称
     * @return DB分片路由规则
     */
    fun assignDbShardingRoutingRule(
        moduleCode: SystemModuleEnum,
        routingName: String
    ): ShardingRoutingRule

    /**
     * 分配数据库表分片路由规则
     * @param tableShardingConfig 数据库表分片配置
     * @param dataSourceName 数据源名称
     * @param routingName 规则名称
     * @return DB分片路由规则
     */
    fun assignTableShardingRoutingRule(
        tableShardingConfig: TableShardingConfig,
        dataSourceName: String,
        routingName: String
    ): ShardingRoutingRule
}
