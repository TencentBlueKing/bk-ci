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
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.TxShardingRoutingRuleDao
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class TxOpShardingRuleService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val txShardingRoutingRuleDao: TxShardingRoutingRuleDao,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TxOpShardingRuleService::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
    }

    fun syncShardingRoutingRuleInfo(): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin syncShardingRoutingRuleInfo!!")
            var offset = 0
            do {
                // 查询分片规则记录
                val shardingRoutingRuleRecords = txShardingRoutingRuleDao.getShardingRoutingRules(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                shardingRoutingRuleRecords?.forEach { shardingRoutingRuleRecord ->
                    val routingName = shardingRoutingRuleRecord.routingName
                    val projectRecord = projectDao.getByEnglishName(dslContext, routingName) ?: return@forEach
                    val channelCode = ProjectChannelCode.valueOf(projectRecord.channel)
                    val clusterName = if (channelCode == ProjectChannelCode.BS ||
                        channelCode == ProjectChannelCode.PREBUILD) {
                        "prod"
                    } else if (channelCode == ProjectChannelCode.CODECC || channelCode == ProjectChannelCode.AUTO) {
                        "auto"
                    } else if (channelCode == ProjectChannelCode.GITCI) {
                        "stream"
                    } else {
                        // 其他渠道的项目的接口请求默认路由到正式集群
                        "prod"
                    }
                    // 同步历史规则集群、模块等信息
                    txShardingRoutingRuleDao.updateShardingRoutingRule(
                        dslContext = dslContext,
                        id = shardingRoutingRuleRecord.id,
                        type = ShardingRuleTypeEnum.DB.name,
                        clusterName = clusterName,
                        moduleCode = SystemModuleEnum.PROCESS.name
                    )
                }
                offset += DEFAULT_PAGE_SIZE
            } while (shardingRoutingRuleRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end syncShardingRoutingRuleInfo!!")
        }
        return true
    }

    fun clearInvalidRuleRedisInfo(): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin clearInvalidRuleRedisInfo!!")
            var offset = 0
            do {
                // 查询分片规则记录
                val shardingRoutingRuleRecords = txShardingRoutingRuleDao.getShardingRoutingRules(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                shardingRoutingRuleRecords?.forEach { shardingRoutingRuleRecord ->
                    val routingName = shardingRoutingRuleRecord.routingName
                    redisOperation.delete("SHARDING_ROUTING_RULE:$routingName")
                }
                offset += DEFAULT_PAGE_SIZE
            } while (shardingRoutingRuleRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end clearInvalidRuleRedisInfo!!")
        }
        return true
    }
}
