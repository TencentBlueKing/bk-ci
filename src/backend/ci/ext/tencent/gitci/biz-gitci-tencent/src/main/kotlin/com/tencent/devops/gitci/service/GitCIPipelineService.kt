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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.gitci.service

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.app.PipelinePage
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCIPipelineService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitCISettingDao: GitCISettingDao,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val repositoryConfService: RepositoryConfService,
    private val currentBuildService: CurrentBuildService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIPipelineService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getPipelineList(
        userId: String,
        gitProjectId: Long,
        page: Int?,
        pageSize: Int?
    ): PipelinePage<GitProjectPipeline> {
        logger.info("get history build list, gitProjectId: $gitProjectId")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId)
        if (conf == null) {
            repositoryConfService.initGitCISetting(userId, gitProjectId)
            return PipelinePage(
                count = 0L,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                totalPages = 0,
                records = emptyList(),
                hasCreatePermission = true,
                hasPipelines = false,
                hasFavorPipelines = false,
                hasPermissionPipelines = false,
                currentView = null
            )
        }
        val pipelines = pipelineResourceDao.getListByGitProjectId(dslContext, gitProjectId)
        if (pipelines.isEmpty()) return PipelinePage(
            count = 0L,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            totalPages = 0,
            records = emptyList(),
            hasCreatePermission = true,
            hasPipelines = false,
            hasFavorPipelines = false,
            hasPermissionPipelines = false,
            currentView = null
        )
        val projectId = "git_$gitProjectId"
        return PipelinePage(
            count = pipelines.size.toLong(),
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            totalPages = 0,
            records = pipelines.map {
                GitProjectPipeline(
                    gitProjectId = gitProjectId,
                    projectCode = projectId,
                    pipelineId = it.pipelineId,
                    branch = it.branch,
                    filePath = it.filePath,
                    displayName = it.displayName,
                    enabled = it.enabled,
                    creator = it.creator,
                    latestBuildDetail = null,
                    createTime = it.createTime.timestampmilli(),
                    updateTime = it.updateTime.timestampmilli()
                )
            },
            hasCreatePermission = true,
            hasPipelines = true,
            hasFavorPipelines = false,
            hasPermissionPipelines = true,
            currentView = null
        )
    }

    private fun getBuildHistory(buildId: String, buildHistoryList: List<BuildHistory>): BuildHistory? {
        buildHistoryList.forEach {
            if (it.id == buildId) {
                return it
            }
        }
        return null
    }
}
