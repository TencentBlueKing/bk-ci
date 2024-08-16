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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.project.pojo.app.AppProjectVO
import com.tencent.devops.project.pojo.enums.ProjectSourceEnum
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamGitProjectPipeline
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("LongParameterList")
class StreamAppService @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamScmService: StreamScmService,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val oauthService: StreamOauthService,
    private val streamProjectService: StreamProjectService
) {

    fun getGitCIProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        searchName: String?
    ): Pagination<AppProjectVO> {
        if (searchName.isNullOrEmpty()) {
            val cacheList = streamProjectService.cacheProjectList(userId).filter { it.enabledCi == true }.map {
                AppProjectVO(
                    projectCode = it.projectCode ?: ("git_" + it.id),
                    // 使用 pathWithPathSpace唯一标识App 中项目名称
                    projectName = it.pathWithNamespace ?: "",
                    logoUrl = it.avatarUrl,
                    projectSource = ProjectSourceEnum.GIT_CI.id
                )
            }
            val hasNext = cacheList.size >= pageSize * page
            val start = ((page - 1) * pageSize).takeIf { it < cacheList.size && it >= 0 } ?: cacheList.size
            val end = (page * pageSize).takeIf { it < cacheList.size && it >= 0 } ?: cacheList.size
            return Pagination(hasNext, cacheList.subList(start, end))
        }
        val token = oauthService.getAndCheckOauthToken(userId).accessToken
        val projectIdMap = streamScmService.getProjectList(
            accessToken = token,
            userId = userId,
            page = page,
            pageSize = pageSize,
            search = searchName,
            orderBy = null, sort = null, owned = null, minAccessLevel = null
        )?.associateBy { it.id!! }?.toMap()
        val hasNext = projectIdMap?.values?.size == pageSize
        if (projectIdMap.isNullOrEmpty()) {
            return Pagination(false, emptyList())
        }
        val projects = streamBasicSettingDao.searchProjectByIds(dslContext, projectIdMap.keys)
        val result = projects.map {
            val gitCodeInfo = projectIdMap[it.id]
            AppProjectVO(
                projectCode = it.projectCode,
                // 使用 pathWithPathSpace唯一标识App 中项目名称
                projectName = gitCodeInfo?.pathWithNamespace ?: gitCodeInfo?.nameWithNamespace ?: it.pathWithNameSpace
                    ?: it.nameWithNameSpace ?: it.name,
                logoUrl = gitCodeInfo?.avatarUrl,
                projectSource = ProjectSourceEnum.GIT_CI.id
            )
        }
        return Pagination(hasNext, result)
    }

    // 返回GITCI保存的流水线
    fun getGitCIPipelines(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        search: String?
    ): Pagination<StreamGitProjectPipeline> {
        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val gitProjectId = getGitProjectId(projectId)
        val pipelines = pipelineResourceDao.getAppDataByGitProjectId(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            keyword = search,
            offset = limit.offset,
            limit = limit.limit,
            orderBy = sortType ?: PipelineSortType.CREATE_TIME
        )
        if (pipelines.isNullOrEmpty()) {
            return Pagination(false, emptyList())
        }
        return Pagination(
            pipelines.size == pageSize,
            pipelines.map {
                StreamGitProjectPipeline(
                    gitProjectId = gitProjectId,
                    pipelineId = it.pipelineId,
                    filePath = it.filePath,
                    displayName = it.displayName,
                    enabled = it.enabled,
                    creator = it.creator,
                    latestBuildBranch = null
                )
            }
        )
    }

    fun getGitCIPipeline(
        projectId: String,
        pipelineId: String
    ): StreamGitProjectPipeline? {
        val gitProjectId = getGitProjectId(projectId)
        val pipeline = pipelineResourceDao.getPipelineById(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId
        ) ?: return null
        with(pipeline) {
            return StreamGitProjectPipeline(
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                filePath = filePath,
                displayName = displayName,
                enabled = enabled,
                creator = creator,
                latestBuildBranch = null
            )
        }
    }

    private fun getGitProjectId(projectId: String): Long {
        return if (projectId.trim().startsWith("git")) {
            projectId.removePrefix("git_").toLong()
        } else {
            projectId.toLong()
        }
    }
}
