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

import com.tencent.devops.misc.dao.process.ProcessShardingDataClearDao
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired

abstract class ProcessShardingDataClearService {

    @Autowired
    lateinit var processShardingDataClearDao: ProcessShardingDataClearDao

    /**
     * 获取DSLContext
     * @return DSLContext
     */
    abstract fun getDSLContext(): DSLContext

    /**
     * 获取执行条件
     * @param routingRule 路由规则
     * @return 布尔值
     */
    abstract fun getExecuteFlag(routingRule: String?): Boolean

    /**
     * 按项目ID清理分片数据
     * @param projectId 项目ID
     * @param routingRule 路由规则
     * @return 布尔值
     */
    fun clearShardingDataByProjectId(projectId: String, routingRule: String?): Boolean {
        if (getExecuteFlag(routingRule)) {
            getDSLContext().transaction { t ->
                val context = DSL.using(t)
                processShardingDataClearDao.deleteAuditResourceByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineFavorByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineGroupByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineInfoByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineJobMutexGroupByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineLabelByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineLabelPipelineByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineTransferHistoryByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineViewByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineViewUserLastViewByProjectId(context, projectId)
                processShardingDataClearDao.deletePipelineViewUserSettingsByProjectId(context, projectId)
                processShardingDataClearDao.deleteProjectPipelineCallbackByProjectId(context, projectId)
                processShardingDataClearDao.deleteTemplateByProjectId(context, projectId)
                processShardingDataClearDao.deleteTemplateTransferHistoryByProjectId(context, projectId)
            }
        }
        return true
    }

    /**
     * 按流水线ID清理分片数据
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param routingRule 路由规则
     * @return 布尔值
     */
    fun clearShardingDataByPipelineId(projectId: String, pipelineId: String, routingRule: String?): Boolean {
        if (getExecuteFlag(routingRule)) {
            getDSLContext().transaction { t ->
                val context = DSL.using(t)
                processShardingDataClearDao.deleteBuildHistoryByPipelineId(context, projectId, pipelineId)
                processShardingDataClearDao.deletePipelineBuildSummaryByPipelineId(context, projectId, pipelineId)
                processShardingDataClearDao.deletePipelineFailureBuildByPipelineId(context, pipelineId)
                processShardingDataClearDao.deletePipelineModelTaskByPipelineId(context, projectId, pipelineId)
                processShardingDataClearDao.deletePipelineResourceByPipelineId(context, pipelineId)
                processShardingDataClearDao.deletePipelineResourceVersionByPipelineId(context, pipelineId)
                processShardingDataClearDao.deletePipelineSettingByPipelineId(context, pipelineId)
                processShardingDataClearDao.deletePipelineSettingVersionByPipelineId(context, pipelineId)
                processShardingDataClearDao.deletePipelineWebhookQueueByPipelineId(context, pipelineId)
                processShardingDataClearDao.deleteTemplatePipelineByPipelineId(context, pipelineId)
            }
        }
        return true
    }

    /**
     * 按构建ID清理分片数据
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 构建ID
     * @param routingRule 路由规则
     * @return 布尔值
     */
    fun clearShardingDataByBuildId(
        projectId: String,
        pipelineId: String,
        buildId: String,
        routingRule: String?
    ): Boolean {
        if (getExecuteFlag(routingRule)) {
            getDSLContext().transaction { t ->
                val context = DSL.using(t)
                processShardingDataClearDao.deleteBuildDetailByBuildId(context, buildId)
                processShardingDataClearDao.deletePipelinePauseValueByBuildId(context, buildId)
                processShardingDataClearDao.deleteReportByBuildId(context, projectId, pipelineId, buildId)
            }
        }
        return true
    }
}
