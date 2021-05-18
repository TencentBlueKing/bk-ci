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

import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.opdata.model.NodeModel
import com.tencent.bkrepo.opdata.model.ProjectModel
import com.tencent.bkrepo.opdata.model.RepoModel
import com.tencent.bkrepo.opdata.model.TProjectMetrics
import com.tencent.bkrepo.opdata.pojo.RepoMetrics
import com.tencent.bkrepo.opdata.repository.ProjectMetricsRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ProjectStatJob(
    private val projectModel: ProjectModel,
    private val repoModel: RepoModel,
    private val nodeModel: NodeModel,
    private val projectMetricsRepository: ProjectMetricsRepository
) {

    @Scheduled(cron = "00 15 * * * ?")
    @SchedulerLock(name = "ProjectStatJob", lockAtMostFor = "PT1H")
    fun statProjectRepoSize() {
        logger.info("start to stat node table metrics")
        val projects = projectModel.getProjectList()
        var result = mutableListOf<TProjectMetrics>()
        projects.forEach {
            var repoCapSize = 0L
            var repoNodeNum = 0L
            val projectId = it.name
            val repos = repoModel.getRepoListByProjectId(it.name)
            var repoMetrics = mutableListOf<RepoMetrics>()
            repos.forEach {
                val repoName = it
                val nodeSize = nodeModel.getNodeSize(projectId, repoName)
                repoCapSize += nodeSize.size
                repoNodeNum += nodeSize.num
                repoMetrics.add(RepoMetrics(repoName, nodeSize.size / (1024 * 1024 * 1024), nodeSize.num))
            }
            result.add(TProjectMetrics(projectId, repoNodeNum, repoCapSize / (1024 * 1024 * 1024), repoMetrics))
        }
        projectMetricsRepository.deleteAll()
        projectMetricsRepository.insert(result)
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
    }
}
