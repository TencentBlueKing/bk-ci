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

package com.tencent.devops.misc.service.shardingprocess

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.misc.dao.project.TxProjectMiscDao
import com.tencent.devops.misc.pojo.process.DeleteDataParam
import com.tencent.devops.misc.service.process.ProcessDataDeleteService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class ProcessShardingDataClearService {

    private val logger = LoggerFactory.getLogger(ProcessShardingDataClearService::class.java)

    @Autowired
    lateinit var txProjectMiscDao: TxProjectMiscDao

    @Autowired
    lateinit var processDataDeleteService: ProcessDataDeleteService

    /**
     * 获取DSLContext
     * @return DSLContext
     */
    abstract fun getDSLContext(): DSLContext?

    /**
     * 获取执行条件
     * @param routingRule 路由规则
     * @return 布尔值
     */
    abstract fun getExecuteFlag(routingRule: String?): Boolean

    /**
     * 按项目ID清理分片数据
     * @param projectId 项目ID
     * @param clusterName 集群名称
     * @param dataSourceName 数据源名称
     * @param broadcastTableDeleteFlag 广播表删除标识
     * @return 布尔值
     */
    fun clearShardingDataByProjectId(
        projectId: String,
        clusterName: String,
        dataSourceName: String,
        broadcastTableDeleteFlag: Boolean? = false
    ): Boolean {
        val dslContext = getDSLContext() ?: return false
        // 查询分片路由规则记录
        val shardingRuleRecord = txProjectMiscDao.getShardingRoutingRule(
            dslContext = dslContext,
            clusterName = clusterName,
            moduleCode = SystemModuleEnum.PROCESS,
            type = ShardingRuleTypeEnum.DB,
            routingName = projectId
        )
        // 检查路由规则是否允许执行删除操作
        if (!getExecuteFlag(shardingRuleRecord?.routingRule)) {
            logger.warn("Unable to delete data from data source ($dataSourceName) under cluster ($clusterName)")
            return false
        }
        val executor = Executors.newFixedThreadPool(1)
        try {
            dslContext.transaction { t ->
                val context = DSL.using(t)
                val deleteDataParam = DeleteDataParam(
                    dslContext = context,
                    projectId = projectId,
                    clusterName = clusterName,
                    dataSourceName = dataSourceName,
                    broadcastTableDeleteFlag = broadcastTableDeleteFlag
                )
                // 提交删除任务到线程池异步执行
                executor.submit {
                    processDataDeleteService.deleteProcessData(deleteDataParam)
                }
            }
        } finally {
            executor.shutdown() // 确保线程池关闭
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        }
        return true
    }
}
