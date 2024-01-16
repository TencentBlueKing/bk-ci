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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataDeleteDao
import com.tencent.devops.misc.lock.MigrationLock
import com.tencent.devops.misc.pojo.constant.MiscMessageCode
import com.tencent.devops.misc.pojo.process.DeleteMigrationDataParam
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistoryQueryParam
import com.tencent.devops.misc.service.project.ProjectDataMigrateHistoryService
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ProcessMigrationDataDeleteService @Autowired constructor(
    private val processDao: ProcessDao,
    private val processDataDeleteDao: ProcessDataDeleteDao,
    private val projectDataMigrateHistoryService: ProjectDataMigrateHistoryService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessMigrationDataDeleteService::class.java)
        private const val DEFAULT_PAGE_SIZE = 20
    }

    @Value("\${process.clearBaseBuildData:false}")
    private val clearBaseBuildData: Boolean = false

    /**
     * 删除process数据库数据
     * @param deleteMigrationDataParam 删除迁移数据参数
     */
    fun deleteProcessData(
        deleteMigrationDataParam: DeleteMigrationDataParam
    ) {
        val migrationLock = deleteMigrationDataParam.migrationLock
        val projectId = deleteMigrationDataParam.projectId
        try {
            migrationLock?.lock()
            val moduleCode = SystemModuleEnum.PROCESS
            val queryParam = ProjectDataMigrateHistoryQueryParam(
                projectId = projectId,
                pipelineId = deleteMigrationDataParam.pipelineId,
                moduleCode = moduleCode,
                targetClusterName = deleteMigrationDataParam.targetClusterName,
                targetDataSourceName = deleteMigrationDataParam.targetDataSourceName
            )
            // 判断是否能删除db中数据
            if (projectDataMigrateHistoryService.isDataCanDelete(queryParam)) {
                deleteProcessRelData(deleteMigrationDataParam)
            } else {
                throw ErrorCodeException(
                    errorCode = MiscMessageCode.ERROR_PROJECT_DATA_REPEAT_MIGRATE,
                    params = arrayOf(projectId),
                    defaultMessage = I18nUtil.getCodeLanMessage(
                        messageCode = MiscMessageCode.ERROR_PROJECT_DATA_REPEAT_MIGRATE,
                        params = arrayOf(projectId)
                    )
                )
            }
        } finally {
            migrationLock?.unlock()
        }
    }

    private fun deleteProcessRelData(
        deleteMigrationDataParam: DeleteMigrationDataParam
    ) {
        val dslContext = deleteMigrationDataParam.dslContext
        val projectId = deleteMigrationDataParam.projectId
        val pipelineId = deleteMigrationDataParam.pipelineId
        val broadcastTableDeleteFlag = deleteMigrationDataParam.broadcastTableDeleteFlag
        if (!pipelineId.isNullOrBlank()) {
            // 如果流水线ID不为空，只需清理与流水线直接相关的数据
            deleteProjectPipelineRelData(
                dslContext = dslContext,
                projectId = projectId,
                pipelineIds = mutableListOf(pipelineId),
                broadcastTableDeleteFlag = broadcastTableDeleteFlag,
                archivePipelineFlag = deleteMigrationDataParam.archivePipelineFlag
            )
            return
        }
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
            deleteProjectPipelineRelData(
                dslContext = dslContext,
                projectId = projectId,
                pipelineIds = pipelineIds,
                broadcastTableDeleteFlag = broadcastTableDeleteFlag
            )
        } while (pipelineIds?.size == DEFAULT_PAGE_SIZE)
        // 如果流水线ID为空，与项目直接相关的数据也需要清理
        deleteProjectDirectlyRelData(
            dslContext = dslContext,
            projectId = projectId,
            targetClusterName = deleteMigrationDataParam.targetClusterName,
            targetDataSourceName = deleteMigrationDataParam.targetDataSourceName
        )
    }

    /**
     * 删除项目下流水线相关的数据
     * @param dslContext jooq上下文
     * @param projectId 项目ID
     * @param pipelineIds 流水线ID集合
     * @param broadcastTableDeleteFlag 广播表删除标识
     * @param archivePipelineFlag 归档流水线标识
     */
    private fun deleteProjectPipelineRelData(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: MutableList<String>?,
        broadcastTableDeleteFlag: Boolean? = true,
        archivePipelineFlag: Boolean? = null
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
                deletePipelineBuildDataByBuilds(
                    buildIds = buildIds,
                    archivePipelineFlag = archivePipelineFlag,
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId
                )
                deletePipelineBuildDataByPipelineId(
                    archivePipelineFlag = archivePipelineFlag,
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    broadcastTableDeleteFlag = broadcastTableDeleteFlag
                )
                offset += DEFAULT_PAGE_SIZE
            } while (historyInfoRecords?.size == DEFAULT_PAGE_SIZE)
        }
        if (!pipelineIds.isNullOrEmpty()) {
            if (archivePipelineFlag != true) {
                processDataDeleteDao.deletePipelineModelTask(dslContext, projectId, pipelineIds)
                processDataDeleteDao.deletePipelineSetting(dslContext, projectId, pipelineIds)
                processDataDeleteDao.deletePipelineSettingVersion(dslContext, projectId, pipelineIds)
            }
            processDataDeleteDao.deletePipelineResource(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineResourceVersion(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineLabelPipeline(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deleteTemplatePipeline(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineBuildSummary(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineBuildHistory(dslContext, projectId, pipelineIds)
            processDataDeleteDao.deletePipelineInfo(dslContext, projectId, pipelineIds)
        }
        logger.info("project[$projectId]|pipeline[$pipelineIds] deleteProjectPipelineRelData success!")
    }

    private fun deletePipelineBuildDataByPipelineId(
        archivePipelineFlag: Boolean?,
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        broadcastTableDeleteFlag: Boolean?
    ) {
        if (archivePipelineFlag != true) {
            processDataDeleteDao.deletePipelineBuildContainer(dslContext, projectId, pipelineId)
            processDataDeleteDao.deletePipelineBuildStage(dslContext, projectId, pipelineId)
            processDataDeleteDao.deletePipelineFavor(dslContext, projectId, pipelineId)
            processDataDeleteDao.deletePipelineViewGroup(dslContext, projectId, pipelineId)
            processDataDeleteDao.deletePipelineRecentUse(dslContext, projectId, pipelineId)
            processDataDeleteDao.deletePipelineTriggerDetail(dslContext, projectId, pipelineId)
            processDataDeleteDao.deletePipelineAuditResource(dslContext, projectId, pipelineId)
            if (broadcastTableDeleteFlag != false) {
                // 如果广播表数据清理标识不是false才清理广播表的数据（迁移库如果和原库一起组成数据库集群则不需要清理广播表数据）
                processDataDeleteDao.deletePipelineRemoteAuth(dslContext, projectId, pipelineId)
                processDataDeleteDao.deletePipelineWebhook(dslContext, projectId, pipelineId)
                processDataDeleteDao.deletePipelineTimer(dslContext, projectId, pipelineId)
            }
        }
    }

    private fun deletePipelineBuildDataByBuilds(
        buildIds: MutableList<String>?,
        archivePipelineFlag: Boolean?,
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ) {
        if (!buildIds.isNullOrEmpty()) {
            if (archivePipelineFlag != true) {
                processDataDeleteDao.deletePipelineBuildDetail(dslContext, projectId, buildIds)
                processDataDeleteDao.deletePipelineBuildVar(dslContext, projectId, buildIds)
                processDataDeleteDao.deletePipelinePauseValue(dslContext, projectId, buildIds)
                processDataDeleteDao.deletePipelineWebhookBuildParameter(dslContext, projectId, buildIds)
                processDataDeleteDao.deletePipelineWebhookQueue(dslContext, projectId, buildIds)
                processDataDeleteDao.deletePipelineBuildTask(dslContext, projectId, buildIds)
                processDataDeleteDao.deleteReport(
                    dslContext = dslContext, projectId = projectId, pipelineId = pipelineId, buildIds = buildIds
                )
                processDataDeleteDao.deletePipelineBuildTemplateAcrossInfo(
                    dslContext = dslContext, projectId = projectId, pipelineId = pipelineId, buildIds = buildIds
                )
                processDataDeleteDao.deletePipelineTriggerReview(dslContext, projectId, buildIds)
            }
            processDataDeleteDao.deletePipelineBuildRecordContainer(dslContext, projectId, buildIds)
            processDataDeleteDao.deletePipelineBuildRecordModel(dslContext, projectId, buildIds)
            processDataDeleteDao.deletePipelineBuildRecordStage(dslContext, projectId, buildIds)
            processDataDeleteDao.deletePipelineBuildRecordTask(dslContext, projectId, buildIds)
        }
    }

    /**
     * 删除项目直接相关的数据
     * @param dslContext jooq上下文
     * @param projectId 项目ID
     * @param targetClusterName 迁移集群
     * @param targetDataSourceName 迁移数据源名称
     * @param migrationLock 项目迁移锁
     * @return 字段列表
     */
    fun deleteProjectDirectlyRelData(
        dslContext: DSLContext,
        projectId: String,
        targetClusterName: String,
        targetDataSourceName: String,
        migrationLock: MigrationLock? = null
    ) {
        try {
            migrationLock?.lock()
            val queryParam = ProjectDataMigrateHistoryQueryParam(
                projectId = projectId,
                moduleCode = SystemModuleEnum.PROCESS,
                targetClusterName = targetClusterName,
                targetDataSourceName = targetDataSourceName
            )
            // 判断是否能删除db中数据
            if (projectDataMigrateHistoryService.isDataCanDelete(queryParam)) {
                deleteProjectRelData(dslContext, projectId)
            } else {
                throw ErrorCodeException(
                    errorCode = MiscMessageCode.ERROR_PROJECT_DATA_REPEAT_MIGRATE,
                    params = arrayOf(projectId),
                    defaultMessage = I18nUtil.getCodeLanMessage(
                        messageCode = MiscMessageCode.ERROR_PROJECT_DATA_REPEAT_MIGRATE,
                        params = arrayOf(projectId)
                    )
                )
            }
        } finally {
            migrationLock?.unlock()
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
        processDataDeleteDao.deleteTemplate(dslContext, projectId)
        processDataDeleteDao.deletePipelineViewTop(dslContext, projectId)
        if (clearBaseBuildData) {
            processDataDeleteDao.deletePipelineTriggerEvent(dslContext, projectId)
            processDataDeleteDao.deleteProjectPipelineCallbackHistory(dslContext, projectId)
        }
        logger.info("project[$projectId] deleteProjectDirectlyRelData success!")
    }
}
