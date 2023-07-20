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

package com.tencent.devops.process.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.event.pojo.measure.PipelineLabelRelateInfo
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.TTemplatePipeline
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_ID_NOT_PROJECT_PIPELINE
import com.tencent.devops.process.dao.PipelineFavorDao
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.label.PipelineLabelPipelineDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.pojo.PipelineFilterByLabelInfo
import com.tencent.devops.process.engine.pojo.PipelineFilterParam
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineDetailInfo
import com.tencent.devops.process.pojo.PipelineIdAndName
import com.tencent.devops.process.pojo.PipelineIdInfo
import com.tencent.devops.process.pojo.PipelinePermissions
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.classify.PipelineGroupLabels
import com.tencent.devops.process.pojo.classify.PipelineViewAndPipelines
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByCreator
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByLabel
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByName
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.classify.enums.Condition
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.code.WebhookInfo
import com.tencent.devops.process.pojo.pipeline.PipelineCount
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.template.TemplatePipelineInfo
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.pipeline.PipelineStatusService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.service.view.PipelineViewService
import com.tencent.devops.process.util.BuildMsgUtils
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_FAVORITE_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_LIST_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_RECENT_USE
import com.tencent.devops.process.utils.PIPELINE_VIEW_UNCLASSIFIED
import com.tencent.devops.quality.api.v2.pojo.response.QualityPipeline
import com.tencent.devops.scm.utils.code.git.GitUtils
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.jooq.Record4
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch

