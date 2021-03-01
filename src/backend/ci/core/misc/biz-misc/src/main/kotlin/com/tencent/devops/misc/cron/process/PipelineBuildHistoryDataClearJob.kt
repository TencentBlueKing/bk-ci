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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.config.MiscBuildDataClearConfig
import com.tencent.devops.misc.service.artifactory.ArtifactoryDataClearService
import com.tencent.devops.misc.service.dispatch.DispatchDataClearService
import com.tencent.devops.misc.service.plugin.PluginDataClearService
import com.tencent.devops.misc.service.process.ProcessDataClearService
import com.tencent.devops.misc.service.process.ProcessService
import com.tencent.devops.misc.service.project.ProjectService
import com.tencent.devops.misc.service.quality.QualityDataClearService
import com.tencent.devops.misc.service.repository.RepositoryDataClearService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.Calendar

@Component
@Suppress("ALL")
class PipelineBuildHistoryDataClearJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val miscBuildDataClearConfig: MiscBuildDataClearConfig,
    private val projectService: ProjectService,
    private val processService: ProcessService,
    private val processDataClearService: ProcessDataClearService,
    private val repositoryDataClearService: RepositoryDataClearService,
    private val dispatchDataClearService: DispatchDataClearService,
    private val pluginDataClearService: PluginDataClearService,
    private val qualityDataClearService: QualityDataClearService,
    private val artifactoryDataClearService: ArtifactoryDataClearService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildHistoryDataClearJob::class.java)
        private const val LOCK_KEY = "pipelineBuildHistoryDataClear"
        private const val PIPELINE_BUILD_HISTORY_PAGE_SIZE = 100
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY =
            "pipeline:build:history:data:clear:project:id"
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_KEY =
            "pipeline:build:history:data:clear:project:list"
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 12000)
    fun pipelineBuildHistoryDataClear() {
        if (!miscBuildDataClearConfig.switch.toBoolean()) {
            // 如果清理构建历史数据开关关闭，则不清理
            return
        }
        logger.info("pipelineBuildHistoryDataClear start")
        val lock = RedisLock(redisOperation,
            LOCK_KEY, 3000)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            // 查询project表中的项目数据处理
            val projectIdListConfig = redisOperation.get(PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_KEY)
            // 组装查询项目的条件
            var projectIdList: List<String>? = null
            if (!projectIdListConfig.isNullOrBlank()) {
                projectIdList = projectIdListConfig!!.split(",")
            }
            var handleProjectPrimaryId = redisOperation.get(PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY)?.toLong()
            if (handleProjectPrimaryId == null) {
                handleProjectPrimaryId = projectService.getMinId(projectIdList) ?: 0L
            } else {
                val maxProjectPrimaryId = projectService.getMaxId(projectIdList) ?: 0L
                if (handleProjectPrimaryId >= maxProjectPrimaryId) {
                    // 已经清理完全部项目的流水线的过期构建记录，再重新开始清理
                    redisOperation.delete(PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY)
                    logger.info("pipelineBuildHistoryDataClear reStart")
                    return
                }
            }
            val maxEveryProjectHandleNum = miscBuildDataClearConfig.maxEveryProjectHandleNum.toInt()
            var maxHandleProjectPrimaryId = handleProjectPrimaryId ?: 0L
            val projectInfoList = if (projectIdListConfig.isNullOrBlank()) {
                maxHandleProjectPrimaryId = handleProjectPrimaryId + maxEveryProjectHandleNum
                projectService.getProjectInfoList(minId = handleProjectPrimaryId, maxId = maxHandleProjectPrimaryId)
            } else {
                projectService.getProjectInfoList(projectIdList = projectIdList)
            }
            // 根据项目依次查询T_PIPELINE_INFO表中的流水线数据处理
            projectInfoList?.forEach { projectInfo ->
                val projectPrimaryId = projectInfo.id
                if (projectPrimaryId > maxHandleProjectPrimaryId) {
                    maxHandleProjectPrimaryId = projectPrimaryId
                }
                val projectId = projectInfo.projectId
                val pipelineIdList = processService.getPipelineIdListByProjectId(projectId)
                pipelineIdList?.forEach { pipelineId ->
                    // 根据流水线ID依次查询T_PIPELINE_BUILD_HISTORY表中X个月前的构建记录
                    val monthRange = miscBuildDataClearConfig.monthRange
                    val maxStartTime = DateTimeUtil.getFutureDateFromNow(Calendar.MONTH, monthRange.toInt())
                    logger.info("pipelineBuildHistoryPastDataClear start..............")
                    cleanBuildHistoryData(
                        pipelineId = pipelineId,
                        projectId = projectId,
                        isCompletelyDelete = false,
                        maxStartTime = DateTimeUtil.convertDateToLocalDateTime(maxStartTime)
                    )
                    // 判断构建记录是否超过系统展示的最大数量，如果超过则需清理超量的数据
                    val maxPipelineBuildNum = processService.getMaxPipelineBuildNum(projectId, pipelineId)
                    val maxKeepNum = miscBuildDataClearConfig.maxKeepNum.toInt()
                    if (maxPipelineBuildNum > maxKeepNum) {
                        logger.info("pipelineBuildHistoryRecentDataClear start.............")
                        cleanBuildHistoryData(
                            pipelineId = pipelineId,
                            projectId = projectId,
                            isCompletelyDelete = true,
                            maxBuildNum = (maxPipelineBuildNum - maxKeepNum).toInt()
                        )
                    }
                }
            }
            // 将当前已处理完的最大项目Id存入redis
            redisOperation.set(
                key = PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY,
                value = maxHandleProjectPrimaryId.toString(),
                expired = false
            )
        } catch (t: Throwable) {
            logger.warn("pipelineBuildHistoryDataClear failed", t)
        } finally {
            lock.unlock()
        }
    }

    private fun cleanBuildHistoryData(
        pipelineId: String,
        projectId: String,
        isCompletelyDelete: Boolean,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null
    ) {
        val totalBuildCount = processService.getTotalBuildCount(pipelineId, maxBuildNum, maxStartTime)
        logger.info("pipelineBuildHistoryDataClear|$projectId|$pipelineId|totalBuildCount=$totalBuildCount")
        var totalHandleNum = 0
        while (totalHandleNum < totalBuildCount) {
            val pipelineHistoryBuildIdList = processService.getHistoryBuildIdList(
                pipelineId = pipelineId,
                totalHandleNum = totalHandleNum,
                handlePageSize = PIPELINE_BUILD_HISTORY_PAGE_SIZE,
                isCompletelyDelete = isCompletelyDelete,
                maxBuildNum = maxBuildNum,
                maxStartTime = maxStartTime
            )
            pipelineHistoryBuildIdList?.forEach { buildId ->
                // 依次删除process表中的相关构建记录(T_PIPELINE_BUILD_HISTORY做为基准表，
                // 为了保证构建流水记录删干净，T_PIPELINE_BUILD_HISTORY记录要最后删)
                processDataClearService.clearBaseBuildData(buildId)
                repositoryDataClearService.clearBuildData(buildId)
                if (isCompletelyDelete) {
                    dispatchDataClearService.clearBuildData(buildId)
                    pluginDataClearService.clearBuildData(buildId)
                    qualityDataClearService.clearBuildData(buildId)
                    artifactoryDataClearService.clearBuildData(buildId)
                    processDataClearService.clearOtherBuildData(projectId, pipelineId, buildId)
                }
            }
            totalHandleNum += PIPELINE_BUILD_HISTORY_PAGE_SIZE
        }
    }
}
