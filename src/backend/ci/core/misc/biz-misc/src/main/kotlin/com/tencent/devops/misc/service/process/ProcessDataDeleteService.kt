/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.dao.process.ProcessDataDeleteDao
import com.tencent.devops.misc.pojo.constant.MiscMessageCode
import com.tencent.devops.misc.pojo.process.DeleteDataParam
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistoryQueryParam
import com.tencent.devops.misc.service.project.ProjectDataMigrateHistoryService
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProcessDataDeleteService @Autowired constructor(
    private val processDao: ProcessDao,
    private val processDataDeleteDao: ProcessDataDeleteDao,
    private val projectDataMigrateHistoryService: ProjectDataMigrateHistoryService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessDataDeleteService::class.java)
        private const val DEFAULT_PAGE_SIZE = 20
    }

    /**
     * 删除process数据库数据
     * @param deleteDataParam 删除迁移数据参数
     */
    fun deleteProcessData(
        deleteDataParam: DeleteDataParam
    ) {
        val lock = deleteDataParam.lock
        val projectId = deleteDataParam.projectId
        try {
            lock?.lock()
            val targetClusterName = deleteDataParam.targetClusterName
            val targetDataSourceName = deleteDataParam.targetDataSourceName
            checkMigrationDataDeleteCondition(targetClusterName, targetDataSourceName, projectId)
            deleteProcessRelData(deleteDataParam)
        } finally {
            lock?.unlock()
        }
    }

    private fun deleteProcessRelData(
        deleteDataParam: DeleteDataParam
    ) {
        val dslContext = deleteDataParam.dslContext
        val projectId = deleteDataParam.projectId
        val pipelineId = deleteDataParam.pipelineId
        val broadcastTableDeleteFlag = deleteDataParam.broadcastTableDeleteFlag
        if (!pipelineId.isNullOrBlank()) {
            // 如果流水线ID不为空，只需清理与流水线直接相关的数据
            deleteProjectPipelineRelData(
                dslContext = dslContext,
                projectId = projectId,
                pipelineIds = mutableListOf(pipelineId),
                broadcastTableDeleteFlag = broadcastTableDeleteFlag,
                archivePipelineFlag = deleteDataParam.archivePipelineFlag
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
                broadcastTableDeleteFlag = broadcastTableDeleteFlag,
                archivePipelineFlag = deleteDataParam.archivePipelineFlag
            )
        } while (pipelineIds?.size == DEFAULT_PAGE_SIZE)
        // 如果流水线ID为空，与项目直接相关的数据也需要清理
        deleteProjectDirectlyRelData(
            dslContext = dslContext,
            projectId = projectId,
            targetClusterName = deleteDataParam.targetClusterName,
            targetDataSourceName = deleteDataParam.targetDataSourceName
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
        broadcastTableDeleteFlag: Boolean? = false,
        archivePipelineFlag: Boolean? = null
    ) {
        pipelineIds?.forEach { pipelineId ->
            // 处理构建历史（普通+调试）
            listOf(false, true).forEach { isDebug ->
                processPipelineHistories(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    isDebug = isDebug,
                    archivePipelineFlag = archivePipelineFlag,
                    broadcastTableDeleteFlag = broadcastTableDeleteFlag
                )
            }
        }

        if (!pipelineIds.isNullOrEmpty()) {
            // 核心数据删除操作
            if (archivePipelineFlag != true) {
                processDataDeleteDao.deletePipelineModelTask(dslContext, projectId, pipelineIds)
            }
            listOf(
                processDataDeleteDao::deletePipelineResource,
                processDataDeleteDao::deletePipelineResourceVersion,
                processDataDeleteDao::deletePipelineLabelPipeline,
                processDataDeleteDao::deleteTemplatePipeline,
                processDataDeleteDao::deletePipelineBuildSummary,
                processDataDeleteDao::deletePipelineBuildHistoryDebug,
                processDataDeleteDao::deletePipelineSetting,
                processDataDeleteDao::deletePipelineSettingVersion,
                processDataDeleteDao::deletePipelineBuildHistory,
                processDataDeleteDao::deletePipelineInfo
            ).forEach { function ->
                function(dslContext, projectId, pipelineIds)
            }
        }
        logger.info("project[$projectId]|pipeline[$pipelineIds] deleteProjectPipelineRelData success!")
    }

    private fun processPipelineHistories(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        isDebug: Boolean,
        archivePipelineFlag: Boolean?,
        broadcastTableDeleteFlag: Boolean?
    ) {
        val tPipelineBuildHistory = TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY
        var offset = 0
        do {
            val records = if (isDebug) {
                processDao.getHistoryDebugInfoList(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
            } else {
                processDao.getHistoryInfoList(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
            }
            val buildIds = records?.map { it[tPipelineBuildHistory.BUILD_ID] }
            // 按BuildID批量删除数据
            deletePipelineBuildDataByBuilds(
                buildIds = buildIds,
                archivePipelineFlag = archivePipelineFlag,
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            offset += DEFAULT_PAGE_SIZE
        } while (records?.size == DEFAULT_PAGE_SIZE)
        // 按PipelineID删除数据
        deletePipelineBuildDataByPipelineId(
            archivePipelineFlag = archivePipelineFlag,
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            broadcastTableDeleteFlag = broadcastTableDeleteFlag
        )
    }

    private fun deletePipelineBuildDataByPipelineId(
        archivePipelineFlag: Boolean?,
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        broadcastTableDeleteFlag: Boolean?
    ) {
        if (archivePipelineFlag != true) {
            // 基础数据删除操作
            listOf(
                processDataDeleteDao::deletePipelineBuildContainer,
                processDataDeleteDao::deletePipelineBuildStage,
                processDataDeleteDao::deletePipelineRecentUse,
                processDataDeleteDao::deletePipelineTriggerDetail,
                processDataDeleteDao::deletePipelineAuditResource,
                processDataDeleteDao::deletePipelineTimerBranch,
                processDataDeleteDao::deletePipelineYamlInfo,
                processDataDeleteDao::deletePipelineYamlVersion,
                processDataDeleteDao::deletePipelineOperationLog,
                processDataDeleteDao::deletePipelineWebhookVersion,
                processDataDeleteDao::deletePipelineCallback,
                processDataDeleteDao::deletePipelineSubRef
            ).forEach { function ->
                function(dslContext, projectId, pipelineId)
            }

            if (broadcastTableDeleteFlag == true) {
                // 广播表数据清理
                listOf(
                    processDataDeleteDao::deletePipelineRemoteAuth,
                    processDataDeleteDao::deletePipelineWebhook,
                    processDataDeleteDao::deletePipelineTimer
                ).forEach { function ->
                    function(dslContext, projectId, pipelineId)
                }
            }
        }

        // 公共数据删除
        listOf(
            processDataDeleteDao::deletePipelineFavor,
            processDataDeleteDao::deletePipelineViewGroup
        ).forEach { function ->
            function(dslContext, projectId, pipelineId)
        }
    }

    private fun deletePipelineBuildDataByBuilds(
        buildIds: MutableList<String>?,
        archivePipelineFlag: Boolean?,
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ) {
        if (buildIds.isNullOrEmpty()) return

        if (archivePipelineFlag != true) {
            // 构建明细数据删除
            listOf(
                processDataDeleteDao::deletePipelineBuildDetail,
                processDataDeleteDao::deletePipelineBuildVar,
                processDataDeleteDao::deletePipelinePauseValue,
                processDataDeleteDao::deletePipelineWebhookBuildParameter,
                processDataDeleteDao::deletePipelineWebhookQueue,
                processDataDeleteDao::deletePipelineBuildTask
            ).forEach { function ->
                function(dslContext, projectId, buildIds)
            }

            processDataDeleteDao.deletePipelineBuildTemplateAcrossInfo(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildIds = buildIds
            )
        }

        // 公共报告数据删除
        processDataDeleteDao.deleteReport(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildIds = buildIds
        )

        // 记录类数据删除
        listOf(
            processDataDeleteDao::deletePipelineTriggerReview,
            processDataDeleteDao::deletePipelineBuildRecordContainer,
            processDataDeleteDao::deletePipelineBuildRecordModel,
            processDataDeleteDao::deletePipelineBuildRecordStage
        ).forEach { function ->
            function(dslContext, projectId, buildIds)
        }
        processDataDeleteDao.deletePipelineBuildRecordTask(dslContext, projectId, buildIds)
    }

    /**
     * 删除项目直接相关的数据
     * @param dslContext jooq上下文
     * @param projectId 项目ID
     * @param targetClusterName 迁移目标集群名称
     * @param targetDataSourceName 迁移目标数据源名称
     * @param lock 锁
     * @return 字段列表
     */
    fun deleteProjectDirectlyRelData(
        dslContext: DSLContext,
        projectId: String,
        targetClusterName: String? = null,
        targetDataSourceName: String? = null,
        lock: RedisLock? = null
    ) {
        try {
            lock?.lock()
            checkMigrationDataDeleteCondition(
                targetClusterName = targetClusterName,
                targetDataSourceName = targetDataSourceName,
                projectId = projectId
            )
            deleteProjectRelData(dslContext, projectId)
        } finally {
            lock?.unlock()
        }
    }

    private fun checkMigrationDataDeleteCondition(
        targetClusterName: String?,
        targetDataSourceName: String?,
        projectId: String
    ) {
        if (targetClusterName.isNullOrEmpty() || targetDataSourceName.isNullOrEmpty()) {
            return
        }
        val queryParam = ProjectDataMigrateHistoryQueryParam(
            projectId = projectId,
            moduleCode = SystemModuleEnum.PROCESS,
            targetClusterName = targetClusterName,
            targetDataSourceName = targetDataSourceName
        )
        // 判断是否能删除db中数据
        if (!projectDataMigrateHistoryService.isDataCanDelete(queryParam)) {
            throw ErrorCodeException(
                errorCode = MiscMessageCode.ERROR_PROJECT_DATA_REPEAT_MIGRATE,
                params = arrayOf(projectId),
                defaultMessage = I18nUtil.getCodeLanMessage(
                    messageCode = MiscMessageCode.ERROR_PROJECT_DATA_REPEAT_MIGRATE,
                    params = arrayOf(projectId)
                )
            )
        }
    }

    private fun deleteProjectRelData(dslContext: DSLContext, projectId: String) {
        processDataDeleteDao.deleteAuditResource(dslContext, projectId)
        processDataDeleteDao.deletePipelineSetting(dslContext, projectId)
        processDataDeleteDao.deletePipelineSettingVersion(dslContext, projectId)
        listOf(
            processDataDeleteDao::deletePipelineGroup,
            processDataDeleteDao::deletePipelineJobMutexGroup,
            processDataDeleteDao::deletePipelineLabel,
            processDataDeleteDao::deletePipelineView,
            processDataDeleteDao::deletePipelineViewUserLastView,
            processDataDeleteDao::deletePipelineViewUserSettings,
            processDataDeleteDao::deleteProjectPipelineCallback,
            processDataDeleteDao::deleteTemplate,
            processDataDeleteDao::deletePipelineViewTop,
            processDataDeleteDao::deletePipelineYamlSync,
            processDataDeleteDao::deletePipelineYamlBranchFile,
            processDataDeleteDao::deletePipelineYamlView,
            processDataDeleteDao::deletePipelineTriggerEvent,
            processDataDeleteDao::deleteProjectPipelineCallbackHistory
        ).forEach { it(dslContext, projectId) }
        logger.info("project[$projectId] deleteProjectDirectlyRelData success!")
    }
}
