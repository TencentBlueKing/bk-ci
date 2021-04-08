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

package com.tencent.devops.misc.cron.process

import com.tencent.devops.misc.config.MiscPipelineTransferContext
import com.tencent.devops.misc.service.auto.PipelineTransferService
import com.tencent.devops.misc.service.process.OldPipelineService
import com.tencent.devops.misc.service.project.ProjectService
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Suppress("UNUSED")
class PipelineTransferJob @Autowired constructor(
    private val projectService: ProjectService,
    private val oldPipelineService: OldPipelineService,
    private val pipelineTransferService: PipelineTransferService,
    private val miscPipelineTransferContext: MiscPipelineTransferContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTransferJob::class.java)
        private const val PIPELINE_BUILD_HISTORY_PAGE_SIZE = 100
    }

    @Scheduled(initialDelay = 12000, fixedDelay = 10000)
    fun transfer() {
        if (!miscPipelineTransferContext.switch()) {
            return
        }

        var fail = false
        logger.info("transfer|START|")
        var maxHandleProjectPrimaryId = 0L
        try {
            if (!miscPipelineTransferContext.tryLock()) {
                logger.info("transfer|END|LOCK_FAIL")
                return
            }

            val needTransferProjectIdList: List<String> = miscPipelineTransferContext.needTransferProjectIdList()

            var handleProjectPrimaryId = miscPipelineTransferContext.getLastTransferProjectSeqId()
            if (handleProjectPrimaryId == null) {
                handleProjectPrimaryId = projectService.getMinId(needTransferProjectIdList) ?: 0L
            } else {
                val maxProjectPrimaryId = projectService.getMaxId(needTransferProjectIdList) ?: 0L
                if (handleProjectPrimaryId >= maxProjectPrimaryId) {
                    logger.info("transfer|END|ALL_FINISH")
                    return
                }
            }

            val transferProjectList = if (needTransferProjectIdList.isNullOrEmpty()) {

                val dealProjectBatchSize = miscPipelineTransferContext.dealProjectBatchSize()

                maxHandleProjectPrimaryId = handleProjectPrimaryId + dealProjectBatchSize

                projectService.getProjectInfoList(minId = handleProjectPrimaryId, maxId = maxHandleProjectPrimaryId)
            } else {

                maxHandleProjectPrimaryId = handleProjectPrimaryId.toLong()

                projectService.getProjectInfoList(projectIdList = needTransferProjectIdList)
            }

            transferProjectList?.forEach nextOne@{ projectInfo ->

                if (!miscPipelineTransferContext.checkTransferChannel(projectInfo.channel)) {
                    return@nextOne
                }

                if (projectInfo.id > maxHandleProjectPrimaryId) {
                    maxHandleProjectPrimaryId = projectInfo.id
                }

                logger.info("transfer|RUN|projectId=${projectInfo.projectId}|channel=${projectInfo.channel}")

                val pipelineInfoList = oldPipelineService.listPipelineInfos(projectInfo.projectId)
                pipelineInfoList.forEach { pipelineInfoRecord ->
                    transferPipelines(pipelineInfoRecord)
                }
            }

            // 将当前已处理完的最大项目Id存入redis
            miscPipelineTransferContext.setLastProjectSeqId(maxHandleProjectPrimaryId)
        } catch (ignored: Throwable) {
            fail = true
            logger.error("transfer|FAIL|$ignored", ignored)
        } finally {
            miscPipelineTransferContext.unLock()
            logger.info("transfer|END|fail=$fail|setLastProjectSeqId=$maxHandleProjectPrimaryId")
        }
    }

    private fun transferPipelines(pipelineInfoRecord: TPipelineInfoRecord) {
        try {

            pipelineTransferService.addPipelineInfo(pipelineInfoRecord)

            val resourceRecord = oldPipelineService.getPipelineRes(pipelineInfoRecord.pipelineId)
            pipelineTransferService.addResourceRecord(resourceRecord)

            val settingRecord = oldPipelineService.getPipelineSetting(pipelineInfoRecord.pipelineId)
            pipelineTransferService.addSettingRecord(settingRecord)

            val summaryRecord = oldPipelineService.getPipelineSummary(pipelineInfoRecord.pipelineId)
            pipelineTransferService.addSummaryRecord(summaryRecord)

            var offset = 0L
            do {
                val listPipelineBuilds = oldPipelineService.listPipelineBuilds(
                    projectId = pipelineInfoRecord.projectId,
                    pipelineId = pipelineInfoRecord.pipelineId,
                    offset = offset,
                    limit = PIPELINE_BUILD_HISTORY_PAGE_SIZE
                )
                if (listPipelineBuilds.isNotEmpty()) {
                    pipelineTransferService.addPipelineBuilds(listPipelineBuilds)
                    offset += PIPELINE_BUILD_HISTORY_PAGE_SIZE
                }
            } while (listPipelineBuilds.size >= PIPELINE_BUILD_HISTORY_PAGE_SIZE)
        } catch (duplicate: Exception) {
            logger.warn("transferPipelines|FAIL|${pipelineInfoRecord.pipelineId}|$duplicate")
        }
    }
}
