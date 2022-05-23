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

import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.DataSourceDao
import com.tencent.devops.project.pojo.TableShardingConfig
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.service.ShardingRoutingRuleAssignService
import com.tencent.devops.project.service.ShardingRoutingRuleService
import com.tencent.devops.project.service.TableShardingConfigService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ShardingRoutingRuleAssignServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val dataSourceDao: DataSourceDao,
    private val shardingRoutingRuleService: ShardingRoutingRuleService,
    private val tableShardingConfigService: TableShardingConfigService
) : ShardingRoutingRuleAssignService {

    companion object {
        private val logger = LoggerFactory.getLogger(ShardingRoutingRuleAssignServiceImpl::class.java)
        private const val DEFAULT_DATA_SOURCE_NAME = "ds_0"
    }

    @Value("\${sharding.database.assign.fusibleSwitch:true}")
    private val assignDbFusibleSwitch: Boolean = true

    /**
     * 为项目分配分片路由规则
     * @param channelCode 渠道代码
     * @param routingName 项目ID
     * @param moduleCodes 模块代码列表
     * @return 布尔值
     */
    override fun assignShardingRoutingRule(
        channelCode: ProjectChannelCode,
        routingName: String,
        moduleCodes: List<SystemModuleEnum>
    ): Boolean {
        // 获取集群名称
        val clusterName = CommonUtils.getDbClusterName()
        moduleCodes.forEach { moduleCode ->
            // 1、为微服务模块分配db分片规则
            val dbShardingRoutingRule = assignDbShardingRoutingRule(moduleCode, routingName)

            // 2、为微服务模块分配数据库表分片规则
            val tableShardingConfigs = tableShardingConfigService.listByModule(
                dslContext = dslContext,
                clusterName = clusterName,
                moduleCode = moduleCode
            )
            tableShardingConfigs?.forEach { tableShardingConfig ->
                assignTableShardingRoutingRule(
                    tableShardingConfig = tableShardingConfig,
                    dataSourceName = dbShardingRoutingRule.dataSourceName,
                    routingName = routingName
                )
            }
        }
        return true
    }

    override fun assignDbShardingRoutingRule(
        moduleCode: SystemModuleEnum,
        routingName: String
    ): ShardingRoutingRule {
        val clusterName = CommonUtils.getDbClusterName()
        var validDataSourceName = DEFAULT_DATA_SOURCE_NAME
        // 根据模块查找还有空余容量的数据源
        val dataSourceNames = dataSourceDao.listByModule(
            dslContext = dslContext,
            clusterName = clusterName,
            moduleCode = moduleCode,
            fullFlag = false
        )?.map { it.dataSourceName }

        if (dataSourceNames.isNullOrEmpty()) {
            logger.warn("[$clusterName]$moduleCode has no dataSource available")
            if (assignDbFusibleSwitch) {
                // 当分配db的熔断开关打开时，如果没有可用的数据源则报错
                throw ErrorCodeException(errorCode = ProjectMessageCode.PROJECT_ASSIGN_DATASOURCE_FAIL)
            }
        } else {
            // 获取可用数据源名称
            validDataSourceName = shardingRoutingRuleService.getValidDataSourceName(
                clusterName = clusterName,
                moduleCode = moduleCode,
                dataSourceNames = dataSourceNames
            )
        }
        val dbShardingRoutingRule = ShardingRoutingRule(
            clusterName = clusterName,
            moduleCode = moduleCode,
            dataSourceName = validDataSourceName,
            type = ShardingRuleTypeEnum.DB,
            routingName = routingName,
            routingRule = validDataSourceName
        )
        // 保存db分片规则
        shardingRoutingRuleService.addShardingRoutingRule(SYSTEM, dbShardingRoutingRule)
        return dbShardingRoutingRule
    }

    override fun assignTableShardingRoutingRule(
        tableShardingConfig: TableShardingConfig,
        dataSourceName: String,
        routingName: String
    ): ShardingRoutingRule {
        // 获取可用数据表真实名称
        val validTableName = shardingRoutingRuleService.getValidTableName(dataSourceName, tableShardingConfig)
        val tableShardingRoutingRule = ShardingRoutingRule(
            clusterName = tableShardingConfig.clusterName,
            moduleCode = tableShardingConfig.moduleCode,
            dataSourceName = dataSourceName,
            tableName = tableShardingConfig.tableName,
            type = ShardingRuleTypeEnum.TABLE,
            routingName = routingName,
            routingRule = validTableName
        )
        // 保存数据库表分片规则
        shardingRoutingRuleService.addShardingRoutingRule(SYSTEM, tableShardingRoutingRule)
        return tableShardingRoutingRule
    }
}
