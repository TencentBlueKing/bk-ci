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

package com.tencent.devops.misc.service.process

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.db.pojo.DATA_SOURCE_NAME_PREFIX
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.misc.pojo.process.DeleteDataParam
import com.tencent.devops.misc.service.project.TxProjectMiscService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
class TxProcessShardingDataClearService {

    private val logger = LoggerFactory.getLogger(TxProcessShardingDataClearService::class.java)

    @Autowired
    private lateinit var txProjectMiscService: TxProjectMiscService

    @Autowired
    private lateinit var processDataDeleteService: ProcessDataDeleteService

    /**
     * 按项目ID清理分片数据
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param clusterName 集群名称
     * @param dataSourceName 数据源名称
     * @param broadcastTableDeleteFlag 广播表删除标识
     * @return 布尔值
     */
    fun clearShardingDataByProjectId(
        userId: String,
        projectId: String,
        clusterName: String,
        dataSourceName: String,
        broadcastTableDeleteFlag: Boolean? = false
    ): Boolean {
        logger.info(
            "User($userId) start clearing sharding data for project $projectId, cluster $clusterName, " +
                    "dataSource $dataSourceName, broadcastTableDeleteFlag: $broadcastTableDeleteFlag"
        )
        val index = dataSourceName.removePrefix(DATA_SOURCE_NAME_PREFIX).toInt() + 1
        val beanName = "p${index}DSLContext"
        if (!SpringContextUtil.isBeanExist(beanName)) {
            return false
        }
        val dslContext = SpringContextUtil.getBean(DSLContext::class.java, beanName)
        // 查询分片路由规则记录
        val shardingRule = txProjectMiscService.getProjectShardingRoutingRule(
            clusterName = clusterName,
            moduleCode = SystemModuleEnum.PROCESS,
            type = ShardingRuleTypeEnum.DB,
            projectId = projectId
        )
        // 检查路由规则是否允许执行删除操作
        val routingRule = shardingRule?.routingRule
        val executeFlag = routingRule != dataSourceName && !routingRule.isNullOrBlank()
        if (!executeFlag) {
            logger.warn("Unable to delete data from data source ($dataSourceName) under cluster ($clusterName)")
            return false
        }
        val executor = ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(100),
            ThreadPoolExecutor.AbortPolicy()
        )
        val deleteDataParam = DeleteDataParam(
            dslContext = dslContext,
            projectId = projectId,
            broadcastTableDeleteFlag = broadcastTableDeleteFlag
        )
        // 提交删除任务到线程池异步执行
        executor.submit {
            try {
                processDataDeleteService.deleteProcessData(deleteDataParam)
            } catch (ignored: Throwable) {
                logger.error(
                    "Failed to delete process data for project $projectId, cluster $clusterName, " +
                            "dataSource $dataSourceName", ignored
                )
            } finally {
                executor.shutdown() // 确保线程池关闭
            }
        }
        logger.info(
            "User($userId) end clearing sharding data for project $projectId, cluster $clusterName, " +
                    "dataSource $dataSourceName, broadcastTableDeleteFlag: $broadcastTableDeleteFlag"
        )
        return true
    }
}
