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
import com.tencent.devops.project.dao.DataSourceDao
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.service.ProjectDataSourceAssignService
import com.tencent.devops.project.service.ShardingRoutingRuleService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ProjectDataSourceRandomAssignServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val dataSourceDao: DataSourceDao,
    private val shardingRoutingRuleService: ShardingRoutingRuleService
) : ProjectDataSourceAssignService {

    @Value("\${tag.prod:prod}")
    private val prodTag: String = "prod"

    @Value("\${tag.auto:auto}")
    private val autoTag: String = "auto"

    @Value("\${tag.stream:stream}")
    private val streamTag: String = "stream"

    override fun assignDataSource(
        channelCode: ProjectChannelCode,
        projectId: String,
        moduleCodes: List<SystemModuleEnum>
    ): Boolean {
        // 根据channelCode获取集群名称
        val clusterName = if (channelCode == ProjectChannelCode.BS || channelCode == ProjectChannelCode.PREBUILD) {
            prodTag
        } else if (channelCode == ProjectChannelCode.CODECC || channelCode == ProjectChannelCode.AUTO) {
            autoTag
        } else if (channelCode == ProjectChannelCode.GITCI) {
            streamTag
        } else {
            // 其他渠道的项目的接口请求默认路由到正式集群
            prodTag
        }
        moduleCodes.forEach { moduleCode ->
            // 根据模块查找还有还有空余容量的数据源
            val dataSources = dataSourceDao.listByModule(
                dslContext = dslContext,
                clusterName = clusterName,
                moduleCode = moduleCode.name,
                fullFlag = false
            )
            if (dataSources.isNullOrEmpty()) {
                return@forEach
            }
            // 从可用的数据源中随机选择一个分配给该项目
            val maxSizeIndex = dataSources.size - 1
            val randomIndex = (0..maxSizeIndex).random()
            val dataSource = dataSources[randomIndex]
            val shardingRoutingRule = ShardingRoutingRule(projectId, dataSource.dataSourceName)
            shardingRoutingRuleService.addShardingRoutingRule("system", shardingRoutingRule)
        }
        return true
    }
}
