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

import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataClearDao
import com.tencent.devops.misc.lock.PipelineVersionLock
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ProcessDataClearService @Autowired constructor(
    private val dslContext: DSLContext,
    @Qualifier(ARCHIVE_SHARDING_DSL_CONTEXT)
    private val archiveShardingDslContext: DSLContext,
    private val processDao: ProcessDao,
    private val processDataClearDao: ProcessDataClearDao,
    private val redisOperation: RedisOperation,
    private val processRelatedPlatformDataClearService: ProcessRelatedPlatformDataClearService
) {

    /**
     * 清除流水线数据
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param archiveFlag 归档标识
     */
    fun clearPipelineData(
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean? = null
    ) {
        val finalDslContext = generateFinalDslContext(archiveFlag)
        finalDslContext.transaction { t ->
            val context = DSL.using(t)
            processDataClearDao.deletePipelineLabelByPipelineId(context, projectId, pipelineId)
            processDataClearDao.deletePipelineResourceByPipelineId(context, projectId, pipelineId)
            processDataClearDao.deletePipelineResourceVersionByPipelineId(context, projectId, pipelineId)
            processDataClearDao.deleteTemplatePipelineByPipelineId(context, projectId, pipelineId)
            processDataClearDao.deletePipelineBuildSummaryByPipelineId(context, projectId, pipelineId)
            if (archiveFlag != true) {
                processDataClearDao.deletePipelineModelTaskByPipelineId(context, projectId, pipelineId)
                processDataClearDao.deletePipelineRemoteAuthByPipelineId(context, projectId, pipelineId)
                processDataClearDao.deletePipelineSettingByPipelineId(context, projectId, pipelineId)
                processDataClearDao.deletePipelineSettingVersionByPipelineId(context, projectId, pipelineId)
                processDataClearDao.deletePipelineTimerByPipelineId(context, projectId, pipelineId)
                processDataClearDao.deletePipelineWebhookByPipelineId(context, projectId, pipelineId)
                processDataClearDao.deletePipelineTemplateAcrossInfo(context, projectId, pipelineId)
                processDataClearDao.deletePipelineViewGroup(context, projectId, pipelineId)
                // 添加删除记录，插入要实现幂等
                processDao.addPipelineDataClear(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId
                )
            }
            processDataClearDao.deletePipelineInfoByPipelineId(context, projectId, pipelineId)
        }
        processRelatedPlatformDataClearService.cleanBuildData(projectId, pipelineId)
    }

    private fun generateFinalDslContext(archiveFlag: Boolean?): DSLContext {
        val finalDslContext = if (archiveFlag == true) {
            archiveShardingDslContext
        } else {
            dslContext
        }
        return finalDslContext
    }

    /**
     * 清除流水线基础构建数据
     * @param projectId 项目ID
     * @param buildId 构建ID
     */
    fun clearBaseBuildData(projectId: String, buildId: String) {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            processDataClearDao.deleteBuildTaskByBuildId(context, projectId, buildId)
            processDataClearDao.deleteBuildVarByBuildId(context, projectId, buildId)
            processDataClearDao.deleteBuildContainerByBuildId(context, projectId, buildId)
            processDataClearDao.deleteBuildStageByBuildId(context, projectId, buildId)
        }
    }

    /**
     * 清除流水线其它构建数据
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 构建ID
     * @param archiveFlag 归档标识
     */
    fun clearOtherBuildData(
        projectId: String,
        pipelineId: String,
        buildId: String,
        archiveFlag: Boolean? = null
    ) {
        val finalDslContext = generateFinalDslContext(archiveFlag)
        finalDslContext.transaction { t ->
            val context = DSL.using(t)
            processDataClearDao.deleteBuildRecordPipelineByBuildId(context, projectId, buildId)
            processDataClearDao.deleteBuildRecordStageByBuildId(context, projectId, buildId)
            processDataClearDao.deleteBuildRecordContainerByBuildId(context, projectId, buildId)
            processDataClearDao.deleteBuildRecordTaskByBuildId(context, projectId, buildId)
            if (archiveFlag == true) {
                processDataClearDao.deleteBuildHistoryByBuildId(context, projectId, buildId)
                // 归档库构建数据清理无需执行方法后的逻辑
                return@transaction
            }
            processDataClearDao.deleteBuildDetailByBuildId(context, projectId, buildId)
            processDataClearDao.deleteReportByBuildId(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
            JooqUtils.retryWhenDeadLock {
                processDataClearDao.deletePipelineBuildTemplateAcrossInfo(context, projectId, pipelineId, buildId)
            }
            processDataClearDao.deleteBuildWebhookParameter(context, projectId, buildId)
            val version = processDao.getPipelineVersionByBuildId(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId
            )
            val pipelineVersionLock = version?.let { PipelineVersionLock(redisOperation, pipelineId, it) }
            try {
                pipelineVersionLock?.lock()
                val deleteResult = processDataClearDao.deleteBuildHistoryByBuildId(context, projectId, buildId)
                if (deleteResult == 0) {
                    // 如果删除的记录数为0则无需执行后面的逻辑
                    return@transaction
                }
                // 添加删除记录，插入要实现幂等
                processDao.addBuildHisDataClear(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                )
                // 无版本信息则无需更新计数
                if (version == null) return@transaction
                // 查询流水线版本记录
                val pipelineVersionInfo = processDao.getPipelineVersionSimple(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = version
                )
                var referCount = pipelineVersionInfo?.referCount
                referCount = if (referCount == null || referCount < 0) {
                    // 兼容老数据缺少关联构建记录的情况，全量统计关联数据数量
                    processDao.countBuildNumByVersion(
                        dslContext = context,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        version = version
                    )
                } else {
                    referCount - 1
                }
                val referFlag = referCount > 0
                // 更新流水线版本关联构建记录信息
                processDao.updatePipelineVersionReferInfo(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = version,
                    referCount = referCount,
                    referFlag = referFlag
                )
            } finally {
                pipelineVersionLock?.unlock()
            }
        }
    }

    /**
     * 清除流水线被跳过的任务数据
     * @param projectId 项目ID
     * @param buildId 构建ID
     * @param archiveFlag 归档标识
     */
    fun clearSkipRecordTaskData(
        projectId: String,
        buildId: String,
        archiveFlag: Boolean? = null
    ) {
        processDataClearDao.deleteBuildRecordTaskByBuildId(
            dslContext = generateFinalDslContext(archiveFlag),
            projectId = projectId,
            buildId = buildId,
            skipTaskDeleteFlag = true
        )
    }
}
