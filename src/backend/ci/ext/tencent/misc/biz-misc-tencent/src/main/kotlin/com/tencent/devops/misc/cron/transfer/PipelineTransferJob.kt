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

package com.tencent.devops.misc.cron.transfer

import com.tencent.devops.misc.config.MiscPipelineTransferContext
import com.tencent.devops.misc.service.auto.tsource.SourcePipelineService
import com.tencent.devops.misc.service.auto.ttarget.TargetPipelineService
import com.tencent.devops.misc.service.project.ProjectMiscService
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 迁移PROD集群上的Auto流水线定时任务
 */
@Component
@Suppress("ALL", "UNUSED")
class PipelineTransferJob @Autowired constructor(
    private val projectMiscService: ProjectMiscService,
    private val sourcePipelineService: SourcePipelineService,
    private val targetPipelineService: TargetPipelineService,
    private val miscPipelineTransferContext: MiscPipelineTransferContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTransferJob::class.java)
        private const val PIPELINE_BUILD_HISTORY_PAGE_SIZE = 14
    }

    @Scheduled(initialDelay = 12000, fixedDelay = 60000)
    fun transfer() {
        if (!miscPipelineTransferContext.enable()) {
            return
        }

        var fail = false
        logger.info("transfer|START")
        var maxHandleProjectPrimaryId = 0L
        try {
            if (!miscPipelineTransferContext.tryLock()) {
                logger.info("transfer|END|LOCK_FAIL")
                return
            }

            val needTransferProjectIdList: List<String> = miscPipelineTransferContext.needTransferProjectIdList()

            var handleProjectPrimaryId = miscPipelineTransferContext.getLastTransferProjectSeqId()
            if (handleProjectPrimaryId == null) {
                handleProjectPrimaryId = projectMiscService.getMinId(needTransferProjectIdList) ?: 0L
            } else {
                val maxProjectPrimaryId = projectMiscService.getMaxId(needTransferProjectIdList) ?: 0L
                if (handleProjectPrimaryId >= maxProjectPrimaryId) {
                    logger.info("transfer|END|ALL_FINISH")
                    return
                }
            }

            val transferProjectList = if (needTransferProjectIdList.isNullOrEmpty()) {

                maxHandleProjectPrimaryId = handleProjectPrimaryId + miscPipelineTransferContext.dealProjectBatchSize()
                logger.info("transfer|startId=$handleProjectPrimaryId|maxId=$maxHandleProjectPrimaryId")

                projectMiscService.getProjectInfoList(minId = handleProjectPrimaryId, maxId = maxHandleProjectPrimaryId)
            } else {

                projectMiscService.getProjectInfoList(projectIdList = needTransferProjectIdList)
            }

            logger.info("transfer|projectSize=${transferProjectList?.size}")

            transferProjectList?.forEach nextOne@{ projectInfo ->

                if (projectInfo.id > maxHandleProjectPrimaryId) {
                    maxHandleProjectPrimaryId = projectInfo.id
                }

                if (miscPipelineTransferContext.isFinishProject(projectInfo.projectId)) {
                    return@nextOne
                }

                if (!miscPipelineTransferContext.checkTransferChannel(projectInfo.channel)) {
                    return@nextOne
                }

                logger.info("transfer|RUN|projectId=${projectInfo.projectId}|channel=${projectInfo.channel}")

                val pipelineInfoList = sourcePipelineService.listPipelineInfos(projectInfo.projectId)
                pipelineInfoList.forEach { pipelineInfoRecord ->
                    transferPipelines(pipelineInfoRecord)
                }

                miscPipelineTransferContext.addFinishProject(projectInfo.projectId)
            }

            // 将当前已处理完的最大项目Id存入redis
            miscPipelineTransferContext.setLastProjectSeqId(maxHandleProjectPrimaryId)
        } catch (ignored: Throwable) {
            fail = true
            logger.error("transfer|FAILED|$ignored", ignored)
        } finally {
            miscPipelineTransferContext.unLock()
            logger.info("transfer|END|fail=$fail|maxHandleProjectPrimaryId=$maxHandleProjectPrimaryId")
        }
    }

    private fun transferPipelines(pipelineInfoRecord: TPipelineInfoRecord) {
        try {

            targetPipelineService.addPipelineInfo(pipelineInfoRecord)
            val projectId = pipelineInfoRecord.projectId
            val pipelineId = pipelineInfoRecord.pipelineId
            val resourceRecord = sourcePipelineService.getPipelineLatestRes(projectId, pipelineId)
            targetPipelineService.addResourceRecord(resourceRecord)

            val settingRecord = sourcePipelineService.getPipelineSetting(projectId, pipelineId)
            targetPipelineService.addSettingRecord(settingRecord)

            val summaryRecord = sourcePipelineService.getPipelineSummary(projectId, pipelineId)
            targetPipelineService.addSummaryRecord(summaryRecord)

            var offset = 0L
            do {
                val listPipelineBuilds = sourcePipelineService.listPipelineBuilds(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    offset = offset,
                    limit = PIPELINE_BUILD_HISTORY_PAGE_SIZE
                )

                targetPipelineService.addPipelineBuilds(listPipelineBuilds)
                listPipelineBuilds.forEach {
                    val pipelineBuildDetail = sourcePipelineService.getPipelineBuildDetail(projectId, it.buildId)
                    targetPipelineService.addDetailRecord(pipelineBuildDetail)
                }

                offset += PIPELINE_BUILD_HISTORY_PAGE_SIZE
            } while (listPipelineBuilds.size >= PIPELINE_BUILD_HISTORY_PAGE_SIZE)
        } catch (ignored: Exception) {
            logger.warn("transferPipelines|DUPLICATE|${pipelineInfoRecord.pipelineId}|$ignored")
        }
    }
}
