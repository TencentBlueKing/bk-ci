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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.jmx.pipeline.PipelineBean
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByCreator
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByLabel
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByName
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.service.PipelineUserService
import com.tencent.devops.process.service.label.PipelineGroupService
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import java.util.Collections
import javax.ws.rs.NotFoundException
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.plugin.trigger.dao.PipelineTimerDao
import com.tencent.devops.process.service.label.PipelineGroupVersionService

@Service
class PipelineVersionService @Autowired constructor(
        private val pipelineRepositoryService: PipelineRepositoryService,
        private val pipelineRepositoryVersionService: PipelineRepositoryVersionService,
        private val pipelinePermissionService: PipelinePermissionService,
        private val pipelineGroupService: PipelineGroupService,
        private val pipelineGroupVersionService: PipelineGroupVersionService,
        private val pipelineUserService: PipelineUserService,
        private val pipelineBean: PipelineBean,
        private val processJmxApi: ProcessJmxApi,
        private val dslContext: DSLContext,
        private val templatePipelineDao: TemplatePipelineDao,
        private val pipelineTimer: PipelineTimerDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineVersionService::class.java)
    }

    private fun checkPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String
    ) {
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission
            )
        ) {
            throw PermissionForbiddenException(message)
        }
    }

    private fun sortPipelineInfo(pipelines: List<PipelineInfo>, sortType: PipelineSortType) {
        Collections.sort(pipelines) { a, b ->
            when (sortType) {
                PipelineSortType.NAME -> {
                    a.pipelineName.toLowerCase().compareTo(b.pipelineName.toLowerCase())
                }
                PipelineSortType.CREATE_TIME -> {
                    b.createTime.compareTo(a.createTime)
                }
                PipelineSortType.UPDATE_TIME -> {
                    b.updateTime.compareTo(a.updateTime)
                }
            }
        }
    }

    fun getPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        version: Int,
        checkPermission: Boolean = true
    ): Model {
        logger.info("start to get pipeline($pipelineId) for project($projectId)")

        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = AuthPermission.VIEW,
                    message = "用户($userId)无权限在工程($projectId)下获取流水线($pipelineId)"
            )
        }

        logger.info("start to get pipeline info for $projectId, $pipelineId)")
        val pipelineInfo = pipelineRepositoryService.getPipelineInfoVersion(projectId, pipelineId, version)
            ?: throw NotFoundException("流水线不存在")

        if (pipelineInfo.channelCode != channelCode) {
            throw NotFoundException("流水线渠道来源${pipelineInfo.channelCode}不符合${channelCode.name}")
        }

        logger.info("start to get pipeline model for $projectId, $pipelineId)")
        val model = pipelineRepositoryVersionService.getModel(pipelineId, version)
            ?: throw NotFoundException("指定的流水线-模型不存在")
        try {
            logger.info("start to get pipeline build no for $projectId, $pipelineId)")
            val buildNo = (model.stages[0].containers[0] as TriggerContainer).buildNo
            if (buildNo != null) {
                logger.info("start to get pipeline($pipelineId) for project($projectId)")
                buildNo.buildNo = pipelineRepositoryService.getBuildNo(projectId, pipelineId) ?: buildNo.buildNo
            }

            // 获取流水线labels
            logger.info("start to get pipeline label for $projectId, $pipelineId)")
//            val groups = pipelineGroupService.getGroups(userId, projectId, pipelineId)
            val groups = pipelineGroupVersionService.getGroups(userId, projectId, pipelineId)
            val labels = mutableListOf<String>()

            logger.info("start to deal with pipeline model for $projectId, $pipelineId)")
            groups.forEach {
                labels.addAll(it.labels)
            }
            model.labels = labels
            model.name = pipelineInfo.pipelineName
            model.desc = pipelineInfo.pipelineDesc
            model.pipelineCreator = pipelineInfo.creator

            if (model.instanceFromTemplate == true) {
                logger.info("get template id for pipeline $pipelineId: ${model.templateId}")
                model.templateId = templatePipelineDao.get(dslContext, pipelineId)?.templateId
            } else {
                logger.info("do not get template id for pipeline $pipelineId")
            }

            // 编排时剔除质量红线原子
            logger.info("start to remove pipeline quality element for $projectId, $pipelineId)")
            return model.removeElements(setOf(QualityGateInElement.classType, QualityGateOutElement.classType))
        } catch (e: Exception) {
            logger.warn("Fail to get the pipeline($pipelineId) definition of project($projectId)", e)
            throw OperationException("Reading the original pipeline failed")
        }
    }

    fun deletePipelineVersion(
            userId: String,
            projectId: String,
            pipelineId: String,
            version: Int,
            channelCode: ChannelCode? = null,
            checkPermission: Boolean = true,
            delete: Boolean = false
    ) {
        val watch = StopWatch()
        var success = false
        try {
            if (checkPermission) {
                watch.start("perm_v_perm")
                checkPermission(
                        userId,
                        projectId,
                        pipelineId,
                        AuthPermission.DELETE,
                        "用户($userId)无权限在工程($projectId)下删除流水线($pipelineId)"
                )
                watch.stop()
            }

            watch.start("s_r_pipeline_del")
            pipelineRepositoryVersionService.deletePipeline(projectId, pipelineId, userId, version, channelCode, delete)
            templatePipelineDao.delete(dslContext, pipelineId)
            if (pipelineTimer.get(dslContext, pipelineId) != null) {
                pipelineTimer.delete(dslContext, pipelineId)
            }
            watch.stop()

            success = true
        } finally {
            logger.info("deletePipeline|[$projectId]|[$pipelineId]|$userId|watch=$watch")
            pipelineBean.delete(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_DELETE, watch.totalTimeMillis)
        }
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
        logger.info("filter view pipelines logic($logic) $filterByPipelineNames $filterByPipelineCreators $filterByLabels")

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
            val pipelineIds = pipelines.map { it.pipelineId }.toSet()
            val pipelineUsers = pipelineUserService.listCreateUsers(pipelineIds)

            pipelines.filter { pipeline ->
                val user = pipelineUsers[pipeline.pipelineId]
                if (user == null) {
                    true
                } else {
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

    fun count(projectId: Set<String>, channelCode: ChannelCode?): Int {
        val watch = StopWatch()
        watch.start("s_r_c_b_p")
        val grayNum = pipelineRepositoryService.countByProjectIds(projectId, channelCode)
        watch.stop()
        logger.info("count|projectId=$projectId|watch=$watch")
        return grayNum
    }

    private fun buildPipelines(
        pipelineBuildSummary: Result<out Record>,
        favorPipelines: List<String>,
        authPipelines: List<String>,
        excludePipelineId: String? = null
    ): MutableList<Pipeline> {

        val pipelines = mutableListOf<Pipeline>()
        val currentTimestamp = System.currentTimeMillis()
        val templatePipelines = getTemplatePipelines(favorPipelines.plus(authPipelines).toSet())
        val latestBuildEstimatedExecutionSeconds = 1L
        pipelineBuildSummary.forEach {
            val pipelineId = it["PIPELINE_ID"] as String
            if (excludePipelineId != null && excludePipelineId == pipelineId) {
                return@forEach // 跳过这个
            }
            val projectId = it["PROJECT_ID"] as String
            val pipelineName = it["PIPELINE_NAME"] as String
            val canManualStartup = it["MANUAL_STARTUP"] as Int == 1
            val buildNum = it["BUILD_NUM"] as Int
            val version = it["VERSION"] as Int
            val taskCount = it["TASK_COUNT"] as Int
            val creator = it["CREATOR"] as String
            val createTime = (it["CREATE_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0
            val updateTime = (it["UPDATE_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0

            val pipelineDesc = it["DESC"] as String?
            val runLockType = it["RUN_LOCK_TYPE"] as Int?

            val finishCount = it["FINISH_COUNT"] as Int? ?: 0
            val runningCount = it["RUNNING_COUNT"] as Int? ?: 0
            val buildId = it["LATEST_BUILD_ID"] as String?
            val startTime = (it["LATEST_START_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0
            val endTime = (it["LATEST_END_TIME"] as LocalDateTime?)?.timestampmilli() ?: 0
            val starter = it["LATEST_START_USER"] as String? ?: ""
            val taskName = it["LATEST_TASK_NAME"] as String?
            val buildStatus = it["LATEST_STATUS"] as Int?
            val latestBuildStatus =
                if (buildStatus != null) {
                    if (buildStatus == BuildStatus.QUALITY_CHECK_FAIL.ordinal) {
                        BuildStatus.FAILED
                    } else {
                        BuildStatus.values()[buildStatus]
                    }
                } else {
                    null
                }
            val buildCount = finishCount + runningCount + 0L
            pipelines.add(
                    Pipeline(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            pipelineName = pipelineName,
                            pipelineDesc = pipelineDesc,
                            taskCount = taskCount,
                            buildCount = buildCount,
                            lock = checkLock(runLockType),
                            canManualStartup = canManualStartup,
                            latestBuildStartTime = startTime,
                            latestBuildEndTime = endTime,
                            latestBuildStatus = latestBuildStatus,
                            latestBuildNum = buildNum,
                            latestBuildTaskName = taskName,
                            latestBuildEstimatedExecutionSeconds = latestBuildEstimatedExecutionSeconds,
                            latestBuildId = buildId,
                            deploymentTime = updateTime,
                            createTime = createTime,
                            pipelineVersion = version,
                            currentTimestamp = currentTimestamp,
                            runningBuildCount = runningCount,
                            hasPermission = authPipelines.contains(pipelineId),
                            hasCollect = favorPipelines.contains(pipelineId),
                            latestBuildUserId = starter,
                            instanceFromTemplate = templatePipelineDao.get(dslContext, pipelineId) != null,
                            creator = creator
                    )
            )
        }

        return pipelines
    }

    private fun checkLock(runLockType: Int?): Boolean {
        return if (runLockType == null) {
            false
        } else PipelineRunLockType.valueOf(runLockType) == PipelineRunLockType.LOCK
    }

    fun listPipelineVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType,
        channelCode: ChannelCode
    ): PipelineViewPipelinePage<PipelineInfo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        var slqLimit: SQLLimit? = null
        if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

        val offset = slqLimit?.offset ?: 0
        val limit = slqLimit?.limit ?: -1
        // 数据量不多，直接全拉
        val pipelines = pipelineRepositoryVersionService.listPipelineVersion(projectId, pipelineId)
        sortPipelineInfo(pipelines, sortType)
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

    private fun getTemplatePipelines(pipelineIds: Set<String>): Set<String> {
        val records = templatePipelineDao.listByPipelines(dslContext, pipelineIds)
        return records.map { it.pipelineId }.toSet()
    }

}
