/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.opdata.job

import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.opdata.constant.B_0
import com.tencent.bkrepo.opdata.constant.GB_1
import com.tencent.bkrepo.opdata.constant.GB_10
import com.tencent.bkrepo.opdata.constant.MB_100
import com.tencent.bkrepo.opdata.constant.MB_500
import com.tencent.bkrepo.opdata.model.ProjectModel
import com.tencent.bkrepo.opdata.model.RepoInfo
import com.tencent.bkrepo.opdata.model.RepoModel
import com.tencent.bkrepo.opdata.model.TFileExtensionMetrics
import com.tencent.bkrepo.opdata.model.TSizeDistributionMetrics
import com.tencent.bkrepo.opdata.repository.FileExtensionMetricsRepository
import com.tencent.bkrepo.opdata.repository.SizeDistributionMetricsRepository
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FileStatJob(
    val projectModel: ProjectModel,
    val repoModel: RepoModel,
    val nodeClient: NodeClient,
    val fileExtensionMetricsRepository: FileExtensionMetricsRepository,
    val sizeDistributionMetricsRepository: SizeDistributionMetricsRepository
) {

    /**
     * 统计文件后缀名和文件大小分布信息
     */
    @Scheduled(cron = "00 00 18 * * ?")
    @SchedulerLock(name = "FileStatJob", lockAtMostFor = "PT1H")
    fun statFileInfo() {
        logger.info("start to stat file info")
        val fileExtensionMetricsList = mutableListOf<TFileExtensionMetrics>()
        val sizeDistributionMetricsList = mutableListOf<TSizeDistributionMetrics>()
        val projects = projectModel.getProjectList()
        projects.forEach { project ->
            val repos = repoModel.getRepoListByProjectId(project.name)
            repos.forEach { repo ->
                paginationStatFileInfo(project, repo, fileExtensionMetricsList, sizeDistributionMetricsList)
            }
        }
        fileExtensionMetricsRepository.deleteAll()
        fileExtensionMetricsRepository.insert(fileExtensionMetricsList)
        sizeDistributionMetricsRepository.deleteAll()
        sizeDistributionMetricsRepository.insert(sizeDistributionMetricsList)
        logger.info("stat file info done")
    }

    private fun paginationStatFileInfo(
        project: ProjectInfo,
        repo: RepoInfo,
        fileExtensionMetricsList: MutableList<TFileExtensionMetrics>,
        sizeDistributionMetricsList: MutableList<TSizeDistributionMetrics>
    ) {
        val sizeDistribution = initMap()
        val extensionStat = mutableMapOf<String, Pair<Long, Long>>()
        var pageNum = 1
        val pageSize = 1000
        var queryCount: Int
        do {
            val option = NodeListOption(pageNum, pageSize, includeFolder = false, deep = true)
            val nodePageInfo = nodeClient.listNodePage(project.name, repo.name, PathUtils.ROOT, option).data
            val nodeList = nodePageInfo?.records ?: emptyList()
            queryCount = nodeList.size
            pageNum += 1
            nodeList.forEach {
                statNodeInfo(it, sizeDistribution, extensionStat)
            }
        } while (queryCount == pageSize)
        extensionStat.forEach { (extension, pair) ->
            val fileExtensionMetrics = TFileExtensionMetrics(
                project.name, repo.name, extension, pair.first, pair.second
            )
            fileExtensionMetricsList.add(fileExtensionMetrics)
        }
        val sizeDistributionMetrics = TSizeDistributionMetrics(project.name, repo.name, sizeDistribution)
        sizeDistributionMetricsList.add(sizeDistributionMetrics)
    }

    private fun statNodeInfo(
        it: NodeInfo,
        sizeDistribution: MutableMap<String, Long>,
        extensionStat: MutableMap<String, Pair<Long, Long>>
    ) {
        for (lowerLimit in sizeRange.reversed()) {
            if (it.size > lowerLimit.toLong()) {
                sizeDistribution[lowerLimit] = sizeDistribution[lowerLimit]!! + 1L
            }
        }
        val extension = it.name.substringAfterLast(".", "none")
        extensionStat[extension] = Pair(
            (extensionStat[extension]?.first ?: 0L) + 1L,
            (extensionStat[extension]?.second ?: 0L) + it.size
        )
    }

    private fun initMap(): MutableMap<String, Long> {
        val sizeDistribution = mutableMapOf<String, Long>()
        sizeRange.forEach {
            sizeDistribution[it] = 0L
        }
        return sizeDistribution
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
        private val sizeRange = listOf(B_0, MB_100, MB_500, GB_1, GB_10)
    }
}
