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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineFavorDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
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
import com.tencent.devops.process.pojo.PipelineIdAndName
import com.tencent.devops.process.pojo.PipelineIdInfo
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.PipelineDetailInfo
import com.tencent.devops.process.pojo.PipelineWithModel
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.classify.PipelineViewAndPipelines
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByCreator
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByLabel
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByName
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.classify.enums.Condition
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.view.PipelineViewService
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_FAVORITE_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import com.tencent.devops.quality.api.v2.pojo.response.QualityPipeline
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import javax.ws.rs.core.Response

@Suppress("ALL")
@Service
class PipelineListFacadeService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewService: PipelineViewService,
    private val processJmxApi: ProcessJmxApi,
    private val dslContext: DSLContext,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineFavorDao: PipelineFavorDao
) {

    @Value("\${process.deletedPipelineStoreDays:30}")
    private val deletedPipelineStoreDays: Int = 30

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineListFacadeService::class.java)
    }

    fun sortPipelines(pipelines: MutableList<Pipeline>, sortType: PipelineSortType) {
        pipelines.sortWith(Comparator { a, b ->
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
        })
    }

    fun getBatchPipelinesWithModel(
        userId: String,
        projectId: String,
        pipelineIds: List<String>,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): List<PipelineWithModel> {
        if (checkPermission) {
            val permission = AuthPermission.VIEW
            val hasViewPermission = pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                permission = permission
            )
            if (!hasViewPermission) {
                val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${permission.value}",
                    defaultMessage = permission.alias
                )
                throw ErrorCodeException(
                    statusCode = Response.Status.FORBIDDEN.statusCode,
                    errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                    defaultMessage = "用户($userId)无权限在工程($projectId)下获取流水线",
                    params = arrayOf(permissionMsg)
                )
            }
        }

        val pipelines = mutableListOf<PipelineWithModel>()
        val pipelineGroupLabel = pipelineGroupService.getPipelinesGroupLabel(pipelineIds.toList())
        val pipelineBuildSummaries = pipelineBuildSummaryDao.listPipelineInfoBuildSummary(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = channelCode,
            sortType = null,
            pipelineIds = pipelineIds
        )

        for (it in pipelineBuildSummaries) {
            val pipelineId = it["PIPELINE_ID"] as String
            val version = it["VERSION"] as Int
            val finishCount = it["FINISH_COUNT"] as Int? ?: 0
            val runningCount = it["RUNNING_COUNT"] as Int? ?: 0
            val buildStatusOrd = it["LATEST_STATUS"] as Int?
            val model = pipelineRepositoryService.getModel(pipelineId, version) ?: continue
            val pipelineBuildStatus = if (buildStatusOrd != null) {
                val tmpStatus = BuildStatus.values()[buildStatusOrd.coerceAtMost(BuildStatus.values().size - 1)]
                if (tmpStatus.isFinish()) {
                    BuildStatusSwitcher.pipelineStatusMaker.finish(tmpStatus)
                } else {
                    tmpStatus
                }
            } else {
                null
            }
            // todo还没想好与Pipeline 结合，减少这部分的代码，收归一处
            pipelines.add(
                PipelineWithModel(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = it["PIPELINE_NAME"] as String,
                    pipelineDesc = it["DESC"] as String?,
                    taskCount = it["TASK_COUNT"] as Int,
                    buildCount = (finishCount + runningCount).toLong(),
                    lock = PipelineRunLockType.checkLock(it["RUN_LOCK_TYPE"] as Int?),
                    canManualStartup = it["MANUAL_STARTUP"] as Int == 1,
                    latestBuildStartTime = (it["LATEST_START_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                    latestBuildEndTime = (it["LATEST_END_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                    latestBuildStatus = pipelineBuildStatus,
                    latestBuildNum = it["BUILD_NUM"] as Int,
                    latestBuildTaskName = it["LATEST_TASK_NAME"] as String?,
                    latestBuildId = it["LATEST_BUILD_ID"] as String?,
                    updateTime = (it["UPDATE_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                    createTime = (it["CREATE_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                    pipelineVersion = version,
                    runningBuildCount = runningCount,
                    latestBuildUserId = it["LATEST_START_USER"] as String? ?: "",
                    creator = it["CREATOR"] as String,
                    groupLabel = pipelineGroupLabel[pipelineId],
                    model = model
                )
            )
        }
        return pipelines
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

        val pipelines = listPermissionPipeline(
            userId = userId,
            projectId = projectId,
            page = null,
            pageSize = null,
            sortType = PipelineSortType.CREATE_TIME,
            channelCode = ChannelCode.BS,
            checkPermission = false
        )

        if (pipelineIdList != null) {
            resultPipelineIds.addAll(pipelineIdList)
        }

        if (templateIdList != null) {
            val templatePipelineIds =
                templatePipelineDao.listPipeline(dslContext, PipelineInstanceTypeEnum.CONSTRAINT.type, templateIdList)
                    .map { it.pipelineId }
            resultPipelineIds.addAll(templatePipelineIds)
        }

        return if (resultPipelineIds.isEmpty()) {
            pipelines.records
        } else {
            pipelines.records.filter { it.pipelineId in resultPipelineIds }
        }
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

            val hasCreatePermission =
                if (!checkPermission) {
                    true
                } else {
                    watcher.start("checkPerm")
                    val validateUserResourcePermission = pipelinePermissionService.checkPipelinePermission(
                        userId = userId, projectId = projectId, permission = AuthPermission.CREATE
                    )
                    watcher.stop()
                    validateUserResourcePermission
                }

            val pageNotNull = page ?: 0
            val pageSizeNotNull = pageSize ?: -1
            var slqLimit: SQLLimit? = null
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
            val pipelineBuildSummary =
                pipelineRuntimeService.getBuildSummaryRecords(
                    projectId = projectId, channelCode = channelCode, pipelineIds = hasPermissionList
                )
            watcher.stop()

            if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

            val offset = slqLimit?.offset ?: 0
            val limit = slqLimit?.limit ?: -1
            var hasFavorPipelines = false
            val hasPermissionPipelines = hasPermissionList.isNotEmpty()

            val allPipelines = mutableListOf<Pipeline>()

            watcher.start("s_r_fav")
            if (pipelineBuildSummary.isNotEmpty) {
                val favorPipelines = pipelineGroupService.getFavorPipelines(userId = userId, projectId = projectId)
                hasFavorPipelines = favorPipelines.isNotEmpty()

                val pipelines = buildPipelines(
                    pipelineBuildSummary = pipelineBuildSummary,
                    favorPipelines = favorPipelines,
                    authPipelines = hasPermissionList
                )
                allPipelines.addAll(pipelines)

                sortPipelines(allPipelines, sortType)
            }

            val toIndex =
                if (limit == -1 || allPipelines.size <= (offset + limit)) allPipelines.size else offset + limit
            val pagePipelines =
                if (offset >= allPipelines.size) listOf<Pipeline>() else allPipelines.subList(offset, toIndex)

            watcher.stop()

            return PipelinePage(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = allPipelines.size.toLong(),
                records = pagePipelines,
                hasCreatePermission = hasCreatePermission,
                hasPipelines = true,
                hasFavorPipelines = hasFavorPipelines,
                hasPermissionPipelines = hasPermissionPipelines,
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
            val pipelineBuildSummary =
                pipelineRuntimeService.getBuildSummaryRecords(
                    projectId = projectId,
                    channelCode = ChannelCode.BS,
                    pipelineIds = hasPermissionList,
                    page = page,
                    pageSize = pageSize
                )
            watcher.stop()

            watcher.start("s_r_fav")
            val count = pipelineBuildSummary.size + 0L
            val pagePipelines =
                if (count > 0) {
                    val favorPipelines = pipelineGroupService.getFavorPipelines(userId = userId, projectId = projectId)
                    buildPipelines(
                        pipelineBuildSummary = pipelineBuildSummary,
                        favorPipelines = favorPipelines,
                        authPipelines = emptyList()
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
        authPipelineIds: List<String> = emptyList(),
        skipPipelineIds: List<String> = emptyList()
    ): PipelineViewPipelinePage<Pipeline> {
        val watcher = Watcher(id = "listViewPipelines|$projectId|$userId")
        watcher.start("perm_r_perm")
        val authPipelines = if (authPipelineIds.isEmpty()) {
            pipelinePermissionService.getResourceByPermission(
                userId = userId, projectId = projectId, permission = AuthPermission.LIST
            )
        } else {
            authPipelineIds
        }
        watcher.stop()

        watcher.start("s_r_summary")
        try {
            val favorPipelines = pipelineGroupService.getFavorPipelines(userId = userId, projectId = projectId)
            val (
                filterByPipelineNames: List<PipelineViewFilterByName>,
                filterByPipelineCreators: List<PipelineViewFilterByCreator>,
                filterByPipelineLabels: List<PipelineViewFilterByLabel>
            ) = generatePipelineFilterInfo(
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
                    labelToPipelineMap = filterByPipelineLabels.generateLabelToPipelineMap()
                )
            )
            pipelineFilterParamList.add(pipelineFilterParam)
            val viewIdList =
                listOf(PIPELINE_VIEW_FAVORITE_PIPELINES, PIPELINE_VIEW_MY_PIPELINES, PIPELINE_VIEW_ALL_PIPELINES)
            if (!viewIdList.contains(viewId)) {
                val view = pipelineViewService.getView(userId = userId, projectId = projectId, viewId = viewId)
                val filters = pipelineViewService.getFilters(view)
                val pipelineViewFilterParam = PipelineFilterParam(
                    logic = view.logic,
                    filterByPipelineNames = filters.first,
                    filterByPipelineCreators = filters.second,
                    filterByLabelInfo = PipelineFilterByLabelInfo(
                        filterByLabels = filters.third,
                        labelToPipelineMap = filters.third.generateLabelToPipelineMap()
                    )
                )
                pipelineFilterParamList.add(pipelineViewFilterParam)
            }
            pipelineViewService.addUsingView(userId = userId, projectId = projectId, viewId = viewId)

            // 查询有权限查看的流水线总数
            val totalAvailablePipelineSize = pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
                dslContext = dslContext,
                projectId = projectId,
                channelCode = channelCode,
                pipelineIds = authPipelines,
                favorPipelines = favorPipelines,
                authPipelines = authPipelines,
                viewId = viewId,
                pipelineFilterParamList = pipelineFilterParamList,
                permissionFlag = true
            )

            // 查询无权限查看的流水线总数
            val totalInvalidPipelineSize =
                if (filterInvalid) 0 else pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
                    dslContext = dslContext,
                    projectId = projectId,
                    channelCode = channelCode,
                    favorPipelines = favorPipelines,
                    authPipelines = authPipelines,
                    viewId = viewId,
                    pipelineFilterParamList = pipelineFilterParamList,
                    permissionFlag = false
                )
            val pipelineList = mutableListOf<Pipeline>()
            val totalSize = totalAvailablePipelineSize + totalInvalidPipelineSize
            if ((null != page && null != pageSize) && !(page == 1 && pageSize == -1)) {
                // 判断可用的流水线是否已到最后一页
                val totalAvailablePipelinePage = PageUtil.calTotalPage(pageSize, totalAvailablePipelineSize)
                if (page < totalAvailablePipelinePage) {
                    // 当前页未到可用流水线最后一页，不需要处理临界点（最后一页）的情况
                    handlePipelineQueryList(
                        pipelineList = pipelineList,
                        projectId = projectId,
                        channelCode = channelCode,
                        sortType = sortType,
                        favorPipelines = favorPipelines,
                        authPipelines = authPipelines,
                        viewId = viewId,
                        pipelineFilterParamList = pipelineFilterParamList,
                        permissionFlag = true,
                        page = page,
                        pageSize = pageSize
                    )
                } else if (page == totalAvailablePipelinePage && totalAvailablePipelineSize > 0) {
                    //  查询可用流水线最后一页不满页的数量
                    val lastPageRemainNum = pageSize - totalAvailablePipelineSize % pageSize
                    handlePipelineQueryList(
                        pipelineList = pipelineList,
                        projectId = projectId,
                        channelCode = channelCode,
                        sortType = sortType,
                        favorPipelines = favorPipelines,
                        authPipelines = authPipelines,
                        viewId = viewId,
                        pipelineFilterParamList = pipelineFilterParamList,
                        permissionFlag = true,
                        page = page,
                        pageSize = pageSize
                    )
                    // 可用流水线最后一页不满页的数量需用不可用的流水线填充
                    if (lastPageRemainNum > 0 && totalInvalidPipelineSize > 0) {
                        handlePipelineQueryList(
                            pipelineList = pipelineList,
                            projectId = projectId,
                            channelCode = channelCode,
                            sortType = sortType,
                            favorPipelines = favorPipelines,
                            authPipelines = authPipelines,
                            viewId = viewId,
                            pipelineFilterParamList = pipelineFilterParamList,
                            permissionFlag = false,
                            page = 1,
                            pageSize = lastPageRemainNum.toInt()
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
                        favorPipelines = favorPipelines,
                        authPipelines = authPipelines,
                        viewId = viewId,
                        pipelineFilterParamList = pipelineFilterParamList,
                        permissionFlag = false,
                        page = page - totalAvailablePipelinePage,
                        pageSize = pageSize,
                        offsetNum = lastPageRemainNum.toInt()
                    )
                }
            } else {
                // 不分页查询
                handlePipelineQueryList(
                    pipelineList = pipelineList,
                    projectId = projectId,
                    channelCode = channelCode,
                    sortType = sortType,
                    favorPipelines = favorPipelines,
                    authPipelines = authPipelines,
                    viewId = viewId,
                    pipelineFilterParamList = pipelineFilterParamList,
                    permissionFlag = true,
                    page = page,
                    pageSize = pageSize
                )

                if (filterInvalid) {
                    handlePipelineQueryList(
                        pipelineList = pipelineList,
                        projectId = projectId,
                        channelCode = channelCode,
                        sortType = sortType,
                        favorPipelines = favorPipelines,
                        authPipelines = authPipelines,
                        viewId = viewId,
                        pipelineFilterParamList = pipelineFilterParamList,
                        permissionFlag = false,
                        page = page,
                        pageSize = pageSize
                    )
                }
            }
            watcher.stop()

            return PipelineViewPipelinePage(
                page = page ?: 1,
                pageSize = pageSize ?: totalSize.toInt(),
                count = totalSize,
                records = pipelineList
            )
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
            processJmxApi.execute(ProcessJmxApi.LIST_NEW_PIPELINES, watcher.totalTimeMillis)
        }
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
        offsetNum: Int? = 0
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
            offsetNum = offsetNum
        )
        pipelineList.addAll(buildPipelines(pipelineRecords, favorPipelines, authPipelines))
    }

    private fun List<PipelineViewFilterByLabel>.generateLabelToPipelineMap(): Map<String, List<String>>? {
        var labelToPipelineMap: Map<String, List<String>>? = null
        if (isNotEmpty()) {
            val labelIds = mutableListOf<String>()
            forEach {
                labelIds.addAll(it.labelIds)
            }
            labelToPipelineMap = pipelineGroupService.getViewLabelToPipelinesMap(labelIds)
        }
        return labelToPipelineMap
    }

    fun filterViewPipelines(
        userId: String,
        projectId: String,
        pipelines: List<Pipeline>,
        viewId: String
    ): List<Pipeline> {
        val view = pipelineViewService.getView(userId = userId, projectId = projectId, viewId = viewId)
        val filters = pipelineViewService.getFilters(view)

        return filterViewPipelines(pipelines, view.logic, filters.first, filters.second, filters.third)
    }

    /**
     * 视图的基础上增加简单过滤
     */
    fun filterViewPipelines(
        pipelines: List<Pipeline>,
        filterByName: String?,
        filterByCreator: String?,
        filterByLabels: String?
    ): List<Pipeline> {
        logger.info("filter view pipelines $filterByName $filterByCreator $filterByLabels")

        val (filterByPipelineNames, filterByPipelineCreators, filterByPipelineLabels) = generatePipelineFilterInfo(
            filterByName,
            filterByCreator,
            filterByLabels
        )

        if (filterByPipelineNames.isEmpty() && filterByPipelineCreators.isEmpty() && filterByPipelineLabels.isEmpty()) {
            return pipelines
        }

        return filterViewPipelines(
            pipelines,
            Logic.AND,
            filterByPipelineNames,
            filterByPipelineCreators,
            filterByPipelineLabels
        )
    }

    private fun generatePipelineFilterInfo(
        filterByName: String?,
        filterByCreator: String?,
        filterByLabels: String?
    ): Triple<List<PipelineViewFilterByName>, List<PipelineViewFilterByCreator>, List<PipelineViewFilterByLabel>> {
        val filterByPipelineNames = if (filterByName.isNullOrEmpty()) {
            emptyList()
        } else {
            listOf(PipelineViewFilterByName(Condition.LIKE, filterByName!!))
        }

        val filterByPipelineCreators = if (filterByCreator.isNullOrEmpty()) {
            emptyList()
        } else {
            listOf(PipelineViewFilterByCreator(Condition.INCLUDE, filterByCreator!!.split(",")))
        }

        val filterByPipelineLabels = if (filterByLabels.isNullOrEmpty()) {
            emptyList()
        } else {
            val labelIds = filterByLabels!!.split(",")
            val labelGroupToLabelMap = pipelineGroupService.getGroupToLabelsMap(labelIds)

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
            val labelToPipelineMap = pipelineGroupService.getViewLabelToPipelinesMap(labelIds)

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
                .intersect(creatorFilterPipelines.asIterable())
                .intersect(labelFilterPipelines.asIterable())
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
            projectId.forEach { project_id ->
                val pipelineBuildSummary = pipelineRuntimeService.getBuildSummaryRecords(
                    projectId = project_id,
                    channelCode = channelCode,
                    page = 1,
                    pageSize = 500
                )
                if (pipelineBuildSummary.isNotEmpty) {
                    pipelines.addAll(buildPipelines(pipelineBuildSummary, emptyList(), emptyList()))
                }
            }
            watcher.stop()
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
        }
        return pipelines
    }

    fun getPipelineInfoNum(
        dslContext: DSLContext,
        projectIds: Set<String>?,
        channelCodes: Set<ChannelCode>?
    ): Int? {
        return pipelineInfoDao.getPipelineInfoNum(dslContext, projectIds, channelCodes)!!.value1()
    }

    fun listPagedPipelines(
        dslContext: DSLContext,
        projectIds: Set<String>,
        channelCodes: Set<ChannelCode>?,
        limit: Int?,
        offset: Int?
    ): MutableList<Pipeline> {
        val watcher = Watcher(id = "listPagedPipelines|${projectIds.size}")
        val pipelines = mutableListOf<Pipeline>()
        try {
            watcher.start("s_s_r_summary")
            val pipelineBuildSummary =
                pipelineRuntimeService.getBuildSummaryRecords(dslContext, projectIds, channelCodes, limit, offset)
            if (pipelineBuildSummary.isNotEmpty) {
                pipelines.addAll(buildPipelines(pipelineBuildSummary, emptyList(), emptyList()))
            }
            watcher.stop()
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
        }
        return pipelines
    }

    fun listPipelinesByIds(channelCodes: Set<ChannelCode>?, pipelineIds: Set<String>): List<Pipeline> {
        val watcher = Watcher(id = "listPipelinesByIds|${pipelineIds.size}")
        val pipelines = mutableListOf<Pipeline>()
        try {
            watcher.start("s_s_r_summary")
            val pipelineBuildSummary = pipelineRuntimeService.getBuildSummaryRecords(channelCodes, pipelineIds)
            if (pipelineBuildSummary.isNotEmpty) {
                pipelines.addAll(buildPipelines(pipelineBuildSummary, emptyList(), emptyList()))
            }
            watcher.stop()
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
        }
        return pipelines
    }

    fun isPipelineRunning(projectId: String, buildId: String, channelCode: ChannelCode): Boolean {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
        return buildInfo != null && buildInfo.status == BuildStatus.RUNNING
    }

    fun isRunning(projectId: String, buildId: String, channelCode: ChannelCode): Boolean {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
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
            val pipelineBuildSummary = pipelineRuntimeService.getBuildSummaryRecords(
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
            val pipelineList = buildPipelines(pipelineBuildSummary, favorPipelines, pipelinesPermissions)
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
            watcher.start("listTemplate")
            val templatePipelineIds = templatePipelineDao.listByPipelines(
                dslContext,
                pipelineIds
            ).map { it.pipelineId } // TODO: 须将是否模板转为PIPELINE基本属性
            watcher.stop()
            return pipelines.map {
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
            }
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

    fun getBuildNoByByPair(buildIds: Set<String>): Map<String, String> {
        if (buildIds.isEmpty()) return mapOf()

        val watcher = Watcher(id = "getBuildNoByByPair|${buildIds.size}")
        try {
            watcher.start("s_r_bs")
            return pipelineRuntimeService.getBuildNoByByPair(buildIds)
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher, warnThreshold = 500)
        }
    }

    fun buildPipelines(
        pipelineBuildSummary: Result<out Record>,
        favorPipelines: List<String>,
        authPipelines: List<String>,
        excludePipelineId: String? = null
    ): MutableList<Pipeline> {

        val pipelines = mutableListOf<Pipeline>()
        val currentTimestamp = System.currentTimeMillis()
        val latestBuildEstimatedExecutionSeconds = 1L
        val pipelineIds = mutableSetOf<String>()
        for (it in pipelineBuildSummary) {
            val pipelineId = it["PIPELINE_ID"] as String
            if (excludePipelineId != null && excludePipelineId == pipelineId) {
                continue
            }
            pipelineIds.add(pipelineId)
            val finishCount = it["FINISH_COUNT"] as Int? ?: 0
            val runningCount = it["RUNNING_COUNT"] as Int? ?: 0
            val buildStatusOrd = it["LATEST_STATUS"] as Int?
            val pipelineBuildStatus = if (buildStatusOrd != null) {
                val tmpStatus = BuildStatus.values()[buildStatusOrd.coerceAtMost(BuildStatus.values().size - 1)]
                if (tmpStatus.isFinish()) {
                    BuildStatusSwitcher.pipelineStatusMaker.finish(tmpStatus)
                } else {
                    tmpStatus
                }
            } else {
                null
            }
            // todo还没想好与PipelineWithModel结合，减少这部分的代码，收归一处
            pipelines.add(
                Pipeline(
                    projectId = it["PROJECT_ID"] as String,
                    pipelineId = pipelineId,
                    pipelineName = it["PIPELINE_NAME"] as String,
                    pipelineDesc = it["DESC"] as String?,
                    taskCount = it["TASK_COUNT"] as Int,
                    buildCount = (finishCount + runningCount).toLong(),
                    lock = PipelineRunLockType.checkLock(it["RUN_LOCK_TYPE"] as Int?),
                    canManualStartup = it["MANUAL_STARTUP"] as Int == 1,
                    latestBuildStartTime = (it["LATEST_START_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                    latestBuildEndTime = (it["LATEST_END_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                    latestBuildStatus = pipelineBuildStatus,
                    latestBuildNum = it["BUILD_NUM"] as Int,
                    latestBuildTaskName = it["LATEST_TASK_NAME"] as String?,
                    latestBuildEstimatedExecutionSeconds = latestBuildEstimatedExecutionSeconds,
                    latestBuildId = it["LATEST_BUILD_ID"] as String?,
                    deploymentTime = (it["UPDATE_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                    createTime = (it["CREATE_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0,
                    pipelineVersion = it["VERSION"] as Int,
                    currentTimestamp = currentTimestamp,
                    runningBuildCount = runningCount,
                    hasPermission = authPipelines.contains(pipelineId),
                    hasCollect = favorPipelines.contains(pipelineId),
                    latestBuildUserId = it["LATEST_START_USER"] as String? ?: "",
                    creator = it["CREATOR"] as String,
                    latestBuildNumAlias = it["BUILD_NUM_ALIAS"] as String?,
                    buildNumRule = it["BUILD_NUM_RULE"] as String?
                )
            )
        }
        val pipelineRecords = templatePipelineDao.listByPipelinesId(dslContext, pipelineIds)
        val pipelineTemplateMap = mutableMapOf<String, String>()
        pipelineRecords.forEach {
            pipelineTemplateMap[it["pipelineId"] as String] = it["templateId"] as String
        }
        val pipelineGroupLabel = pipelineGroupService.getPipelinesGroupLabel(pipelineIds.toList())
        pipelines.forEach {
            val templateId = pipelineTemplateMap[it.pipelineId]
            it.instanceFromTemplate = templateId != null
            it.templateId = templateId
            it.groupLabel = pipelineGroupLabel[it.pipelineId]
        }

        return pipelines
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

    // 旧接口
    fun getPipelineIdAndProjectIdByBuildId(buildId: String): Pair<String, String> {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                defaultMessage = "构建任务${buildId}不存在",
                params = arrayOf(buildId)
            )
        return Pair(buildInfo.pipelineId, buildInfo.projectId)
    }

    fun listDeletePipelineIdByProject(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        channelCode: ChannelCode
    ): PipelineViewPipelinePage<PipelineInfo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        var slqLimit: SQLLimit? = null
        if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

        val offset = slqLimit?.offset ?: 0
        val limit = slqLimit?.limit ?: -1
        // 数据量不多，直接全拉
        val pipelines =
            pipelineRepositoryService.listDeletePipelineIdByProject(projectId, deletedPipelineStoreDays.toLong())
        val list: List<PipelineInfo> = when {
            offset >= pipelines.size -> emptyList()
            limit < 0 -> pipelines.subList(offset, pipelines.size)
            else -> {
                val toIndex = if (pipelines.size <= (offset + limit)) pipelines.size else offset + limit
                pipelines.subList(offset, toIndex)
            }
        }
        return PipelineViewPipelinePage(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = pipelines.size + 0L,
            records = list
        )
    }

    fun listPermissionPipelineCount(
        userId: String,
        projectId: String,
        channelCode: ChannelCode = ChannelCode.BS,
        checkPermission: Boolean = true
    ): Int {
        val watcher = Watcher(id = "listPermissionPipelineCount|$projectId|$userId")
        try {
            watcher.start("perm_r_perm")
            val hasPermissionList = pipelinePermissionService.getResourceByPermission(
                userId = userId, projectId = projectId, permission = AuthPermission.LIST
            )
            watcher.start("s_r_c_b_id")
            return pipelineRepositoryService.countByPipelineIds(
                projectId = projectId, channelCode = channelCode, pipelineIds = hasPermissionList
            )
        } finally {
            LogUtils.printCostTimeWE(watcher = watcher)
        }
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
            pipelineInfoDao.searchByPipelineName(
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

    fun searchIdAndName(
        projectId: String,
        pipelineName: String?,
        page: Int?,
        pageSize: Int?
    ): List<PipelineIdAndName> {
        logger.info("searchIdAndName |$projectId|$pipelineName| $page| $pageSize")
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 10
        val page = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val pipelineRecords =
            pipelineInfoDao.searchByPipelineName(
                dslContext = dslContext,
                pipelineName = pipelineName,
                projectCode = projectId,
                limit = page.limit,
                offset = page.offset
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
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW
            )) {
            throw PermissionForbiddenException("$userId 无流水线$pipelineId 查看权限")
        }
        val pipelineInfo = pipelineInfoDao.getPipelineInfo(
            dslContext = dslContext,
            pipelineId = pipelineId
        ) ?: return null
        if (pipelineInfo.projectId != projectId) {
            throw ParamBlankException("$pipelineId 非 $projectId 流水线")
        }
        val hasEditPermission = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT
        )
        val templateId = templatePipelineDao.get(dslContext, pipelineId)?.templateId
        val instanceFromTemplate = templateId != null
        val favorInfos = pipelineFavorDao.listByPipelineId(
            dslContext = dslContext,
            userId = userId,
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
        return pipelineInfos.map {
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
        }
    }

    fun getByAutoIds(
        ids: List<Int>
    ): List<SimplePipeline> {
        val pipelines = pipelineInfoDao.getPieplineByAutoId(
            dslContext = dslContext,
            ids = ids
        )
        return pipelines.map {
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
        }
    }

    // 质量红线获取视图流水线
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
        val authPipelines = if (authPipelineIds.isEmpty()) {
            pipelinePermissionService.getResourceByPermission(
                userId, projectId, AuthPermission.LIST
            )
        } else {
            authPipelineIds
        }
        watch.stop()

        watch.start("s_r_summary")
        val pipelineBuildSummary = pipelineRuntimeService.getBuildSummaryRecords(projectId, channelCode)
        watch.stop()

        watch.start("s_r_fav")
        val skipPipelineIdsNew = mutableListOf<String>()
        if (pipelineBuildSummary.isNotEmpty) {
            pipelineBuildSummary.forEach {
                skipPipelineIdsNew.add(it["PIPELINE_ID"] as String)
            }
        }
        if (skipPipelineIds.isNotEmpty()) {
            skipPipelineIdsNew.addAll(skipPipelineIds)
        }

        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        var slqLimit: SQLLimit? = null
        if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

        val offset = slqLimit?.offset ?: 0
        val limit = slqLimit?.limit ?: -1
        var count = 0L
        try {

            val list = if (pipelineBuildSummary.isNotEmpty) {

                val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
                val pipelines = buildPipelines(pipelineBuildSummary, favorPipelines, authPipelines)
                val allFilterPipelines = filterViewPipelines(
                        pipelines,
                        filterByPipelineName,
                        filterByCreator,
                        filterByLabels
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
}
