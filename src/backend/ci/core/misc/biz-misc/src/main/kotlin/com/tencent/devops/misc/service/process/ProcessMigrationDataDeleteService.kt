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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataDeleteDao
import com.tencent.devops.misc.lock.ProjectMigrationLock
import com.tencent.devops.misc.pojo.constant.MiscMessageCode
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistoryQueryParam
import com.tencent.devops.misc.service.project.ProjectDataMigrateHistoryService
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProcessMigrationDataDeleteService @Autowired constructor(
    private val processDao: ProcessDao,
    private val processDataDeleteDao: ProcessDataDeleteDao,
    private val projectDataMigrateHistoryService: ProjectDataMigrateHistoryService,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessMigrationDataDeleteService::class.java)
        private const val DEFAULT_PAGE_SIZE = 20
    }

    /**
     * 删除process数据库数据
     * @param dslContext jooq上下文
     * @param projectId 项目ID
     * @param targetClusterName 迁移集群
     * @param targetDataSourceName 迁移数据源名称
     */
    fun deleteProcessData(
        dslContext: DSLContext,
        projectId: String,
        targetClusterName: String,
        targetDataSourceName: String
    ) {
        val projectMigrationLock = ProjectMigrationLock(redisOperation, projectId)
        try {
            projectMigrationLock.lock()
            val moduleCode = SystemModuleEnum.PROCESS
            val queryParam = ProjectDataMigrateHistoryQueryParam(
                projectId = projectId,
                moduleCode = moduleCode,
                targetClusterName = targetClusterName,
                targetDataSourceName = targetDataSourceName
            )
            // 项目已经迁移成功则不再删除db中数据
            if (!projectDataMigrateHistoryService.isProjectDataMigrated(queryParam)) {
                deleteProcessRelData(
                    dslContext = dslContext,
                    projectId = projectId,
                    targetClusterName = targetClusterName,
                    targetDataSourceName = targetDataSourceName,
                    projectMigrationLock = projectMigrationLock
                )
            } else {
                throw ErrorCodeException(
                    errorCode = MiscMessageCode.ERROR_PROJECT_DATA_HAS_BEEN_MIGRATED_SUCCESSFULLY,
                    params = arrayOf(projectId)
                )
            }
        } finally {
            projectMigrationLock.unlock()
        }
    }

    private fun deleteProcessRelData(
        dslContext: DSLContext,
        projectId: String,
        targetClusterName: String,
        targetDataSourceName: String,
        projectMigrationLock: ProjectMigrationLock
    ) {
        var minPipelineInfoId = processDao.getMinPipelineInfoIdByProjectId(dslContext, projectId)
        do {
            val pipelineIds = processDao.getPipelineIdListByProjectId(
                dslContext = dslContext,
                projectId = projectId,
                minId = minPipelineInfoId,
                limit = DEFAULT_PAGE_SIZE.toLong()
            )?.map { it.getValue(0).toString() }
            if (pipelineIds?.isNotEmpty() == true) {
                // 重置minId的值
                minPipelineInfoId = (processDao.getPipelineInfoByPipelineId(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineIds[pipelineIds.size - 1]
                )?.id ?: 0L) + 1
            }
            deleteProjectPipelineRelData(dslContext, projectId, pipelineIds)
        } while (pipelineIds?.size == DEFAULT_PAGE_SIZE)
        deleteProjectDirectlyRelData(
            dslContext = dslContext,
            projectId = projectId,
            targetClusterName = targetClusterName,
            targetDataSourceName = targetDataSourceName,
            projectMigrationLock = projectMigrationLock
        )
    }

    /**
     * 删除项目下流水线相关的数据
     * @param dslContext jooq上下文
     * @param projectId 项目ID
     * @param pipelineIds 流水线ID集合
     */
    private fun deleteProjectPipelineRelData(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: MutableList<String>?
    ) {
        val tPipelineBuildHistory = TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY
        pipelineIds?.forEach { pipelineId ->
            var offset = 0
            do {
                val historyInfoRecords = processDao.getHistoryInfoList(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val buildIds = historyInfoRecords?.map { it[tPipelineBuildHistory.BUILD_ID] }
                if (!buildIds.isNullOrEmpty()) {
                    processDataDeleteDao.deletePipelineBuildDetail(dslContext, projectId, buildIds)
                    processDataDeleteDao.deletePipelineBuildVar(dslContext, projectId, buildIds)
                    processDataDeleteDao.deletePipelinePauseValue(dslContext, projectId, buildIds)
                    processDataDeleteDao.deletePipelineWebhookBuildParameter(dslContext, projectId, buildIds)
                    processDataDeleteDao.deletePipelineBuildRecordContainer(dslContext, projectId, buildIds)
                    processDataDeleteDao.deletePipelineBuildRecordModel(dslContext, projectId, buildIds)
                    processDataDeleteDao.deletePipelineBuildRecordStage(dslContext, projectId, buildIds)
                    processDataDeleteDao.deletePipelineBuildRecordTask(dslContext, projectId, buildIds)
                    processDataDeleteDao.deletePipelineWebhookQueue(dslContext, projectId, buildIds)
                    processDataDeleteDao.deletePipelineBuildTask(dslContext, projectId, buildIds)
                    processDataDeleteDao.deleteReport(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildIds = buildIds
                    )
                    processDataDeleteDao.deletePipelineBuildTemplateAcrossInfo(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildIds = buildIds
                    )
                }
                processDataDeleteDao.deletePipelineBuildContainer(dslContext, projectId, pipelineId)
                processDataDeleteDao.deletePipelineBuildStage(dslContext, projectId, pipelineId)
                processDataDeleteDao.deletePipelineFavor(dslContext, projectId, pipelineId)
                processDataDeleteDao.deletePipelineWebhookBuildLogDetail(dslContext, projectId, pipelineId)
                offset += DEFAULT_PAGE_SIZE
            } while (historyInfoRecords?.size == DEFAULT_PAGE_SIZE)
        }
        if (!pipelineIds.isNullOrEmpty()) {
            processDataDeleteDao.deletePipelineBuildSummary(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineInfo(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineLabelPipeline(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineModelTask(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineResource(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineResourceVersion(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineSetting(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineSettingVersion(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineBuildHistory(dslContext, projectId, pipelineIds)
        }
        logger.info("project[$projectId]|pipeline[$pipelineIds] deleteProjectPipelineRelData success!")
    }

    /**
     * 删除项目直接相关的数据
     * @param dslContext jooq上下文
     * @param projectId 项目ID
     * @param targetClusterName 迁移集群
     * @param targetDataSourceName 迁移数据源名称
     * @param projectMigrationLock 项目迁移锁
     * @return 字段列表
     */
    fun deleteProjectDirectlyRelData(
        dslContext: DSLContext,
        projectId: String,
        targetClusterName: String,
        targetDataSourceName: String,
        projectMigrationLock: ProjectMigrationLock? = null
    ) {
        if (projectMigrationLock != null) {
            val lock = ProjectMigrationLock(redisOperation, projectId)
            try {
                lock.lock()
                val queryParam = ProjectDataMigrateHistoryQueryParam(
                    projectId = projectId,
                    moduleCode = SystemModuleEnum.PROCESS,
                    targetClusterName = targetClusterName,
                    targetDataSourceName = targetDataSourceName
                )
                // 项目已经迁移成功则不再删除db中数据
                if (!projectDataMigrateHistoryService.isProjectDataMigrated(queryParam)) {
                    deleteProjectRelData(dslContext, projectId)
                } else {
                    throw ErrorCodeException(
                        errorCode = MiscMessageCode.ERROR_PROJECT_DATA_HAS_BEEN_MIGRATED_SUCCESSFULLY,
                        params = arrayOf(projectId)
                    )
                }
            } finally {
                lock.unlock()
            }
        } else {
            deleteProjectRelData(dslContext, projectId)
        }
    }

    private fun deleteProjectRelData(dslContext: DSLContext, projectId: String) {
        processDataDeleteDao.deleteAuditResource(dslContext, projectId)
        processDataDeleteDao.deletePipelineGroup(dslContext, projectId)
        processDataDeleteDao.deletePipelineJobMutexGroup(dslContext, projectId)
        processDataDeleteDao.deletePipelineLabel(dslContext, projectId)
        processDataDeleteDao.deletePipelineView(dslContext, projectId)
        processDataDeleteDao.deletePipelineViewUserLastView(dslContext, projectId)
        processDataDeleteDao.deletePipelineViewUserSettings(dslContext, projectId)
        processDataDeleteDao.deleteProjectPipelineCallback(dslContext, projectId)
        processDataDeleteDao.deleteProjectPipelineCallbackHistory(dslContext, projectId)
        processDataDeleteDao.deleteTemplate(dslContext, projectId)
        processDataDeleteDao.deleteTemplatePipeline(dslContext, projectId)
        processDataDeleteDao.deletePipelineViewGroup(dslContext, projectId)
        processDataDeleteDao.deletePipelineViewTop(dslContext, projectId)
        processDataDeleteDao.deletePipelineRecentUse(dslContext, projectId)
        logger.info("project[$projectId] deleteProjectDirectlyRelData success!")
    }
}
