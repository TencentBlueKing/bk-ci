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

import com.tencent.devops.misc.pojo.project.ProjectDataClearConfig
import com.tencent.devops.misc.service.artifactory.ArtifactoryDataClearService
import com.tencent.devops.misc.service.dispatch.DispatchDataClearService
import com.tencent.devops.misc.service.plugin.PluginDataClearService
import com.tencent.devops.misc.service.quality.QualityDataClearService
import com.tencent.devops.misc.service.repository.RepositoryDataClearService
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 流水线构建数据清理核心服务
 *
 * 提供可复用的流水线级别构建数据清理能力，供定时任务和事件驱动两种路径共用。
 */
@Service
@Suppress("LongParameterList")
class PipelineBuildDataClearService @Autowired constructor(
    private val processMiscService: ProcessMiscService,
    private val processDataClearService: ProcessDataClearService,
    private val repositoryDataClearService: RepositoryDataClearService,
    private val dispatchDataClearService: DispatchDataClearService,
    private val pluginDataClearService: PluginDataClearService,
    private val qualityDataClearService: QualityDataClearService,
    private val artifactoryDataClearService: ArtifactoryDataClearService,
    private val processRelatedPlatformDataClearService: ProcessRelatedPlatformDataClearService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildDataClearService::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
        // 连续空页阈值：出现这么多轮空查询后直接结束分页扫表，防止 while 循环空转
        private const val MAX_EMPTY_PAGE_THRESHOLD = 5
    }

    @Value("\${process.clearBaseBuildData:false}")
    private val clearBaseBuildData: Boolean = false

    /**
     * 清理正常流水线的超量和过期构建数据（定时任务使用）
     */
    fun cleanNormalPipelineData(
        pipelineId: String,
        projectId: String,
        projectDataClearConfig: ProjectDataClearConfig
    ) {
        cleanOverflowPipelineData(pipelineId, projectId, projectDataClearConfig)
        cleanExpiredPipelineData(pipelineId, projectId, projectDataClearConfig)
    }

    /**
     * 仅清理超过 maxKeepNum 的构建数据（事件驱动路径使用）
     *
     * 构建结束事件触发时，通常只有"数量"可能突破上限，
     * 没必要再做一次全量的"过期时间"扫描，节省 DB 压力。
     */
    fun cleanOverflowPipelineData(
        pipelineId: String,
        projectId: String,
        projectDataClearConfig: ProjectDataClearConfig
    ) {
        val maxPipelineBuildNum = processMiscService.getMaxPipelineBuildNum(projectId, pipelineId)
        val maxKeepNum = projectDataClearConfig.maxKeepNum
        val maxBuildNum = maxPipelineBuildNum - maxKeepNum
        if (maxBuildNum <= 0) {
            return
        }
        logger.info(
            "cleanOverflowPipelineData|$projectId|$pipelineId|exceed maxKeepNum, cleaning $maxBuildNum builds"
        )
        cleanBuildHistoryData(
            pipelineId = pipelineId,
            projectId = projectId,
            isCompletelyDelete = true,
            maxBuildNum = maxBuildNum.toInt()
        )
    }

    /**
     * 清理过期（超过 maxStartTime）的构建数据
     */
    fun cleanExpiredPipelineData(
        pipelineId: String,
        projectId: String,
        projectDataClearConfig: ProjectDataClearConfig
    ) {
        cleanBuildHistoryData(
            pipelineId = pipelineId,
            projectId = projectId,
            isCompletelyDelete = false,
            maxStartTime = projectDataClearConfig.maxStartTime
        )
    }

    /**
     * 清理已删除或已归档流水线的全部构建数据及流水线记录
     */
    fun cleanDeletePipelineData(
        pipelineId: String,
        projectId: String,
        archiveFlag: Boolean? = null
    ) {
        cleanBuildHistoryData(
            pipelineId = pipelineId,
            projectId = projectId,
            isCompletelyDelete = true,
            archiveFlag = archiveFlag
        )
        processDataClearService.clearPipelineData(projectId, pipelineId, archiveFlag)
    }

    /**
     * 按条件分页清理指定流水线的构建历史数据
     */
    fun cleanBuildHistoryData(
        pipelineId: String,
        projectId: String,
        isCompletelyDelete: Boolean,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null,
        archiveFlag: Boolean? = null
    ) {
        val maxPipelineBuildNum = processMiscService.getMaxPipelineBuildNum(
            projectId = projectId,
            pipelineId = pipelineId,
            maxBuildNum = maxBuildNum,
            maxStartTime = maxStartTime,
            archiveFlag = archiveFlag
        )
        logger.info("cleanBuildHistoryData|$projectId|$pipelineId|maxPipelineBuildNum=$maxPipelineBuildNum")
        var totalHandleNum = processMiscService.getMinPipelineBuildNum(projectId, pipelineId, archiveFlag).toInt()
        // 连续命中空页的次数，超过阈值则认为剩余区间已无可清理数据，提前结束避免无效分页扫表
        var emptyPageCount = 0
        while (totalHandleNum <= maxPipelineBuildNum) {
            val cleanBuilds = mutableListOf<String>()
            val pipelineHistoryBuildIdList = processMiscService.getHistoryBuildIdList(
                projectId = projectId,
                pipelineId = pipelineId,
                totalHandleNum = totalHandleNum,
                handlePageSize = DEFAULT_PAGE_SIZE,
                isCompletelyDelete = isCompletelyDelete,
                maxBuildNum = maxBuildNum,
                maxStartTime = maxStartTime,
                archiveFlag = archiveFlag
            )
            if (pipelineHistoryBuildIdList.isNullOrEmpty()) {
                emptyPageCount++
                if (emptyPageCount >= MAX_EMPTY_PAGE_THRESHOLD) {
                    logger.info(
                        "cleanBuildHistoryData|$projectId|$pipelineId|" +
                            "reached $MAX_EMPTY_PAGE_THRESHOLD consecutive empty pages, break"
                    )
                    break
                }
                totalHandleNum += DEFAULT_PAGE_SIZE
                continue
            }
            emptyPageCount = 0
            pipelineHistoryBuildIdList.forEach { buildId ->
                if (clearBaseBuildData && archiveFlag != true) {
                    processDataClearService.clearBaseBuildData(projectId, buildId)
                }
                repositoryDataClearService.clearBuildData(buildId)
                if (isCompletelyDelete) {
                    dispatchDataClearService.clearBuildData(buildId)
                    pluginDataClearService.clearBuildData(buildId)
                    qualityDataClearService.clearBuildData(buildId)
                    artifactoryDataClearService.clearBuildData(buildId)
                    processDataClearService.clearOtherBuildData(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        archiveFlag = archiveFlag
                    )
                    cleanBuilds.add(buildId)
                } else {
                    processDataClearService.clearSkipRecordTaskData(projectId, buildId, archiveFlag)
                }
            }
            totalHandleNum += DEFAULT_PAGE_SIZE
            if (cleanBuilds.isNotEmpty()) {
                processRelatedPlatformDataClearService.cleanBuildData(projectId, pipelineId, cleanBuilds)
            }
        }
    }
}
