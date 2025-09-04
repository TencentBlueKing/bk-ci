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
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ShardingRoutingRuleDao
import com.tencent.devops.project.dao.TxShardingRoutingRuleDao
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class TxOpShardingRuleService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val shardingRoutingRuleDao: ShardingRoutingRuleDao,
    private val txShardingRoutingRuleDao: TxShardingRoutingRuleDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TxOpShardingRuleService::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
    }

    @Value("\${tag.prod:prod}")
    private val prodTag: String = "prod"

    @Value("\${tag.auto:auto}")
    private val autoTag: String = "auto"

    @Value("\${tag.stream:stream}")
    private val streamTag: String = "stream"

    fun syncDbShardingRoutingRuleInfo(
        moduleCode: SystemModuleEnum,
        clusterName: String? = null
    ): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin syncShardingRoutingRuleInfo!!")
            var offset = 0
            do {
                // 查询process服务DB分片规则记录
                val processDBShardingRoutingRuleRecords = txShardingRoutingRuleDao.getShardingRoutingRules(
                    dslContext = dslContext,
                    type = ShardingRuleTypeEnum.DB.name,
                    moduleCode = SystemModuleEnum.PROCESS.name,
                    clusterName = clusterName,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                processDBShardingRoutingRuleRecords?.forEach { shardingRoutingRuleRecord ->
                    val routingName = shardingRoutingRuleRecord.routingName
                    if (moduleCode == SystemModuleEnum.PROCESS) {
                        val projectRecord = projectDao.getByEnglishName(dslContext, routingName) ?: return@forEach
                        val channelCode = ProjectChannelCode.valueOf(projectRecord.channel)
                        // 根据channelCode获取集群名称
                        val projectClusterName = if (channelCode == ProjectChannelCode.BS ||
                            channelCode == ProjectChannelCode.PREBUILD) {
                            prodTag
                        } else if (channelCode == ProjectChannelCode.CODECC || channelCode == ProjectChannelCode.AUTO) {
                            autoTag
                        } else if (channelCode == ProjectChannelCode.GITCI) {
                            streamTag
                        } else {
                            // 其他渠道的项目的接口请求默认路由到正式集群
                            prodTag
                        }
                        // 同步历史规则集群、模块等信息
                        try {
                            txShardingRoutingRuleDao.updateShardingRoutingRule(
                                dslContext = dslContext,
                                id = shardingRoutingRuleRecord.id,
                                type = ShardingRuleTypeEnum.DB.name,
                                clusterName = projectClusterName,
                                moduleCode = moduleCode.name,
                                dataSourceName = shardingRoutingRuleRecord.routingRule
                            )
                        } catch (t: Throwable) {
                            logger.warn("syncShardingRoutingRuleInfo updateRule failed", t)
                        }
                    } else if (clusterName != null) {
                        val dbShardingRoutingRule = shardingRoutingRuleDao.get(
                            dslContext = dslContext,
                            clusterName = clusterName,
                            moduleCode = moduleCode,
                            type = ShardingRuleTypeEnum.DB,
                            routingName = shardingRoutingRuleRecord.routingName
                        )
                        if (dbShardingRoutingRule != null) {
                            // 路由规则已经存在则无需再新增
                            return@forEach
                        }
                        val shardingRoutingRule = ShardingRoutingRule(
                            clusterName = clusterName,
                            moduleCode = moduleCode,
                            dataSourceName = shardingRoutingRuleRecord.dataSourceName,
                            type = ShardingRuleTypeEnum.DB,
                            routingName = shardingRoutingRuleRecord.routingName,
                            routingRule = shardingRoutingRuleRecord.routingRule
                        )
                        try {
                            shardingRoutingRuleDao.add(
                                dslContext = dslContext,
                                userId = shardingRoutingRuleRecord.creator,
                                shardingRoutingRule = shardingRoutingRule
                            )
                        } catch (t: Throwable) {
                            logger.warn("syncShardingRoutingRuleInfo addRule failed", t)
                        }
                    }
                }
                offset += DEFAULT_PAGE_SIZE
            } while (processDBShardingRoutingRuleRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end syncShardingRoutingRuleInfo!!")
        }
        return true
    }
}
