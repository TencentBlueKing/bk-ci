/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.process.service.app

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.BSCCProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthProject
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.app.pipeline.AppPipeline
import com.tencent.devops.process.pojo.app.pipeline.AppPipelineHistory
import com.tencent.devops.process.pojo.app.pipeline.AppProject
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@Service
class AppPipelineService @Autowired constructor(
    private val bkAuthProjectApi: BSAuthProjectApi,
    private val bkCCProjectApi: BSCCProjectApi,
    private val buildService: PipelineBuildService,
    private val pipelineService: PipelineService,
    private val client: Client,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AppPipelineService::class.java)
    }

    private val executor = Executors.newFixedThreadPool(16)

    fun listProjects(
        userId: String,
        page: Int,
        pageSize: Int,
        channelCode: ChannelCode = ChannelCode.BS
    ): Page<AppProject> {
        var beginTime = System.currentTimeMillis()
        val projectIds = bkAuthProjectApi.getUserProjects(bsPipelineAuthServiceCode, userId, null)
        logger.info("get project ids time: ${System.currentTimeMillis() - beginTime}")
        beginTime = System.currentTimeMillis()

        // 遍历获取所有项目信息
        val bkProjectInfos =
            bkCCProjectApi.getProjectListAsOuter(projectIds.toSet()).map { it.projectCode to it }.toMap()
        logger.info("get project info: ${System.currentTimeMillis() - beginTime}")
        beginTime = System.currentTimeMillis()

        // 获取名字和logo
        val tasks = executor.invokeAll(bkProjectInfos.values.filter { it.approvalStatus == "2" }.map { project ->
            AppProjectTask(pipelineService, project, userId)
        })
        val projects = tasks.map { it.get() }
        logger.info("get project name & logo: ${System.currentTimeMillis() - beginTime}")

        return Page(projects.count().toLong(), -1, -1, 1, projects)
    }

    fun listPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode = ChannelCode.BS,
        sortType: PipelineSortType = PipelineSortType.CREATE_TIME,
        checkPermission: Boolean = true
    ): PipelinePage<AppPipeline> {
        val result = pipelineService.listPermissionPipeline(
            userId,
            projectId,
            page,
            pageSize,
            sortType,
            channelCode,
            checkPermission
        )

        // 生成结果
        val appPipelines = mutableListOf<AppPipeline>()

        val projectInfoList = client.get(ServiceProjectResource::class).listByProjectCode(setOf(projectId)).data
        val projectInfo = if (projectInfoList == null || projectInfoList.isEmpty()) null else projectInfoList[0]
        val projectName = projectInfo?.projectName ?: ""
        var logoAddr = projectInfo?.logoAddr ?: ""
        logoAddr = "https://download.bkdevops.qq.com/images" + logoAddr.removePrefix("http://radosgw.open.oa.com")

        result.records.filter { it.hasPermission }.map {
            with(it) {
                appPipelines.add(
                    AppPipeline(
                        projectId,
                        projectName,
                        pipelineId,
                        pipelineName,
                        pipelineDesc ?: "",
                        latestBuildStatus,
                        latestBuildNum,
                        latestBuildId,
                        latestBuildStartTime,
                        latestBuildEndTime,
                        latestBuildUserId,
                        pipelineVersion,
                        canManualStartup,
                        hasCollect,
                        deploymentTime,
                        createTime,
                        logoAddr
                    )
                )
            }
        }
        return PipelinePage(
            result.count,
            result.page,
            result.pageSize,
            result.totalPages,
            appPipelines,
            result.hasCreatePermission,
            result.hasPipelines,
            result.hasFavorPipelines,
            result.hasPermissionPipelines,
            result.currentView
        )
    }

    fun listPipelineHistory(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode = ChannelCode.BS,
        checkPermission: Boolean = true,
        materialBranch: List<String>?
    ): Page<AppPipelineHistory> {

        val result = buildService.getHistoryBuild(
            userId,
            projectId,
            pipelineId,
            page,
            pageSize,
            null,
            null,
            materialBranch,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        val histories = result.records.map { h ->
            AppPipelineHistory(
                projectId,
                pipelineId,
                h.id,
                h.userId,
                StartType.toStartType(h.trigger),
                h.buildNum,
                h.startTime,
                h.endTime,
                h.status,
                h.currentTimestamp,
                h.pipelineVersion
            ).apply {
                isMobileStart = h.isMobileStart
            }
        }
        return Page(result.count, result.page, result.pageSize, result.totalPages, histories)
    }

    fun getHistoryConditionBranch(userId: String, projectId: String, pipelineId: String, alias: List<String>?) =
        buildService.getHistoryConditionBranch(userId, projectId, pipelineId, alias)

    class AppProjectTask(
        private val pipelineService: PipelineService,
        private val project: BkAuthProject,
        private val userId: String
    ) : Callable<AppProject> {
        override fun call(): AppProject {
            val count = pipelineService.listPermissionPipelineCount(userId, project.projectCode)
            val logoAddr = if (project.logoAddr.startsWith("http://radosgw.open.oa.com")) {
                "https://dev-download.bkdevops.qq.com/images" + project.logoAddr.removePrefix("http://radosgw.open.oa.com")
            } else {
                project.logoAddr
            }
            return AppProject(project.projectCode, count, project.projectName, logoAddr, project.approvalStatus)
        }
    }
}
