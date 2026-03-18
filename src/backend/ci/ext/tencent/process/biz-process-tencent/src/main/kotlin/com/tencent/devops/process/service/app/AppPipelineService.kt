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

package com.tencent.devops.process.service.app

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.BSCCProjectApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.engine.pojo.builds.BuildHistoryQueryParam
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.app.pipeline.AppPipeline
import com.tencent.devops.process.pojo.app.pipeline.AppPipelineHistory
import com.tencent.devops.process.pojo.app.pipeline.AppProject
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.project.api.service.ServiceProjectResource
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * App流水线构建历史查询请求参数
 */
@Schema(title = "App流水线构建历史查询请求参数")
data class AppPipelineHistoryQueryReq(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "第几页")
    val page: Int? = null,
    @get:Schema(title = "每页条数")
    val pageSize: Int? = null,
    @get:Schema(title = "渠道号")
    val channelCode: ChannelCode = ChannelCode.getRequestChannelCode(),
    @get:Schema(title = "是否校验权限")
    val checkPermission: Boolean = true,
    @get:Schema(title = "代码库分支")
    val materialBranch: List<String>? = null,
    @get:Schema(title = "是否调试")
    val debug: Boolean? = null,
    @get:Schema(title = "触发代码库")
    val triggerAlias: List<String>? = null,
    @get:Schema(title = "触发分支")
    val triggerBranch: List<String>? = null,
    @get:Schema(title = "触发人")
    val triggerUser: List<String>? = null,
    @get:Schema(title = "触发事件类型")
    val triggerEventTypes: List<String>? = null,
    @get:Schema(title = "触发节点HashId")
    val triggerNodeHashIds: List<String>? = null
)

@Service
class AppPipelineService @Autowired constructor(
    private val authProjectApi: AuthProjectApi,
    private val bkCCProjectApi: BSCCProjectApi,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val client: Client,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AppPipelineService::class.java)
    }

    fun listProjects(
        userId: String,
        page: Int,
        pageSize: Int,
        channelCode: ChannelCode = ChannelCode.getRequestChannelCode()
    ): Page<AppProject> {
        var beginTime = System.currentTimeMillis()
        val projectIds = authProjectApi.getUserProjects(pipelineAuthServiceCode, userId, null)
        logger.info("get project ids time: ${System.currentTimeMillis() - beginTime}")
        beginTime = System.currentTimeMillis()

        // 遍历获取所有项目信息
        val bkProjectInfos = bkCCProjectApi.getProjectListAsOuter(projectIds.toSet()).associateBy { it.projectCode }
        logger.info("get project info: ${System.currentTimeMillis() - beginTime}")

        val projects = mutableListOf<AppProject>()
        bkProjectInfos.values.forEach { project ->
            projects.add(
                AppProject(
                    projectId = project.projectCode,
                    activePipelineCount = 0,
                    projectName = project.projectName,
                    projectLogo = project.logoAddr,
                    approvalStatus = project.approvalStatus
                )
            )
        }

        return Page(count = projects.count().toLong(), page = -1, pageSize = -1, totalPages = 1, records = projects)
    }

    fun listPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode = ChannelCode.getRequestChannelCode(),
        sortType: PipelineSortType = PipelineSortType.CREATE_TIME,
        checkPermission: Boolean = true
    ): PipelinePage<AppPipeline> {
        val result = pipelineListFacadeService.listPermissionPipeline(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            sortType = sortType,
            channelCode = channelCode,
            checkPermission = checkPermission
        )

        // 生成结果
        val appPipelines = mutableListOf<AppPipeline>()

        val projectInfoList = client.get(ServiceProjectResource::class).listByProjectCode(setOf(projectId)).data
        val projectInfo = if (projectInfoList.isNullOrEmpty()) null else projectInfoList[0]
        val projectName = projectInfo?.projectName ?: ""
        val logoAddr = projectInfo?.logoAddr ?: ""

        result.records.filter { it.hasPermission }.map {
            with(it) {
                appPipelines.add(
                    AppPipeline(
                        projectId = projectId,
                        projectName = projectName,
                        pipelineId = pipelineId,
                        pipelineName = pipelineName,
                        pipelineDesc = pipelineDesc ?: "",
                        latestBuildStatus = latestBuildStatus,
                        latestBuildNum = latestBuildNum,
                        latestBuildId = latestBuildId,
                        latestBuildStartTime = latestBuildStartTime,
                        latestBuildEndTime = latestBuildEndTime,
                        latestBuildUser = latestBuildUserId,
                        pipelineVersion = pipelineVersion,
                        canManualStartup = canManualStartup,
                        hasCollect = hasCollect,
                        deploymentTime = deploymentTime,
                        createTime = createTime,
                        logoUrl = logoAddr
                    )
                )
            }
        }
        return PipelinePage(
            count = result.count,
            page = result.page,
            pageSize = result.pageSize,
            totalPages = result.totalPages,
            records = appPipelines,
            hasCreatePermission = result.hasCreatePermission,
            hasPipelines = result.hasPipelines,
            hasFavorPipelines = result.hasFavorPipelines,
            hasPermissionPipelines = result.hasPermissionPipelines,
            currentView = result.currentView
        )
    }

    fun listPipelineHistory(request: AppPipelineHistoryQueryReq): Page<AppPipelineHistory> {
        val queryParam = BuildHistoryQueryParam(
            projectId = request.projectId,
            pipelineId = request.pipelineId,
            materialBranch = request.materialBranch,
            debug = request.debug,
            triggerAlias = request.triggerAlias,
            triggerBranch = request.triggerBranch,
            triggerUser = request.triggerUser,
            triggerEventTypes = request.triggerEventTypes,
            triggerNodeHashIds = request.triggerNodeHashIds
        )
        val result = pipelineBuildFacadeService.getHistoryBuild(
            userId = request.userId,
            page = request.page,
            pageSize = request.pageSize,
            queryParam = queryParam
        )
        val histories = result.records.map { h ->
            val packageVersion = h.artifactList?.joinToString(";") {
                it.appVersion.orEmpty()
            }.orEmpty()
            AppPipelineHistory(
                projectId = request.projectId,
                pipelineId = request.pipelineId,
                buildId = h.id,
                userId = h.userId,
                trigger = StartType.toStartType(h.trigger),
                buildNum = h.buildNum,
                startTime = h.startTime,
                endTime = h.endTime,
                status = h.status,
                curTimestamp = h.currentTimestamp,
                pipelineVersion = h.pipelineVersion,
                packageVersion = packageVersion
            ).apply {
                isMobileStart = h.isMobileStart
            }
        }
        return Page(
            count = result.count,
            page = result.page,
            pageSize = result.pageSize,
            totalPages = result.totalPages,
            records = histories
        )
    }
}