@Suppress("ALL")
@Service
class PipelineListFacadeService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewService: PipelineViewService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelineStatusService: PipelineStatusService,
    private val processJmxApi: ProcessJmxApi,
    private val dslContext: DSLContext,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineBuildTaskDao: PipelineBuildTaskDao,
    private val pipelineFavorDao: PipelineFavorDao,
    private val pipelineLabelPipelineDao: PipelineLabelPipelineDao,
    private val pipelineRecentUseService: PipelineRecentUseService
) {

    @Value("\${process.deletedPipelineStoreDays:30}")
    private val deletedPipelineStoreDays: Int = 30

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineListFacadeService::class.java)
    }

    fun sortPipelines(pipelines: MutableList<Pipeline>, sortType: PipelineSortType) {
        if (pipelines.isEmpty()) {
            return
        }
        pipelines.sortWith { a, b ->
            when (sortType) {
                PipelineSortType.NAME -> {
                    a.pipelineName.toLowerCase().compareTo(b.pipelineName.toLowerCase())
                }

                PipelineSortType.CREATE_TIME -> {
                    b.createTime.compareTo(a.createTime)
                }

                PipelineSortType.UPDATE_TIME -> {
                    b.deploymentTime.compareTo(a.deploymentTime)
                }

                PipelineSortType.LAST_EXEC_TIME -> {
                    b.deploymentTime.compareTo(a.latestBuildStartTime ?: 0)
                }
            }
        }
    }

    fun getBatchPipelinesWithModel(
        userId: String,
        projectId: String,
        pipelineIds: List<String>,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): List<Pipeline> {
        if (checkPermission) {
            val permission = AuthPermission.VIEW
            val hasViewPermission = pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                permission = permission
            )
            if (!hasViewPermission) {
                val permissionMsg = permission.getI18n(I18nUtil.getLanguage(userId))
                throw ErrorCodeException(
                    statusCode = Response.Status.FORBIDDEN.statusCode,
                    errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                    params = arrayOf(permissionMsg)
                )
            }
        }
        val buildPipelineRecords = pipelineBuildSummaryDao.listPipelineInfoBuildSummary(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = channelCode,
            sortType = null,
            pipelineIds = pipelineIds,
            userId = userId
        )

        return buildPipelines(
            pipelineInfoRecords = buildPipelineRecords,
            projectId = projectId,
            queryModelFlag = true
        )
    }

    fun listPipelineInfo(userId: String, projectId: String, pipelineIdList: List<String>?): List<Pipeline> {
        val pipelines = listPermissionPipeline(
            userId = userId,
            projectId = projectId,
            page = null,
            pageSize = null,
            sortType = PipelineSortType.CREATE_TIME,
            channelCode = ChannelCode.BS,
            checkPermission = false
        )

        return if (pipelineIdList == null) {
            pipelines.records
        } else {
            pipelines.records.filter { pipelineIdList.contains(it.pipelineId) }
        }
    }

    fun listPipelineInfo(
        userId: String,
        projectId: String,
        pipelineIdList: Collection<String>?,
        templateIdList: Collection<String>? = null
    ): List<Pipeline> {
        val resultPipelineIds = mutableSetOf<String>()

        if (pipelineIdList != null) {
            resultPipelineIds.addAll(pipelineIdList)
        }

        if (templateIdList != null) {
            val templatePipelineIds = templatePipelineDao.listPipeline(
                dslContext = dslContext,
                projectId = projectId,
                instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
                templateIds = templateIdList
            ).map { it[KEY_PIPELINE_ID] as String }
            resultPipelineIds.addAll(templatePipelineIds)
        }

        val pipelines = mutableListOf<Pipeline>()
        val buildPipelineRecords = pipelineRuntimeService.getBuildPipelineRecords(
            projectId = projectId,
            channelCode = ChannelCode.BS,
            pipelineIds = resultPipelineIds
        )
        if (buildPipelineRecords.isNotEmpty) {
            pipelines.addAll(
                buildPipelines(
                    pipelineInfoRecords = buildPipelineRecords,
                    favorPipelines = emptyList(),
                    authPipelines = emptyList(),
                    projectId = projectId
                )
            )
        }
        return pipelines
    }

    fun listPermissionPipeline(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): PipelinePage<Pipeline> {

        val watcher = Watcher(id = "listPermissionPipeline|$projectId|$userId")
        try {
            val hasCreatePermission = if (!checkPermission) {
                true
            } else {
                watcher.start("checkPerm")
                val validateUserResourcePermission = pipelinePermissionService.checkPipelinePermission(
                    userId = userId, projectId = projectId, permission = AuthPermission.CREATE
                )
                watcher.stop()
                validateUserResourcePermission
            }

            val pageNotNull = if (page == null || page <= 0) 1 else page
            val pageSizeNotNull = if (pageSize == null || pageSize <= 0) 10 else pageSize
            val hasPermissionList = if (checkPermission) {
                watcher.start("perm_r_perm")
                val hasPermissionList = pipelinePermissionService.getResourceByPermission(
                    userId = userId, projectId = projectId, permission = AuthPermission.LIST
                )
                watcher.stop()
                if (hasPermissionList.isEmpty()) {
                    return PipelinePage(
                        page = pageNotNull,
                        pageSize = pageSizeNotNull,
                        count = 0,
                        records = emptyList(),
                        hasCreatePermission = hasCreatePermission,
                        hasPipelines = false,
                        hasFavorPipelines = false,
                        hasPermissionPipelines = false,
                        currentView = null
                    )
                }
                hasPermissionList
            } else {
                emptyList()
            }

            watcher.start("s_r_summary")
            val buildPipelineRecords = pipelineRuntimeService.getBuildPipelineRecords(
                projectId = projectId,
                channelCode = channelCode,
                pipelineIds = hasPermissionList,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                sortType = sortType
            )
            val buildPipelineCount = pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
                dslContext = dslContext,
                projectId = projectId,
                channelCode = channelCode,
                pipelineIds = hasPermissionList,
                userId = userId
            )
            watcher.stop()

            watcher.start("s_r_fav")
            // 得到列表和是否有收藏的流水线
            val (pagePipelines, hasFavorPipelines) = if (buildPipelineRecords.isNotEmpty) {
                val favorPipelines = pipelineGroupService.getFavorPipelines(userId = userId, projectId = projectId)
                val pipelines = buildPipelines(
                    pipelineInfoRecords = buildPipelineRecords,
                    favorPipelines = favorPipelines,
                    authPipelines = hasPermissionList,
                    projectId = projectId
                )
                pipelines to favorPipelines.isNotEmpty()
            } else {
                mutableListOf<Pipeline>() to false
            }
            watcher.stop()

            return PipelinePage(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = buildPipelineCount,
                records = pagePipelines,
                hasCreatePermission = hasCreatePermission,
                hasPipelines = true,
                hasFavorPipelines = hasFavorPipelines,
                hasPermissionPipelines = hasPermissionList.isNotEmpty(),
                currentView = null
            )
        } finally {
            LogUtils.printCostTimeWE(watcher)
            processJmxApi.execute(ProcessJmxApi.LIST_APP_PIPELINES, watcher.totalTimeMillis)
        }
    }

    fun hasPermissionList(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        excludePipelineId: String?,
        page: Int?,
        pageSize: Int?
    ): SQLPage<Pipeline> {

        val watcher = Watcher(id = "hasPermissionList|$projectId|$userId")
        try {
            watcher.start("perm_r_perm")
            val hasPermissionList = pipelinePermissionService.getResourceByPermission(
                userId = userId, projectId = projectId, permission = authPermission
            ).toMutableList()
            watcher.stop()
            watcher.start("s_r_summary")
            if (excludePipelineId != null) {
                // 移除需排除的流水线ID
                hasPermissionList.remove(excludePipelineId)
            }
            val buildPipelineRecords = pipelineRuntimeService.getBuildPipelineRecords(
                projectId = projectId,
                channelCode = ChannelCode.BS,
                pipelineIds = hasPermissionList,
                page = page,
                pageSize = pageSize
            )
            watcher.stop()

            watcher.start("s_r_fav")
            val count = buildPipelineRecords.size + 0L
            val pagePipelines =
                if (count > 0) {
                    val favorPipelines = pipelineGroupService.getFavorPipelines(userId = userId, projectId = projectId)
                    buildPipelines(
                        pipelineInfoRecords = buildPipelineRecords,
                        favorPipelines = favorPipelines,
                        authPipelines = emptyList(),
                        projectId = projectId
                    )
                } else {
                    mutableListOf()
                }

            watcher.stop()
            return SQLPage(count = count, records = pagePipelines)
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
            processJmxApi.execute(ProcessJmxApi.LIST_APP_PIPELINES, watcher.totalTimeMillis)
        }
    }

    /**
     * 根据视图ID获取流水线编
     * 其中 PIPELINE_VIEW_FAVORITE_PIPELINES，PIPELINE_VIEW_MY_PIPELINES，PIPELINE_VIEW_ALL_PIPELINES
     * 分别对应 我的收藏，我的流水线，全部流水线
     */
    fun listViewPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType,
        channelCode: ChannelCode,
        viewId: String,
        checkPermission: Boolean = true,
        filterByPipelineName: String? = null,
        filterByCreator: String? = null,
        filterByLabels: String? = null,
        filterInvalid: Boolean = false,
        filterByViewIds: String? = null,
        collation: PipelineCollation = PipelineCollation.DEFAULT,
        showDelete: Boolean = false
    ): PipelineViewPipelinePage<Pipeline> {
        val watcher = Watcher(id = "listViewPipelines|$projectId|$userId")
        watcher.start("perm_r_perm")
        val authPipelines = pipelinePermissionService.getResourceByPermission(
            userId = userId, projectId = projectId, permission = AuthPermission.LIST
        )
        watcher.stop()

        watcher.start("s_r_summary")
        try {
            val favorPipelines = pipelineGroupService.getFavorPipelines(userId = userId, projectId = projectId)
            val (
                filterByPipelineNames: List<PipelineViewFilterByName>,
                filterByPipelineCreators: List<PipelineViewFilterByCreator>,
                filterByPipelineLabels: List<PipelineViewFilterByLabel>
            ) = generatePipelineFilterInfo(
                projectId = projectId,
                filterByName = filterByPipelineName,
                filterByCreator = filterByCreator,
                filterByLabels = filterByLabels
            )
            val pipelineFilterParamList = mutableListOf<PipelineFilterParam>()
            val pipelineFilterParam = PipelineFilterParam(
                logic = Logic.AND,
                filterByPipelineNames = filterByPipelineNames,
                filterByPipelineCreators = filterByPipelineCreators,
                filterByLabelInfo = PipelineFilterByLabelInfo(
                    filterByLabels = filterByPipelineLabels,
                    labelToPipelineMap = filterByPipelineLabels.generateLabelToPipelineMap(projectId)
                )
            )
            pipelineFilterParamList.add(pipelineFilterParam)

            val pipelineIds = mutableSetOf<String>()
            val viewIdList = listOf(
                PIPELINE_VIEW_FAVORITE_PIPELINES,
                PIPELINE_VIEW_MY_PIPELINES,
                PIPELINE_VIEW_ALL_PIPELINES,
                PIPELINE_VIEW_MY_LIST_PIPELINES,
                PIPELINE_VIEW_UNCLASSIFIED,
                PIPELINE_VIEW_RECENT_USE
            )
            val includeDelete = showDelete && (PIPELINE_VIEW_RECENT_USE == viewId || !viewIdList.contains(viewId))

            if (!viewIdList.contains(viewId)) { // 已分组的视图
                pipelineIds.addAll(pipelineViewGroupService.listPipelineIdsByViewId(projectId, viewId))
            } else if (viewId == PIPELINE_VIEW_UNCLASSIFIED) { // 非分组的视图
                val allPipelineIds = pipelineInfoDao.listPipelineIdByProject(dslContext, projectId).toMutableSet()
                pipelineIds.addAll(
                    allPipelineIds.subtract(pipelineViewGroupService.getClassifiedPipelineIds(projectId).toSet())
                )
                // 避免过滤器为空的情况
                if (pipelineIds.isEmpty()) {
                    pipelineIds.add("##NONE##")
                }
            } else if (viewId == PIPELINE_VIEW_RECENT_USE) { // 最近访问
                pipelineIds.addAll(pipelineRecentUseService.listPipelineIds(userId, projectId))
            }
            // 剔除掉filterByViewIds
            if (filterByViewIds != null) {
                val pipelineIdsByFilterViewIds =
                    pipelineViewGroupService.listPipelineIdsByViewIds(projectId, filterByViewIds.split(",")).toSet()
                if (pipelineIds.isEmpty()) {
                    pipelineIds.addAll(pipelineIdsByFilterViewIds)
                } else {
                    pipelineIds.retainAll(pipelineIdsByFilterViewIds)
                }
            }

            pipelineViewService.addUsingView(userId = userId, projectId = projectId, viewId = viewId)

            // 查询有权限查看的流水线总数
            val totalAvailablePipelineSize = pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
                dslContext = dslContext,
                projectId = projectId,
                channelCode = channelCode,
                pipelineIds = pipelineIds,
                favorPipelines = favorPipelines,
                authPipelines = authPipelines,
                viewId = viewId,
                pipelineFilterParamList = pipelineFilterParamList,
                permissionFlag = true,
                includeDelete = includeDelete,
                userId = userId
            )

            // 查询无权限查看的流水线总数
            val totalInvalidPipelineSize =
                if (filterInvalid) 0 else pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
                    dslContext = dslContext,
                    projectId = projectId,
                    channelCode = channelCode,
                    pipelineIds = pipelineIds,
                    viewId = viewId,
                    favorPipelines = favorPipelines,
                    authPipelines = authPipelines,
                    pipelineFilterParamList = pipelineFilterParamList,
                    permissionFlag = false,
                    includeDelete = includeDelete,
                    userId = userId
                )
            val pipelineList = mutableListOf<Pipeline>()
            val totalSize = totalAvailablePipelineSize + totalInvalidPipelineSize
            if (includeDelete) {
                handlePipelineQueryList(
                    pipelineList = pipelineList,
                    projectId = projectId,
                    channelCode = channelCode,
                    sortType = sortType,
                    pipelineIds = pipelineIds,
                    favorPipelines = favorPipelines,
                    authPipelines = authPipelines,
                    viewId = viewId,
                    pipelineFilterParamList = pipelineFilterParamList,
                    permissionFlag = null,
                    page = page,
                    pageSize = pageSize,
                    includeDelete = true,
                    collation = collation,
                    userId = userId
                )
            } else if ((null != page && null != pageSize) && !(page == 1 && pageSize == -1)) {
                // 判断可用的流水线是否已到最后一页
                val totalAvailablePipelinePage = PageUtil.calTotalPage(pageSize, totalAvailablePipelineSize)
                if (page < totalAvailablePipelinePage) {
                    // 当前页未到可用流水线最后一页，不需要处理临界点（最后一页）的情况
                    handlePipelineQueryList(
                        pipelineList = pipelineList,
                        projectId = projectId,
                        channelCode = channelCode,
                        sortType = sortType,
                        pipelineIds = pipelineIds,
                        favorPipelines = favorPipelines,
                        authPipelines = authPipelines,
                        viewId = viewId,
                        pipelineFilterParamList = pipelineFilterParamList,
                        permissionFlag = true,
                        page = page,
                        pageSize = pageSize,
                        includeDelete = includeDelete,
                        collation = collation,
                        userId = userId
                    )
                } else if (page == totalAvailablePipelinePage && totalAvailablePipelineSize > 0) {
                    //  查询可用流水线最后一页不满页的数量
                    val lastPageRemainNum = pageSize - totalAvailablePipelineSize % pageSize
                    handlePipelineQueryList(
                        pipelineList = pipelineList,
                        projectId = projectId,
                        channelCode = channelCode,
                        sortType = sortType,
                        pipelineIds = pipelineIds,
                        favorPipelines = favorPipelines,
                        authPipelines = authPipelines,
                        viewId = viewId,
                        pipelineFilterParamList = pipelineFilterParamList,
                        permissionFlag = true,
                        page = page,
                        pageSize = pageSize,
                        includeDelete = includeDelete,
                        collation = collation,
                        userId = userId
                    )
                    // 可用流水线最后一页不满页的数量需用不可用的流水线填充
                    if (lastPageRemainNum > 0 && totalInvalidPipelineSize > 0) {
                        handlePipelineQueryList(
                            pipelineList = pipelineList,
                            projectId = projectId,
                            channelCode = channelCode,
                            sortType = sortType,
                            pipelineIds = pipelineIds,
                            favorPipelines = favorPipelines,
                            authPipelines = authPipelines,
                            viewId = viewId,
                            pipelineFilterParamList = pipelineFilterParamList,
                            permissionFlag = false,
                            page = 1,
                            pageSize = lastPageRemainNum.toInt(),
                            includeDelete = includeDelete,
                            collation = collation,
                            userId = userId
                        )
                    }
                } else if (totalInvalidPipelineSize > 0) {
                    // 当前页大于可用流水线最后一页，需要排除掉可用流水线最后一页不满页的数量用不可用的流水线填充的情况
                    val lastPageRemainNum =
                        if (totalAvailablePipelineSize > 0) pageSize - totalAvailablePipelineSize % pageSize else 0
                    handlePipelineQueryList(
                        pipelineList = pipelineList,
                        projectId = projectId,
                        channelCode = channelCode,
                        sortType = sortType,
                        pipelineIds = pipelineIds,
                        favorPipelines = favorPipelines,
                        authPipelines = authPipelines,
                        viewId = viewId,
                        pipelineFilterParamList = pipelineFilterParamList,
                        permissionFlag = false,
                        page = page - totalAvailablePipelinePage,
                        pageSize = pageSize,
                        pageOffsetNum = lastPageRemainNum.toInt(),
                        includeDelete = includeDelete,
                        collation = collation,
                        userId = userId
                    )
                }
            } else {
                // 不分页查询
                handlePipelineQueryList(
                    pipelineList = pipelineList,
                    projectId = projectId,
                    channelCode = channelCode,
                    sortType = sortType,
                    pipelineIds = pipelineIds,
                    favorPipelines = favorPipelines,
                    authPipelines = authPipelines,
                    viewId = viewId,
                    pipelineFilterParamList = pipelineFilterParamList,
                    permissionFlag = true,
                    page = page,
                    pageSize = pageSize,
                    includeDelete = includeDelete,
                    collation = collation,
                    userId = userId
                )

                if (filterInvalid) {
                    handlePipelineQueryList(
                        pipelineList = pipelineList,
                        projectId = projectId,
                        channelCode = channelCode,
                        sortType = sortType,
                        pipelineIds = pipelineIds,
                        favorPipelines = favorPipelines,
                        authPipelines = authPipelines,
                        viewId = viewId,
                        pipelineFilterParamList = pipelineFilterParamList,
                        permissionFlag = false,
                        page = page,
                        pageSize = pageSize,
                        includeDelete = includeDelete,
                        collation = collation,
                        userId = userId
                    )
                }
            }
            watcher.stop()

            return PipelineViewPipelinePage(
                page = page ?: 1,
                pageSize = pageSize ?: totalSize.toInt(),
                count = totalSize,
                records = fillPipelinePermissions(
                    userId = userId,
                    projectId = projectId,
                    pipelineList = pipelineList
                )
            )
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
            processJmxApi.execute(ProcessJmxApi.LIST_NEW_PIPELINES, watcher.totalTimeMillis)
        }
    }

    private fun fillPipelinePermissions(
        userId: String,
        projectId: String,
        pipelineList: List<Pipeline>
    ): List<Pipeline> {
        val permissionToListMap = pipelinePermissionService.filterPipelines(
            userId = userId,
            projectId = projectId,
            authPermissions = setOf(
                AuthPermission.MANAGE,
                AuthPermission.VIEW,
                AuthPermission.DELETE,
                AuthPermission.SHARE,
                AuthPermission.EDIT,
                AuthPermission.DOWNLOAD,
                AuthPermission.EXECUTE
            ),
            pipelineIds = pipelineList.map { it.pipelineId }
        )
        return pipelineList.map { pipeline ->
            pipeline.copy(
                permissions = PipelinePermissions(
                    canManage = permissionToListMap[AuthPermission.MANAGE]?.contains(pipeline.pipelineId) ?: false,
                    canDelete = permissionToListMap[AuthPermission.DELETE]?.contains(pipeline.pipelineId) ?: false,
                    canView = permissionToListMap[AuthPermission.VIEW]?.contains(pipeline.pipelineId) ?: false,
                    canEdit = permissionToListMap[AuthPermission.EDIT]?.contains(pipeline.pipelineId) ?: false,
                    canExecute = permissionToListMap[AuthPermission.EXECUTE]?.contains(pipeline.pipelineId) ?: false,
                    canDownload = permissionToListMap[AuthPermission.DOWNLOAD]?.contains(pipeline.pipelineId) ?: false,
                    canShare = permissionToListMap[AuthPermission.SHARE]?.contains(pipeline.pipelineId) ?: false
                )
            )
        }
    }

    fun getCount(userId: String, projectId: String): PipelineCount {
        val authPipelines = pipelinePermissionService.getResourceByPermission(
            userId = userId, projectId = projectId, permission = AuthPermission.LIST
        )
        val favorPipelines = pipelineGroupService.getFavorPipelines(userId = userId, projectId = projectId)
        val recentUsePipelines = pipelineRecentUseService.listPipelineIds(userId, projectId)
        val totalCount = pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = ChannelCode.BS,
            authPipelines = authPipelines,
            favorPipelines = favorPipelines,
            viewId = PIPELINE_VIEW_ALL_PIPELINES,
            includeDelete = false,
            userId = userId
        ).toInt()
        val myFavoriteCount = pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = ChannelCode.BS,
            authPipelines = authPipelines,
            favorPipelines = favorPipelines,
            viewId = PIPELINE_VIEW_FAVORITE_PIPELINES,
            includeDelete = false,
            userId = userId
        ).toInt()
        val myPipelineCount = pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = ChannelCode.BS,
            authPipelines = authPipelines,
            favorPipelines = favorPipelines,
            viewId = PIPELINE_VIEW_MY_PIPELINES,
            includeDelete = false,
            userId = userId
        ).toInt()
        val recentUseCount = pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = ChannelCode.BS,
            authPipelines = authPipelines,
            favorPipelines = favorPipelines,
            viewId = PIPELINE_VIEW_RECENT_USE,
            includeDelete = false,
            userId = userId,
            pipelineIds = recentUsePipelines
        ).toInt()
        val recycleCount = pipelineInfoDao.countDeletePipeline(dslContext, projectId, deletedPipelineStoreDays.toLong())
        return PipelineCount(totalCount, myFavoriteCount, myPipelineCount, recycleCount, recentUseCount)
    }

    private fun handlePipelineQueryList(
        pipelineList: MutableList<Pipeline>,
        projectId: String,
        channelCode: ChannelCode,
        sortType: PipelineSortType? = null,
        pipelineIds: Collection<String>? = null,
        favorPipelines: List<String> = emptyList(),
        authPipelines: List<String> = emptyList(),
        viewId: String? = null,
        pipelineFilterParamList: List<PipelineFilterParam>? = null,
        permissionFlag: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null,
        pageOffsetNum: Int? = 0,
        includeDelete: Boolean? = false,
        collation: PipelineCollation = PipelineCollation.DEFAULT,
        userId: String
    ) {
        val pipelineRecords = pipelineBuildSummaryDao.listPipelineInfoBuildSummary(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = channelCode,
            sortType = sortType,
            pipelineIds = pipelineIds,
            favorPipelines = favorPipelines,
            authPipelines = authPipelines,
            viewId = viewId,
            pipelineFilterParamList = pipelineFilterParamList,
            permissionFlag = permissionFlag,
            page = page,
            pageSize = pageSize,
            pageOffsetNum = pageOffsetNum,
            includeDelete = includeDelete,
            collation = collation,
            userId = userId
        )
        pipelineList.addAll(
            buildPipelines(
                pipelineInfoRecords = pipelineRecords,
                favorPipelines = favorPipelines,
                authPipelines = authPipelines,
                projectId = projectId
            )
        )
    }

    private fun List<PipelineViewFilterByLabel>.generateLabelToPipelineMap(
        projectId: String
    ): Map<String, List<String>>? {
        var labelToPipelineMap: Map<String, List<String>>? = null
        if (isNotEmpty()) {
            val labelIds = mutableListOf<String>()
            forEach {
                labelIds.addAll(it.labelIds)
            }
            labelToPipelineMap = pipelineGroupService.getViewLabelToPipelinesMap(projectId, labelIds)
        }
        return labelToPipelineMap
    }

    fun filterViewPipelines(
        userId: String,
        projectId: String,
        pipelines: List<Pipeline>,
        viewId: String
    ): List<Pipeline> {
        val pipelineIds = pipelineViewGroupService.listPipelineIdsByViewId(projectId, viewId)
        return pipelines.filter { pipelineIds.contains(it.pipelineId) }
    }

    /**
     * 视图的基础上增加简单过滤
     */
    fun filterViewPipelines(
        projectId: String,
        pipelines: List<Pipeline>,
        filterByName: String?,
        filterByCreator: String?,
        filterByLabels: String?
    ): List<Pipeline> {
        logger.info("filter view pipelines $filterByName $filterByCreator $filterByLabels")

        val (filterByPipelineNames, filterByPipelineCreators, filterByPipelineLabels) = generatePipelineFilterInfo(
            projectId = projectId,
            filterByName = filterByName,
            filterByCreator = filterByCreator,
            filterByLabels = filterByLabels
        )

        if (filterByPipelineNames.isEmpty() && filterByPipelineCreators.isEmpty() && filterByPipelineLabels.isEmpty()) {
            return pipelines
        }

        return filterViewPipelines(
            projectId = projectId,
            pipelines = pipelines,
            logic = Logic.AND,
            filterByPipelineNames = filterByPipelineNames,
            filterByPipelineCreators = filterByPipelineCreators,
            filterByLabels = filterByPipelineLabels
        )
    }

    private fun generatePipelineFilterInfo(
        projectId: String,
        filterByName: String?,
        filterByCreator: String?,
        filterByLabels: String?
    ): Triple<List<PipelineViewFilterByName>, List<PipelineViewFilterByCreator>, List<PipelineViewFilterByLabel>> {
        val filterByPipelineNames = if (filterByName.isNullOrEmpty()) {
            emptyList()
        } else {
            listOf(PipelineViewFilterByName(Condition.LIKE, filterByName))
        }

        val filterByPipelineCreators = if (filterByCreator.isNullOrEmpty()) {
            emptyList()
        } else {
            listOf(PipelineViewFilterByCreator(Condition.INCLUDE, filterByCreator.split(",")))
        }

        val filterByPipelineLabels = if (filterByLabels.isNullOrEmpty()) {
            emptyList()
        } else {
            val labelIds = filterByLabels.split(",")
            val labelGroupToLabelMap = pipelineGroupService.getGroupToLabelsMap(projectId, labelIds)

            labelGroupToLabelMap.map {
                PipelineViewFilterByLabel(Condition.INCLUDE, it.key, it.value)
            }
        }
        return Triple(filterByPipelineNames, filterByPipelineCreators, filterByPipelineLabels)
    }

    /**
     * 视图过滤
     */
    private fun filterViewPipelines(
        projectId: String,
        pipelines: List<Pipeline>,
        logic: Logic,
        filterByPipelineNames: List<PipelineViewFilterByName>,
        filterByPipelineCreators: List<PipelineViewFilterByCreator>,
        filterByLabels: List<PipelineViewFilterByLabel>
    ): List<Pipeline> {

        // filter by pipeline name
        val nameFilterPipelines = if (filterByPipelineNames.isEmpty()) {
            if (logic == Logic.AND) pipelines else emptyList()
        } else {
            pipelines.filter { pipeline ->
                val name = pipeline.pipelineName
                var filter: Boolean = (logic == Logic.AND)
                filterByPipelineNames.forEach {
                    val result = it.condition.filter(it.pipelineName, name)
                    filter = if (logic == Logic.AND) {
                        filter && result
                    } else {
                        filter || result
                    }
                }
                filter
            }
        }
        logger.info("Filter by name size(${nameFilterPipelines.size})")

        if (logic == Logic.AND && nameFilterPipelines.isEmpty()) {
            logger.info("All pipelines filter by name $filterByPipelineNames")
            return nameFilterPipelines
        }

        // filter by creator, creator's condition is only include
        val creatorFilterPipelines = if (filterByPipelineCreators.isEmpty()) {
            if (logic == Logic.AND) pipelines else emptyList()
        } else {
            pipelines.filter { pipeline ->
                val user = pipeline.creator
                var filter: Boolean = (logic == Logic.AND)

                filterByPipelineCreators.forEach { filterByCreator ->
                    val result = filterByCreator.userIds.contains(user)

                    filter = if (logic == Logic.AND) {
                        filter && result
                    } else {
                        filter || result
                    }
                }
                filter
            }
        }
        logger.info("Filter by creator size(${creatorFilterPipelines.size})")

        if (logic == Logic.AND && creatorFilterPipelines.isEmpty()) {
            logger.info("All pipelines filter by creator $filterByPipelineCreators")
            return creatorFilterPipelines
        }

        // filter by label, label's condition is only include
        val labelIds = mutableListOf<String>()
        filterByLabels.forEach {
            labelIds.addAll(it.labelIds)
        }
        val labelFilterPipelines = if (filterByLabels.isEmpty()) {
            if (logic == Logic.AND) pipelines else emptyList()
        } else {
            val labelToPipelineMap = pipelineGroupService.getViewLabelToPipelinesMap(projectId, labelIds)

            pipelines.filter { pipeline ->
                val pipelineId = pipeline.pipelineId
                var filter: Boolean = (logic == Logic.AND)

                filterByLabels.forEach { filterByLabel ->
                    var result = false

                    filterByLabel.labelIds.forEach {
                        val labelPipelineIds = labelToPipelineMap[it]
                        if (labelPipelineIds != null) {
                            result = result || labelPipelineIds.contains(pipelineId)
                        }
                    }

                    filter = if (logic == Logic.AND) {
                        filter && result
                    } else {
                        filter || result
                    }
                }
                filter
            }
        }
        logger.info("Filter by label size(${labelFilterPipelines.size})")

        if (logic == Logic.AND && labelFilterPipelines.isEmpty()) {
            logger.info("All pipelines filter by label $filterByLabels")
            return labelFilterPipelines
        }

        return if (logic == Logic.AND) {
            nameFilterPipelines
                .intersect(creatorFilterPipelines.toSet())
                .intersect(labelFilterPipelines.toSet())
                .toList()
        } else {
            nameFilterPipelines
                .union(creatorFilterPipelines.asIterable())
                .union(labelFilterPipelines.asIterable())
                .toList()
        }
    }

    fun listViewAndPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): PipelineViewAndPipelines {
        val currentViewIdAndViewList = pipelineViewService.getCurrentViewIdAndList(userId, projectId)
        val currentViewId = currentViewIdAndViewList.first
        val currentViewList = currentViewIdAndViewList.second

        val pipelinePage = listViewPipelines(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            sortType = PipelineSortType.CREATE_TIME,
            channelCode = ChannelCode.BS,
            viewId = currentViewId,
            checkPermission = true
        )

        return PipelineViewAndPipelines(currentViewId, currentViewList, pipelinePage)
    }

    fun listPipelines(projectId: Set<String>, channelCode: ChannelCode): List<Pipeline> {

        val watcher = Watcher(id = "listPipelines|${projectId.size}")
        val pipelines = mutableListOf<Pipeline>()
        try {
            watcher.start("s_s_r_summary")
            projectId.forEach { tmpProjectId ->
                val buildPipelineRecords = pipelineRuntimeService.getBuildPipelineRecords(tmpProjectId, channelCode)
                if (buildPipelineRecords.isNotEmpty) {
                    pipelines.addAll(
                        buildPipelines(
                            pipelineInfoRecords = buildPipelineRecords,
                            favorPipelines = emptyList(),
                            authPipelines = emptyList(),
                            projectId = tmpProjectId
                        )
                    )
                }
            }
            watcher.stop()
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
        }
        return pipelines
    }

    fun isPipelineRunning(projectId: String, buildId: String, channelCode: ChannelCode): Boolean {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        return buildInfo != null && buildInfo.status == BuildStatus.RUNNING
    }

    fun isRunning(projectId: String, buildId: String, channelCode: ChannelCode): Boolean {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        return buildInfo != null && buildInfo.status.isRunning()
    }

    fun getPipelineStatus(
        userId: String,
        projectId: String,
        pipelines: Set<String>,
        channelCode: ChannelCode? = ChannelCode.BS
    ): List<Pipeline> {
        val watcher = Watcher(id = "getPipelineStatus|$projectId|$userId|${pipelines.size}")
        try {
            watcher.start("s_r_summary")
            val buildPipelineRecords = pipelineRuntimeService.getBuildPipelineRecords(
                projectId = projectId,
                channelCode = channelCode ?: ChannelCode.BS,
                pipelineIds = pipelines
            )
            watcher.start("perm_r_perm")
            val pipelinesPermissions = pipelinePermissionService.getResourceByPermission(
                userId = userId, projectId = projectId, permission = AuthPermission.LIST
            )
            watcher.start("s_r_fav")
            val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
            val pipelineList = buildPipelines(
                pipelineInfoRecords = buildPipelineRecords,
                favorPipelines = favorPipelines,
                authPipelines = pipelinesPermissions,
                projectId = projectId
            )
            sortPipelines(pipelineList, PipelineSortType.UPDATE_TIME)
            watcher.stop()
            return pipelineList
        } finally {
            processJmxApi.execute(ProcessJmxApi.LIST_NEW_PIPELINES_STATUS, watcher.totalTimeMillis)
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }

    // 获取单条流水线的运行状态
    fun getSinglePipelineStatus(
        userId: String,
        projectId: String,
        pipeline: String,
        channelCode: ChannelCode?
    ): Pipeline? {
        val pipelines = setOf(pipeline)
        val pipelineList = getPipelineStatus(userId, projectId, pipelines, channelCode)
        return if (pipelineList.isEmpty()) {
            null
        } else {
            pipelineList[0]
        }
    }

    fun count(projectId: Set<String>, channelCode: ChannelCode?): Int {
        val watcher = Watcher(id = "count|${projectId.size}")
        try {
            watcher.start("s_r_c_b_p")
            return pipelineRepositoryService.countByProjectIds(projectId, channelCode)
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }

    fun getPipelineByIds(pipelineIds: Set<String>, projectId: String? = null): List<SimplePipeline> {
        if (pipelineIds.isEmpty() || projectId.isNullOrBlank()) return listOf()

        val watcher = Watcher(id = "getPipelineByIds|$projectId|${pipelineIds.size}")
        try {
            watcher.start("s_r_list_b_ps")
            val pipelines = pipelineInfoDao.listInfoByPipelineIds(
                dslContext = dslContext,
                projectId = projectId,
                pipelineIds = pipelineIds
            )
            val simplePipelines = mutableListOf<SimplePipeline>()
            if (pipelines.isEmpty()) {
                return simplePipelines
            }
            watcher.start("listTemplate")
            val templatePipelineIds = templatePipelineDao.listByPipelines(
                dslContext = dslContext,
                pipelineIds = pipelineIds,
                projectId = projectId
            ).map { it.value1() } // TODO: 须将是否模板转为PIPELINE基本属性
            watcher.stop()
            val simplePipelineIds = mutableListOf<String>()
            pipelines.forEach {
                val pipelineId = it.pipelineId
                if (simplePipelineIds.contains(pipelineId)) {
                    return@forEach
                }
                simplePipelines.add(
                    SimplePipeline(
                        projectId = it.projectId,
                        pipelineId = it.pipelineId,
                        pipelineName = it.pipelineName,
                        pipelineDesc = it.pipelineDesc,
                        taskCount = it.taskCount,
                        isDelete = it.delete,
                        instanceFromTemplate = templatePipelineIds.contains(it.pipelineId),
                        id = it.id
                    )
                )
            }
            return simplePipelines
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }

    fun getPipelineNameByIds(
        projectId: String,
        pipelineIds: Set<String>,
        filterDelete: Boolean = true
    ): Map<String, String> {

        if (pipelineIds.isEmpty()) return mapOf()
        if (projectId.isBlank()) return mapOf()

        val watcher = Watcher(id = "getPipelineNameByIds|$projectId|${pipelineIds.size}")
        try {
            watcher.start("s_r_list_b_ps")
            return pipelineRepositoryService.listPipelineNameByIds(
                projectId = projectId,
                pipelineIds = pipelineIds,
                filterDelete = filterDelete
            )
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher, warnThreshold = 500)
        }
    }

    fun getBuildNoByByPair(buildIds: Set<String>, projectId: String?): Map<String, String> {
        if (buildIds.isEmpty()) return mapOf()

        val watcher = Watcher(id = "getBuildNoByByPair|${buildIds.size}")
        try {
            watcher.start("s_r_bs")
            return pipelineRuntimeService.getBuildNoByByPair(buildIds, projectId)
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher, warnThreshold = 500)
        }
    }

    fun buildPipelines(
        pipelineInfoRecords: Result<TPipelineInfoRecord>,
        favorPipelines: List<String> = emptyList(),
        authPipelines: List<String> = emptyList(),
        excludePipelineId: String? = null,
        projectId: String,
        queryModelFlag: Boolean? = false
    ): MutableList<Pipeline> {
        // 初始化信息
        val pipelines = mutableListOf<Pipeline>()
        val pipelineIds = mutableSetOf<String>()
        initPipelines(
            pipelineInfoRecords = pipelineInfoRecords,
            excludePipelineId = excludePipelineId,
            pipelineIds = pipelineIds,
            pipelines = pipelines,
            authPipelines = authPipelines,
            favorPipelines = favorPipelines
        )

        // 获取setting信息
        val pipelineSettingMap = pipelineSettingDao.getSimpleSettings(
            dslContext = dslContext,
            pipelineIds = pipelineIds,
            projectId = projectId
        ).map { it.get(TPipelineSetting.T_PIPELINE_SETTING.PIPELINE_ID) to it }.toMap()

        // 获取summary信息
        val lastBuildMap = mutableMapOf<String/*buildId*/, String/*pipelineId*/>()
        val pipelineBuildSummaryMap = pipelineBuildSummaryDao.listSummaryByPipelineIds(
            dslContext = dslContext,
            pipelineIds = pipelineIds,
            projectId = projectId
        ).map {
            if (null != it.latestBuildId) {
                lastBuildMap[it.latestBuildId] = it.pipelineId
            }
            it.pipelineId to it
        }.toMap()

        // 根据LastBuildId获取最新构建的信息
        val pipelineBuildMap = pipelineBuildDao.listBuildInfoByBuildIds(
            dslContext = dslContext,
            projectId = projectId,
            buildIds = lastBuildMap.keys
        ).map { it.pipelineId to it }.toMap()

        // 根据LastBuild获取运行中的构建任务个数
        val buildTaskCountList = pipelineBuildTaskDao.countGroupByBuildId(
            dslContext = dslContext,
            projectId = projectId,
            buildIds = lastBuildMap.keys
        )
        val buildTaskTotalCountMap = buildTaskCountList.groupBy { it.value1() }
            .map { it -> lastBuildMap.getOrDefault(it.key, "") to it.value.sumOf { it.value3() } }
            .toMap()
        val buildTaskFinishCountMap = buildTaskCountList.filter { it.value2() == BuildStatus.SUCCEED.ordinal }
            .groupBy { it.value1() }
            .map { it -> lastBuildMap.getOrDefault(it.key, "") to it.value.sumOf { it.value3() } }
            .toMap()

        // 获取template信息
        val tTemplate = TTemplatePipeline.T_TEMPLATE_PIPELINE
        val pipelineTemplateMap = templatePipelineDao.listSimpleByPipelines(
            dslContext = dslContext,
            pipelineIds = pipelineIds,
            projectId = projectId
        ).map {
            it.get(tTemplate.PIPELINE_ID) to TemplatePipelineInfo(
                templateId = it.get(tTemplate.TEMPLATE_ID),
                version = it.get(tTemplate.VERSION),
                versionName = it.get(tTemplate.VERSION_NAME),
                pipelineId = it.get(tTemplate.PIPELINE_ID)
            )
        }.toMap()

        // 获取label信息
        val pipelineGroupLabel = pipelineGroupService.getPipelinesGroupLabel(pipelineIds, projectId)

        // 获取model信息
        val pipelineModelMap = if (queryModelFlag == true) {
            pipelineRepositoryService.listModel(projectId, pipelineIds)
        } else {
            emptyMap()
        }

        // 获取view信息
        val pipelineViewNameMap = pipelineViewGroupService.getViewNameMap(projectId, pipelineIds)

        // 完善数据
        finalPipelines(
            pipelines = pipelines,
            pipelineModelMap = pipelineModelMap,
            pipelineTemplateMap = pipelineTemplateMap,
            pipelineGroupLabel = pipelineGroupLabel,
            pipelineBuildSummaryMap = pipelineBuildSummaryMap,
            pipelineSettingMap = pipelineSettingMap,
            pipelineViewNameMap = pipelineViewNameMap,
            pipelineBuildMap = pipelineBuildMap,
            buildTaskTotalCountMap = buildTaskTotalCountMap,
            buildTaskFinishCountMap = buildTaskFinishCountMap
        )

        return pipelines
    }

    private fun finalPipelines(
        pipelines: MutableList<Pipeline>,
        pipelineModelMap: Map<String, Model?>,
        pipelineTemplateMap: Map<String, TemplatePipelineInfo>,
        pipelineGroupLabel: Map<String, List<PipelineGroupLabels>>,
        pipelineBuildSummaryMap: Map<String, TPipelineBuildSummaryRecord>,
        pipelineSettingMap: Map<String, Record4<String, String, Int, String>>,
        pipelineViewNameMap: Map<String, MutableList<String>>,
        pipelineBuildMap: Map<String, TPipelineBuildHistoryRecord>,
        buildTaskTotalCountMap: Map<String, Int>,
        buildTaskFinishCountMap: Map<String, Int>
    ) {
        pipelines.forEach {
            val pipelineId = it.pipelineId
            it.model = pipelineModelMap[pipelineId]
            val templateInfo = pipelineTemplateMap[pipelineId]
            it.instanceFromTemplate = templateInfo?.templateId != null
            it.templateId = templateInfo?.templateId
            it.version = templateInfo?.version
            it.versionName = templateInfo?.versionName
            it.groupLabel = pipelineGroupLabel[pipelineId]
            val pipelineBuildSummaryRecord = pipelineBuildSummaryMap[pipelineId]
            if (pipelineBuildSummaryRecord != null) {
                val finishCount = pipelineBuildSummaryRecord.finishCount ?: 0
                val runningCount = pipelineBuildSummaryRecord.runningCount ?: 0
                val buildStatusOrd = pipelineBuildSummaryRecord.latestStatus
                val pipelineBuildStatus = pipelineStatusService.getBuildStatus(buildStatusOrd)
                it.buildCount = (finishCount + runningCount).toLong()
                it.latestBuildStatus = pipelineBuildStatus
                it.runningBuildCount = runningCount
                it.latestBuildStartTime = (pipelineBuildSummaryRecord.latestStartTime)?.timestampmilli() ?: 0
                it.latestBuildEndTime = (pipelineBuildSummaryRecord.latestEndTime)?.timestampmilli() ?: 0
                it.latestBuildNum = pipelineBuildSummaryRecord.buildNum
                it.latestBuildTaskName = pipelineBuildSummaryRecord.latestTaskName
                it.latestBuildId = pipelineBuildSummaryRecord.latestBuildId
                it.latestBuildUserId = pipelineBuildSummaryRecord.latestStartUser ?: ""
                it.latestBuildNumAlias = pipelineBuildSummaryRecord.buildNumAlias
                it.viewNames = pipelineViewNameMap[it.pipelineId]
            }
            pipelineBuildMap[pipelineId]?.let { lastBuild ->
                it.lastBuildMsg = BuildMsgUtils.getBuildMsg(
                    buildMsg = lastBuild.buildMsg,
                    startType = StartType.toStartType(lastBuild.trigger),
                    channelCode = ChannelCode.getChannel(lastBuild.channel)
                )
                it.trigger = lastBuild.trigger
                val webhookInfo = lastBuild.webhookInfo?.let { self ->
                    JsonUtil.to(self, object : TypeReference<WebhookInfo?>() {})
                }
                if (webhookInfo != null) {
                    it.webhookAliasName = webhookInfo.webhookAliasName ?: getProjectName(webhookInfo.webhookRepoUrl)
                    it.webhookRepoUrl = webhookInfo.webhookRepoUrl
                    it.webhookType = it.webhookType
                    val eventType = try {
                        webhookInfo.webhookEventType?.let { e -> CodeEventType.valueOf(e) }
                    } catch (e: Exception) {
                        null
                    }
                    it.webhookMessage = when (eventType) {
                        CodeEventType.PUSH -> webhookInfo.webhookCommitId?.let { e ->
                            val endIndex = e.length.coerceAtMost(7)
                            "Commit [${e.substring(0, endIndex)}] pushed"
                        }

                        CodeEventType.MERGE_REQUEST -> webhookInfo.mrIid?.let { e -> "Merge requests [!$e] open" }
                        CodeEventType.TAG_PUSH -> webhookInfo.tagName?.let { e -> "Tag [$e] pushed" }
                        CodeEventType.ISSUES -> webhookInfo.issueIid?.let { e -> "Issue [$e] opened" }
                        CodeEventType.NOTE -> webhookInfo.noteId?.let { e -> "Note [$e] submitted" }
                        CodeEventType.REVIEW -> webhookInfo.reviewId?.let { e -> "Review [$e] created" }
                        else -> null
                    }
                }
            }
            it.lastBuildFinishCount = buildTaskFinishCountMap.getOrDefault(pipelineId, 0)
            it.lastBuildTotalCount = buildTaskTotalCountMap.getOrDefault(pipelineId, 0)
            val pipelineSettingRecord = pipelineSettingMap[pipelineId]
            if (pipelineSettingRecord != null) {
                val tSetting = TPipelineSetting.T_PIPELINE_SETTING
                it.pipelineDesc = pipelineSettingRecord.get(tSetting.DESC)
                it.lock = PipelineRunLockType.checkLock(pipelineSettingRecord.get(tSetting.RUN_LOCK_TYPE))
                it.buildNumRule = pipelineSettingRecord.get(tSetting.BUILD_NUM_RULE)
            }
        }
    }

    private fun getProjectName(webhookRepoUrl: String?): String {
        if (null == webhookRepoUrl) {
            return ""
        }
        return try {
            GitUtils.getProjectName(webhookRepoUrl)
        } catch (e: Exception) {
            webhookRepoUrl
        }
    }

    private fun initPipelines(
        pipelineInfoRecords: Result<TPipelineInfoRecord>,
        excludePipelineId: String?,
        pipelineIds: MutableSet<String>,
        pipelines: MutableList<Pipeline>,
        authPipelines: List<String>,
        favorPipelines: List<String>
    ) {
        val currentTimestamp = System.currentTimeMillis()
        val latestBuildEstimatedExecutionSeconds = 1L
        for (it in pipelineInfoRecords) {
            val pipelineId = it.pipelineId
            if (excludePipelineId != null && excludePipelineId == pipelineId) {
                continue
            }
            pipelineIds.add(pipelineId)
            pipelines.add(
                Pipeline(
                    projectId = it.projectId,
                    pipelineId = pipelineId,
                    pipelineName = it.pipelineName,
                    taskCount = it.taskCount,
                    canManualStartup = it.manualStartup == 1,
                    latestBuildEstimatedExecutionSeconds = latestBuildEstimatedExecutionSeconds,
                    deploymentTime = (it.updateTime)?.timestampmilli() ?: 0,
                    createTime = (it.createTime)?.timestampmilli() ?: 0,
                    updateTime = (it.updateTime)?.timestampmilli() ?: 0,
                    pipelineVersion = it.version,
                    currentTimestamp = currentTimestamp,
                    hasPermission = authPipelines.contains(pipelineId),
                    hasCollect = favorPipelines.contains(pipelineId),
                    updater = it.lastModifyUser,
                    creator = it.creator,
                    delete = it.delete
                )
            )
        }
    }

    fun listPermissionPipelineName(projectId: String, userId: String): List<Map<String, String>> {
        val pipelines = pipelinePermissionService.getResourceByPermission(
            userId = userId, projectId = projectId, permission = AuthPermission.EXECUTE
        )

        val pipelineIds = pipelines.toSet()
        val newPipelineNameList = pipelineRepositoryService.listPipelineNameByIds(projectId, pipelineIds)
        val list = mutableListOf<Map<String, String>>()
        newPipelineNameList.forEach {
            list.add(mapOf(Pair("pipelineId", it.key), Pair("pipelineName", it.value)))
        }

        return list
    }

    fun getAllBuildNo(projectId: String, pipelineId: String): List<Map<String, String>> {
        val watcher = Watcher(id = "getAllBuildNo|$pipelineId")
        try {
            watcher.start("s_r_all_bn")
            val newBuildNums = pipelineRuntimeService.getAllBuildNum(projectId, pipelineId)
            watcher.stop()
            val result = mutableListOf<Map<String, String>>()
            newBuildNums.forEach {
                result.add(mapOf(Pair("key", it.toString())))
            }
            return result
        } finally {
            LogUtils.printCostTimeWE(watcher, warnThreshold = 999)
        }
    }

    // 获取整条流水线的所有运行状态
    fun getPipelineAllStatus(userId: String, projectId: String, pipeline: String): List<Pipeline>? {
        val pipelines = setOf(pipeline)
        val pipelineList = getPipelineStatus(userId, projectId, pipelines)
        return if (pipelineList.isEmpty()) {
            null
        } else {
            return pipelineList
        }
    }

    fun listDeletePipelineIdByProject(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType,
        channelCode: ChannelCode,
        collation: PipelineCollation
    ): PipelineViewPipelinePage<PipelineInfo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val slqLimit: SQLLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

        // 获取列表和数目
        val list = pipelineRepositoryService.listDeletePipelineIdByProject(
            projectId = projectId,
            days = deletedPipelineStoreDays.toLong(),
            offset = slqLimit.offset,
            limit = slqLimit.limit,
            sortType = sortType,
            collation = collation
        )
        val count = pipelineInfoDao.countDeletePipeline(dslContext, projectId, deletedPipelineStoreDays.toLong())
        // 加上流水线组
        val pipelineViewNameMap =
            pipelineViewGroupService.getViewNameMap(projectId, list.map { it.pipelineId }.toMutableSet())
        list.forEach { it.viewNames = pipelineViewNameMap[it.pipelineId] }
        return PipelineViewPipelinePage(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = count.toLong(),
            records = list
        )
    }

    fun getPipelinePage(projectId: String, limit: Int?, offset: Int?): PipelineViewPipelinePage<PipelineInfo> {
        logger.info("getPipeline |$projectId| $limit| $offset")
        val limitNotNull = limit ?: 10
        val offsetNotNull = offset ?: 0
        val pipelineRecords =
            pipelineInfoDao.listPipelineInfoByProject(
                dslContext = dslContext,
                projectId = projectId,
                limit = limitNotNull,
                offset = offsetNotNull
            )
        val pipelineInfos = mutableListOf<PipelineInfo>()
        pipelineRecords?.map {
            pipelineInfoDao.convert(it, null)?.let { it1 -> pipelineInfos.add(it1) }
        }
        val count = pipelineInfoDao.countByProjectIds(
            dslContext = dslContext,
            projectIds = arrayListOf(projectId),
            channelCode = null
        )
        return PipelineViewPipelinePage(
            page = limitNotNull,
            pageSize = offsetNotNull,
            records = pipelineInfos,
            count = count.toLong()
        )
    }

    fun searchByPipelineName(
        projectId: String,
        pipelineName: String?,
        limit: Int?,
        offset: Int?
    ): PipelineViewPipelinePage<PipelineInfo> {
        logger.info("searchByPipelineName |$projectId|$pipelineName| $limit| $offset")
        val limitNotNull = limit ?: 10
        val offsetNotNull = offset ?: 0
        val pipelineRecords =
            pipelineInfoDao.searchByProject(
                dslContext = dslContext,
                pipelineName = pipelineName,
                projectCode = projectId,
                limit = limitNotNull,
                offset = offsetNotNull
            )
        val pipelineInfos = mutableListOf<PipelineInfo>()
        pipelineRecords?.map {
            pipelineInfoDao.convert(it, null)?.let { it1 -> pipelineInfos.add(it1) }
        }
        val count = pipelineInfoDao.countPipelineInfoByProject(
            dslContext = dslContext,
            pipelineName = pipelineName,
            projectCode = projectId
        )
        return PipelineViewPipelinePage(
            page = limitNotNull,
            pageSize = offsetNotNull,
            records = pipelineInfos,
            count = count.toLong()
        )
    }

    fun searchByProjectIdAndName(
        projectId: String,
        keyword: String?,
        page: Int,
        pageSize: Int,
        channelCodes: List<ChannelCode>
    ): Page<PipelineIdAndName> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val pipelineRecords =
            pipelineInfoDao.searchByProjectId(
                dslContext = dslContext,
                pipelineName = keyword,
                projectCode = projectId,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset,
                channelCodes = channelCodes
            )
        val pipelineInfos = mutableListOf<PipelineIdAndName>()
        pipelineRecords?.map {
            pipelineInfos.add(
                PipelineIdAndName(it.pipelineId, it.pipelineName)
            )
        }
        val count = pipelineInfoDao.countByProjectIds(
            dslContext = dslContext,
            projectIds = listOf(projectId),
            channelCodes = channelCodes,
            keyword = keyword
        )
        return Page(
            page = page,
            pageSize = pageSize,
            count = count.toLong(),
            records = pipelineInfos
        )
    }

    fun searchByPipeline(
        projectId: String,
        pipelineId: String
    ): PipelineIdAndName? {
        val pipelineRecords = pipelineInfoDao.getPipelineId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: return null
        return PipelineIdAndName(
            pipelineId = pipelineRecords.pipelineId,
            pipelineName = pipelineRecords.pipelineName,
            channelCode = ChannelCode.getChannel(pipelineRecords.channel)
        )
    }

    fun searchIdAndName(
        projectId: String,
        pipelineName: String?,
        page: Int?,
        pageSize: Int?
    ): List<PipelineIdAndName> {
        logger.info("searchIdAndName |$projectId|$pipelineName| $page| $pageSize")
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 10
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val pipelineRecords =
            pipelineInfoDao.searchByProject(
                dslContext = dslContext,
                pipelineName = pipelineName,
                projectCode = projectId,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )
        val pipelineInfos = mutableListOf<PipelineIdAndName>()
        pipelineRecords?.map {
            pipelineInfos.add(PipelineIdAndName(it.pipelineId, it.pipelineName))
        }

        return pipelineInfos
    }

    fun getPipelineDetail(
        userId: String,
        projectId: String,
        pipelineId: String
    ): PipelineDetailInfo? {
        val permission = AuthPermission.VIEW
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission
            )
        ) {
            throw PermissionForbiddenException(
                MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
            )
        }
        val pipelineInfo = pipelineInfoDao.getPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: return null
        if (pipelineInfo.projectId != projectId) {
            throw ParamBlankException(
                MessageUtil.getMessageByLocale(
                    ERROR_PIPELINE_ID_NOT_PROJECT_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(pipelineId, projectId)
                )
            )
        }
        val hasEditPermission = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT
        )
        val templateId = templatePipelineDao.get(dslContext, projectId, pipelineId)?.templateId
        val instanceFromTemplate = templateId != null
        val favorInfos = pipelineFavorDao.listByPipelineId(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val hasCollect = if (favorInfos != null) {
            favorInfos.size > 0
        } else false
        return PipelineDetailInfo(
            pipelineId = pipelineInfo.pipelineId,
            pipelineName = pipelineInfo.pipelineName,
            instanceFromTemplate = instanceFromTemplate,
            hasCollect = hasCollect,
            canManualStartup = pipelineInfo.manualStartup,
            pipelineVersion = pipelineInfo.version.toString(),
            deploymentTime = DateTimeUtil.toDateTime(pipelineInfo.updateTime),
            hasPermission = hasEditPermission,
            templateId = templateId
        )
    }

    fun getPipelineIdByNames(
        projectId: String,
        pipelineNames: Set<String>,
        filterDelete: Boolean
    ): Map<String, String> {

        if (pipelineNames.isEmpty() || projectId.isBlank()) return mapOf()

        return pipelineRepositoryService.listPipelineIdByName(projectId, pipelineNames, filterDelete)
    }

    fun getProjectPipelineId(
        projectCode: String
    ): List<PipelineIdInfo> {
        val pipelineIdInfos = pipelineInfoDao.listByProject(dslContext, projectCode)
        val idInfos = mutableListOf<PipelineIdInfo>()
        pipelineIdInfos.forEach {
            idInfos.add(PipelineIdInfo(it.get("pipelineId") as String, it.get("id") as Long))
        }
        return idInfos
    }

    fun getPipelineId(
        projectId: String,
        pipelineId: String
    ): PipelineIdInfo? {
        val pipelineInfo = pipelineInfoDao.getPipelineId(dslContext, projectId, pipelineId) ?: return null
        return PipelineIdInfo(
            id = pipelineInfo.id,
            pipelineId = pipelineId
        )
    }

    fun getByPipelineIds(
        pipelineIds: Set<String>
    ): List<SimplePipeline> {
        val pipelineInfos = pipelineInfoDao.listInfoByPipelineIds(
            dslContext = dslContext,
            pipelineIds = pipelineIds,
            projectId = null
        )
        return generateSimplePipelines(pipelineInfos)
    }

    private fun generateSimplePipelines(pipelineInfos: Result<TPipelineInfoRecord>): MutableList<SimplePipeline> {
        val simplePipelines = mutableListOf<SimplePipeline>()
        if (pipelineInfos.isEmpty()) {
            return simplePipelines
        }
        val simplePipelineIds = mutableListOf<String>()
        pipelineInfos.forEach {
            val pipelineId = it.pipelineId
            if (simplePipelineIds.contains(pipelineId)) {
                return@forEach
            }
            simplePipelines.add(
                SimplePipeline(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    pipelineName = it.pipelineName,
                    pipelineDesc = it.pipelineDesc,
                    taskCount = it.taskCount,
                    isDelete = it.delete,
                    instanceFromTemplate = false,
                    id = it.id,
                    createUser = it.creator
                )
            )
        }
        return simplePipelines
    }

    fun getByAutoIds(
        ids: List<Long>,
        projectId: String? = null
    ): List<SimplePipeline> {
        val pipelines = pipelineInfoDao.getPipelineByAutoId(
            dslContext = dslContext,
            ids = ids,
            projectId = projectId
        )
        return generateSimplePipelines(pipelines)
    }

    fun listQualityViewPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType,
        channelCode: ChannelCode,
        viewId: String,
        checkPermission: Boolean = true,
        filterByPipelineName: String? = null,
        filterByCreator: String? = null,
        filterByLabels: String? = null,
        callByApp: Boolean? = false,
        authPipelineIds: List<String> = emptyList(),
        skipPipelineIds: List<String> = emptyList()
    ): PipelineViewPipelinePage<QualityPipeline> {

        val watch = StopWatch()
        watch.start("perm_r_perm")
        val authPipelines = authPipelineIds.ifEmpty {
            pipelinePermissionService.getResourceByPermission(
                userId = userId, projectId = projectId, permission = AuthPermission.LIST
            )
        }
        watch.stop()

        watch.start("s_r_summary")
        val buildPipelineRecords = pipelineRuntimeService.getBuildPipelineRecords(projectId, channelCode)
        watch.stop()

        watch.start("s_r_fav")
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        var slqLimit: SQLLimit? = null
        if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

        val offset = slqLimit?.offset ?: 0
        val limit = slqLimit?.limit ?: -1
        var count = 0L
        try {

            val list = if (buildPipelineRecords.isNotEmpty) {

                val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
                val pipelines = buildPipelines(
                    pipelineInfoRecords = buildPipelineRecords,
                    favorPipelines = favorPipelines,
                    authPipelines = authPipelines,
                    projectId = projectId
                )
                val allFilterPipelines = filterViewPipelines(
                    projectId = projectId,
                    pipelines = pipelines,
                    filterByName = filterByPipelineName,
                    filterByCreator = filterByCreator,
                    filterByLabels = filterByLabels
                )

                val hasPipelines = allFilterPipelines.isNotEmpty()

                if (!hasPipelines) {
                    return PipelineViewPipelinePage(pageNotNull, pageSizeNotNull, 0, emptyList())
                }

                val filterPipelines = when (viewId) {
                    PIPELINE_VIEW_FAVORITE_PIPELINES -> {
                        logger.info("User($userId) favorite pipeline ids($favorPipelines)")
                        allFilterPipelines.filter { favorPipelines.contains(it.pipelineId) }
                    }

                    PIPELINE_VIEW_MY_PIPELINES -> {
                        logger.info("User($userId) my pipelines")
                        allFilterPipelines.filter {
                            authPipelines.contains(it.pipelineId)
                        }
                    }

                    PIPELINE_VIEW_ALL_PIPELINES -> {
                        logger.info("User($userId) all pipelines")
                        allFilterPipelines
                    }

                    else -> {
                        logger.info("User($userId) filter view($viewId)")
                        filterViewPipelines(userId, projectId, allFilterPipelines, viewId)
                    }
                }

                val permissionList = filterPipelines.filter { it.hasPermission }.toMutableList()
                sortPipelines(permissionList, sortType)
                count = permissionList.size.toLong()

                val toIndex =
                    if (limit == -1 || permissionList.size <= (offset + limit)) permissionList.size else offset + limit

                if (offset >= permissionList.size) mutableListOf() else permissionList.subList(offset, toIndex)
            } else {
                mutableListOf()
            }
            watch.stop()

            val records = list.map {
                QualityPipeline(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    pipelineName = it.pipelineName,
                    pipelineDesc = it.pipelineDesc,
                    taskCount = it.taskCount,
                    buildCount = it.buildCount,
                    latestBuildStartTime = it.latestBuildStartTime,
                    latestBuildEndTime = it.latestBuildEndTime
                )
            }
            return PipelineViewPipelinePage(pageNotNull, pageSizeNotNull, count, records)
        } finally {
            logger.info("listViewPipelines|[$projectId]|$userId|watch=$watch")
            processJmxApi.execute(ProcessJmxApi.LIST_NEW_PIPELINES, watch.totalTimeMillis)
        }
    }

    fun getProjectPipelineLabelInfos(
        projectIds: List<String>
    ): List<PipelineLabelRelateInfo> {
        return pipelineLabelPipelineDao.getPipelineLabelRelateInfos(dslContext, projectIds)
    }
}
