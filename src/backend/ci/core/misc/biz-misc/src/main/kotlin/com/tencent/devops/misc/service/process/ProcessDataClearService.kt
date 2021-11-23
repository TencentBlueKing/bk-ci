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

import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataClearDao
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProcessDataClearService @Autowired constructor(
    private val dslContext: DSLContext,
    private val processDao: ProcessDao,
    private val processDataClearDao: ProcessDataClearDao
) {

    /**
     * 清除流水线数据
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     */
    fun clearPipelineData(
        projectId: String,
        pipelineId: String
    ) {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            processDataClearDao.deletePipelineLabelByPipelineId(context, pipelineId)
            processDataClearDao.deletePipelineModelTaskByPipelineId(context, pipelineId)
            processDataClearDao.deletePipelineRemoteAuthByPipelineId(context, pipelineId)
            processDataClearDao.deletePipelineResourceByPipelineId(context, pipelineId)
            processDataClearDao.deletePipelineResourceVersionByPipelineId(context, pipelineId)
            processDataClearDao.deletePipelineSettingByPipelineId(context, pipelineId)
            processDataClearDao.deletePipelineSettingVersionByPipelineId(context, pipelineId)
            processDataClearDao.deletePipelineTimerByPipelineId(context, pipelineId)
            processDataClearDao.deletePipelineWebhookByPipelineId(context, pipelineId)
            processDataClearDao.deleteTemplatePipelineByPipelineId(context, pipelineId)
            processDataClearDao.deletePipelineBuildSummaryByPipelineId(context, pipelineId)
            // 添加删除记录，插入要实现幂等
            processDao.addPipelineDataClear(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId
            )
            processDataClearDao.deletePipelineInfoByPipelineId(context, pipelineId)
        }
    }

    /**
     * 清除流水线基础构建数据
     * @param buildId 构建ID
     */
    fun clearBaseBuildData(buildId: String) {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            processDataClearDao.deleteBuildTaskByBuildId(context, buildId)
            processDataClearDao.deleteBuildVarByBuildId(context, buildId)
            processDataClearDao.deleteBuildContainerByBuildId(context, buildId)
            processDataClearDao.deleteBuildStageByBuildId(context, buildId)
        }
    }

    /**
     * 清除流水线其它构建数据
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 构建ID
     */
    fun clearOtherBuildData(
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            processDataClearDao.deleteBuildDetailByBuildId(context, buildId)
            processDataClearDao.deleteReportByBuildId(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
            processDataClearDao.deleteMetadataByBuildId(context, buildId)
            // 添加删除记录，插入要实现幂等
            processDao.addBuildHisDataClear(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
            processDataClearDao.deleteBuildHistoryByBuildId(context, buildId)
        }
    }
}
