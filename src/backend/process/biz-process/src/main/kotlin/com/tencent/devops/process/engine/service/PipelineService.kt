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

import com.fasterxml.jackson.core.JsonParseException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.PipelineAlreadyExistException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.jmx.pipeline.PipelineBean
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.classify.PipelineViewAndPipelines
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByCreator
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByLabel
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByName
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.classify.enums.Condition
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.PipelineSettingService
import com.tencent.devops.process.service.PipelineUserService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.view.PipelineViewService
import com.tencent.devops.process.template.dao.TemplatePipelineDao
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_FAVORITE_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import java.util.Collections
import javax.ws.rs.NotFoundException

@Service
class PipelineService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewService: PipelineViewService,
    private val pipelineUserService: PipelineUserService,
    private val pipelineBean: PipelineBean,
    private val processJmxApi: ProcessJmxApi,
    private val dslContext: DSLContext,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineSettingService: PipelineSettingService,
    private val modelCheckPlugin: ModelCheckPlugin
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineService::class.java)
    }

    private fun checkPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: BkAuthPermission,
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

    private fun checkCreatePermission(
        userId: String?,
        projectId: String,
        permission: BkAuthPermission,
        message: String
    ) {
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId!!,
                projectId = projectId,
                permission = permission
            )
        ) {
            throw PermissionForbiddenException(message)
        }
    }

    fun hasPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        bkAuthPermission: BkAuthPermission
    ): Boolean = pipelinePermissionService.checkPipelinePermission(
        userId = userId,
        projectId = projectId,
        pipelineId = pipelineId,
        permission = bkAuthPermission
    )

    fun hasCreatePipelinePermission(userId: String, projectId: String): Boolean =
        pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            permission = BkAuthPermission.CREATE
        )

    private fun checkPipelineName(name: String) {
        if (name.toCharArray().size > 64) {
            throw OperationException("流水线名称过长")
        }
    }

    private fun sortPipelines(pipelines: List<Pipeline>, sortType: PipelineSortType) {
        Collections.sort(pipelines) { a, b ->
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
            }
        }
    }

    fun createPipeline(
        userId: String,
        projectId: String,
        model: Model,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        fixPipelineId: String? = null
    ): String {
        val apiStartEpoch = System.currentTimeMillis()
        var success = false
        try {
            checkPipelineName(model.name)

            if (checkPermission) {
                checkCreatePermission(userId, projectId, BkAuthPermission.CREATE, "用户($userId)无权限在工程($projectId)下创建流水线")
            }

            if (isPipelineExist(projectId, fixPipelineId, model.name, channelCode)) {
                logger.warn("The pipeline(${model.name}) is exist")
                throw PipelineAlreadyExistException("流水线(${model.name})已经存在")
            }

            var pipelineId: String? = null
            try {
                val instance = if (model.instanceFromTemplate == null || !model.instanceFromTemplate!!) {
                    // 将模版常量变更实例化为流水线变量
                    val triggerContainer = model.stages[0].containers[0] as TriggerContainer
                    instanceModel(
                        templateModel = model,
                        pipelineName = model.name,
                        buildNo = triggerContainer.buildNo,
                        param = triggerContainer.params,
                        instanceFromTemplate = false,
                        labels = model.labels
                    )
                } else {
                    model
                }
                pipelineId =
                    pipelineRepositoryService.deployPipeline(
                        instance,
                        projectId,
                        fixPipelineId,
                        userId,
                        channelCode,
                        true
                    )
                if (checkPermission) {
                    logger.info("[$pipelineId]|start to create auth")
                    try {
                        pipelinePermissionService.createResource(userId, projectId, pipelineId, model.name)
                    } catch (ignored: Throwable) {
                        if (fixPipelineId != pipelineId) {
                            throw ignored
                        }
                    }
                }
                pipelineGroupService.addPipelineLabel(userId, pipelineId, model.labels)
                pipelineUserService.create(pipelineId, userId)
                success = true
                return pipelineId
            } catch (duplicateKeyException: DuplicateKeyException) {
                logger.info("duplicateKeyException: ${duplicateKeyException.message}")
                throw PipelineAlreadyExistException("流水线已经存在，请勿重复创建")
            } catch (ignored: Throwable) {
                if (pipelineId != null) {
                    pipelineRepositoryService.deletePipeline(projectId, pipelineId, userId, channelCode, true)
                }
                throw ignored
            } finally {
                if (!success) {
                    modelCheckPlugin.beforeDeleteElementInExistsModel(userId, model, null, pipelineId)
                }
            }
        } finally {
            pipelineBean.create(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_CREATE, System.currentTimeMillis() - apiStartEpoch)
        }
    }

    /**
     * 通过流水线参数和模板来实例化流水线
     */
    fun instanceModel(
        templateModel: Model,
        pipelineName: String,
        buildNo: BuildNo?,
        param: List<BuildFormProperty>?,
        instanceFromTemplate: Boolean,
        labels: List<String>? = null
    ): Model {
        val templateTrigger = templateModel.stages[0].containers[0] as TriggerContainer
        val instanceParam = if (templateTrigger.templateParams == null) {
            mergeProperties(templateTrigger.params, param ?: emptyList())
        } else {
            mergeProperties(
                templateTrigger.params,
                mergeProperties(templateTrigger.templateParams!!, param ?: emptyList())
            )
        }

        val triggerContainer = TriggerContainer(
            null,
            templateTrigger.name,
            templateTrigger.elements,
            null, null, null, null,
            instanceParam,
            null,
            buildNo,
            templateTrigger.canRetry,
            templateTrigger.containerId
        )

        val stages = ArrayList<Stage>()

        templateModel.stages.forEachIndexed { index, stage ->
            if (index == 0) {
                stages.add(Stage(listOf(triggerContainer), null))
            } else {
                stages.add(stage)
            }
        }

        return Model(pipelineName, "", stages, labels ?: templateModel.labels, instanceFromTemplate)
    }

    /**
     * 对于模板， 合并模板的参数，以流水线的为主，如果流水线的有就会覆盖模板的
     */
    fun mergeProperties(from: List<BuildFormProperty>, to: List<BuildFormProperty>): List<BuildFormProperty> {
        val result = ArrayList<BuildFormProperty>()

        from.forEach { f ->
            var override = false
            run lit@{
                to.forEach { t ->
                    if (t.id == f.id) {
                        override = true
                        return@lit
                    }
                }
            }
            if (override) {
                return@forEach
            }
            result.add(f)
        }
        result.addAll(to)
        return result
    }

    /**
     * Copy the exist pipeline
     */
    fun copyPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        name: String,
        desc: String?,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): String {

        val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw NotFoundException("流水线不存在")

        logger.info("Start to copy the pipeline $pipelineId")
        checkPipelineName(name)
        if (checkPermission) {
            checkPermission(
                userId,
                projectId,
                pipelineId,
                BkAuthPermission.EDIT,
                "用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)"
            )
            checkCreatePermission(
                userId,
                projectId,
                BkAuthPermission.CREATE,
                "用户($userId)无权限在工程($projectId)下创建流水线($pipelineId)"
            )
        }

        if (pipeline.channelCode != channelCode) {
            throw NotFoundException("指定要复制的流水线渠道来源${pipeline.channelCode}不符合$channelCode")
        }

        val model = pipelineRepositoryService.getModel(pipelineId)
            ?: throw NotFoundException("指定要复制的流水线-模型不存在")
        try {
            val copyMode = Model(name, desc ?: model.desc, model.stages)
            modelCheckPlugin.clearUpModel(copyMode)
            return createPipeline(userId, projectId, copyMode, channelCode)
        } catch (e: JsonParseException) {
            logger.error("Parse process($pipelineId) fail", e)
            throw OperationException("非法的流水线")
        } catch (e: PipelineAlreadyExistException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Fail to get the pipeline($pipelineId) definition of project($projectId)", e)
            throw OperationException("Reading the original pipeline failed")
        }
    }

    fun editPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        checkTemplate: Boolean = true
    ) {
        if (checkTemplate && isTemplatePipeline(pipelineId)) {
            throw OperationException("模板流水线不支持编辑")
        }
        val apiStartEpoch = System.currentTimeMillis()
        var success = false
        logger.info("Start to edit the pipeline $pipelineId of project $projectId with channel $channelCode and permission $checkPermission by user $userId")
        try {
            checkPipelineName(model.name)
            if (checkPermission) {
                checkPermission(
                    userId,
                    projectId,
                    pipelineId,
                    BkAuthPermission.EDIT,
                    "用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)"
                )
            }

            if (isPipelineExist(projectId, pipelineId, model.name, channelCode)) {
                logger.warn("The pipeline(${model.name}) is exist")
                throw PipelineAlreadyExistException("流水线(${model.name})已经存在")
            }

            val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
                ?: throw NotFoundException("指定编辑的流水线不存在")

            if (pipeline.channelCode != channelCode) {
                throw NotFoundException("指定编辑的流水线渠道来源${pipeline.channelCode}不符合${channelCode.name}")
            }

            val existModel = pipelineRepositoryService.getModel(pipelineId) ?: throw NotFoundException("指定的流水线-模型不存在")
            // 对已经存在的模型做处理
            modelCheckPlugin.beforeDeleteElementInExistsModel(userId, existModel, model, pipelineId)

            pipelineRepositoryService.deployPipeline(model, projectId, pipelineId, userId, channelCode, false)
            if (checkPermission) {
                pipelinePermissionService.modifyResource(projectId, pipelineId, model.name)
            }
            pipelineGroupService.updatePipelineLabel(userId, pipelineId, model.labels)
            pipelineUserService.update(pipelineId, userId)
            success = true
        } finally {
            pipelineBean.edit(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_EDIT, System.currentTimeMillis() - apiStartEpoch)
        }
    }

    fun renamePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        name: String,
        channelCode: ChannelCode
    ) {
        val pipelineModel = getPipeline(userId, projectId, pipelineId, channelCode)
        pipelineModel.name = name
        editPipeline(userId, projectId, pipelineId, pipelineModel, channelCode, true, false)
        val pipelineDesc = pipelineInfoDao.getPipelineInfo(dslContext, pipelineId, channelCode)?.pipelineDesc
        pipelineSettingDao.updateSetting(dslContext, pipelineId, name, pipelineDesc ?: "")
        pipelineInfoDao.update(dslContext, pipelineId, userId, false, name, pipelineDesc)
    }

    fun saveAll(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        setting: PipelineSetting,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        checkTemplate: Boolean = true
    ) {
        editPipeline(userId, projectId, pipelineId, model, channelCode, checkPermission, checkTemplate)
        pipelineSettingService.saveSetting(userId, setting, false)
    }

    fun saveSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        setting: PipelineSetting,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ) {
        val pipelineModel = getPipeline(userId, projectId, pipelineId, channelCode)
        editPipeline(userId, projectId, pipelineId, pipelineModel, channelCode, checkPermission, false)
        pipelineSettingService.saveSetting(userId, setting, false)
    }

    fun getPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): Model {

        if (checkPermission) {
            checkPermission(
                userId,
                projectId,
                pipelineId,
                BkAuthPermission.VIEW,
                "用户($userId)无权限在工程($projectId)下获取流水线($pipelineId)"
            )
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw NotFoundException("流水线不存在")

        if (pipelineInfo.channelCode != channelCode) {
            throw NotFoundException("流水线渠道来源${pipelineInfo.channelCode}不符合${channelCode.name}")
        }

        val model = pipelineRepositoryService.getModel(pipelineId)
            ?: throw NotFoundException("指定的流水线-模型不存在")
        try {
            val buildNo = (model.stages[0].containers[0] as TriggerContainer).buildNo
            if (buildNo != null) {
                buildNo.buildNo = pipelineRepositoryService.getBuildNo(projectId, pipelineId) ?: buildNo.buildNo
            }

            // 获取流水线labels
            val groups = pipelineGroupService.getGroups(userId, projectId, pipelineId)
            val labels = mutableListOf<String>()
            groups.forEach {
                labels.addAll(it.labels)
            }
            model.labels = labels
            model.name = pipelineInfo.pipelineName
            model.desc = pipelineInfo.pipelineDesc
            model.pipelineCreator = pipelineInfo.creator

            return model
        } catch (e: Exception) {
            logger.warn("Fail to get the pipeline($pipelineId) definition of project($projectId)", e)
            throw OperationException("Reading the original pipeline failed")
        }
    }

    fun deletePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
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
                    BkAuthPermission.DELETE,
                    "用户($userId)无权限在工程($projectId)下删除流水线($pipelineId)"
                )
                watch.stop()
            }

            watch.start("s_r_pipeline_del")
            pipelineRepositoryService.deletePipeline(projectId, pipelineId, userId, channelCode, delete)
            templatePipelineDao.delete(dslContext, pipelineId)
            watch.stop()

            if (checkPermission) {
                watch.start("perm_d_perm")
                pipelinePermissionService.deleteResource(projectId, pipelineId)
                watch.stop()
            }
            success = true
        } finally {
            logger.info("deletePipeline|[$projectId]|[$pipelineId]|$userId|watch=$watch")
            pipelineBean.delete(success)
            processJmxApi.execute(ProcessJmxApi.NEW_PIPELINE_DELETE, watch.totalTimeMillis)
        }
    }

    fun listPipelineInfo(userId: String, projectId: String, pipelineIdList: List<String>?): List<Pipeline> {
        val pipelines =
            listPermissionPipeline(userId, projectId, null, null, PipelineSortType.CREATE_TIME, ChannelCode.BS, false)

        return if (pipelineIdList == null) {
            pipelines.records
        } else {
            pipelines.records.filter { pipelineIdList.contains(it.pipelineId) }
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

        val watch = StopWatch()
        try {

            val hasCreatePermission =
                if (!checkPermission) {
                    true
                } else {
                    watch.start("perm_v_perm")
                    val validateUserResourcePermission = hasCreatePipelinePermission(userId, projectId)
                    watch.stop()
                    validateUserResourcePermission
                }

            val pageNotNull = page ?: 0
            val pageSizeNotNull = pageSize ?: -1
            var slqLimit: SQLLimit? = null
            val hasPermissionList = if (checkPermission) {
                watch.start("perm_r_perm")
                val hasPermissionList = pipelinePermissionService.getResourceByPermission(
                    userId = userId, projectId = projectId, permission = BkAuthPermission.LIST
                )
                watch.stop()
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

            watch.start("s_r_summary")
            val pipelineBuildSummary =
                pipelineRuntimeService.getBuildSummaryRecords(projectId, channelCode, hasPermissionList)
            watch.stop()

            if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

            val offset = slqLimit?.offset ?: 0
            val limit = slqLimit?.limit ?: -1
            var hasFavorPipelines = false
            val hasPermissionPipelines = hasPermissionList.isNotEmpty()

            val allPipelines = mutableListOf<Pipeline>()

            watch.start("s_r_fav")
            if (pipelineBuildSummary.isNotEmpty) {
                val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
                hasFavorPipelines = favorPipelines.isNotEmpty()

                val pipelines = buildPipelines(pipelineBuildSummary, favorPipelines, hasPermissionList)
                allPipelines.addAll(pipelines)

                sortPipelines(allPipelines, sortType)
            }

            val toIndex =
                if (limit == -1 || allPipelines.size <= (offset + limit)) allPipelines.size else offset + limit
            val pagePipelines =
                if (offset >= allPipelines.size) listOf<Pipeline>() else allPipelines.subList(offset, toIndex)

            watch.stop()

            return PipelinePage(
                pageNotNull,
                pageSizeNotNull,
                allPipelines.size.toLong(),
                pagePipelines,
                hasCreatePermission,
                true,
                hasFavorPipelines,
                hasPermissionPipelines,
                null
            )
        } finally {
            logger.info("listPermissionPipeline|[$projectId]|$userId|watch=$watch")
            processJmxApi.execute(ProcessJmxApi.LIST_APP_PIPELINES, watch.totalTimeMillis)
        }
    }

    fun hasPermissionList(
        userId: String,
        projectId: String,
        bkAuthPermission: BkAuthPermission,
        excludePipelineId: String?,
        offset: Int,
        limit: Int
    ): SQLPage<Pipeline> {

        val watch = StopWatch()
        try {
            watch.start("perm_r_perm")
            val hasPermissionList = pipelinePermissionService.getResourceByPermission(
                userId = userId, projectId = projectId, permission = bkAuthPermission
            )
            watch.stop()
            watch.start("s_r_summary")
            val pipelineBuildSummary =
                pipelineRuntimeService.getBuildSummaryRecords(projectId, ChannelCode.BS, hasPermissionList)
            watch.stop()

            watch.start("s_r_fav")
            val count = pipelineBuildSummary.size + 0L
            val allPipelines =
                if (count > 0) {
                    val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
                    buildPipelines(pipelineBuildSummary, favorPipelines, emptyList(), excludePipelineId)
                } else {
                    mutableListOf()
                }

            val toIndex =
                if (limit == -1 || allPipelines.size <= (offset + limit)) allPipelines.size else offset + limit
            val pagePipelines = allPipelines.subList(offset, toIndex)

            watch.stop()
            return SQLPage(count, pagePipelines)
        } finally {
            logger.info("hasPermissionList|[$projectId]|$userId|watch=$watch")
            processJmxApi.execute(ProcessJmxApi.LIST_APP_PIPELINES, watch.totalTimeMillis)
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
        callByApp: Boolean? = false,
        authPipelineIds: List<String> = emptyList(),
        skipPipelineIds: List<String> = emptyList()
    ): PipelineViewPipelinePage<Pipeline> {

        val watch = StopWatch()
        watch.start("perm_r_perm")
        val authPipelines = if (authPipelineIds.isEmpty()) {
            pipelinePermissionService.getResourceByPermission(
                userId = userId, projectId = projectId, permission = BkAuthPermission.LIST
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
                val allFilterPipelines =
                    filterViewPipelines(pipelines, filterByPipelineName, filterByCreator, filterByLabels)

                val hasPipelines = allFilterPipelines.isNotEmpty()

                if (!hasPipelines) {
                    return PipelineViewPipelinePage(
                        page = pageNotNull,
                        pageSize = pageSizeNotNull,
                        count = 0,
                        records = emptyList()
                    )
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
                pipelineViewService.addUsingView(userId, projectId, viewId)

                // 排序按照先有权限再到没有权限的来
                val noPermissionList = filterPipelines.filter { !it.hasPermission }
                val permissionList = filterPipelines.filter { it.hasPermission }
                sortPipelines(permissionList, sortType)
                sortPipelines(noPermissionList, sortType)

                val allPipelines = mutableListOf<Pipeline>()
                allPipelines.addAll(permissionList)
                allPipelines.addAll(noPermissionList)
                count += allPipelines.size
                val toIndex =
                    if (limit == -1 || allPipelines.size <= (offset + limit)) allPipelines.size else offset + limit

                if (offset >= allPipelines.size) listOf<Pipeline>() else allPipelines.subList(offset, toIndex)
            } else {
                emptyList()
            }
            watch.stop()

            return PipelineViewPipelinePage(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = count,
                records = list
            )
        } finally {
            logger.info("listViewPipelines|[$projectId]|$userId|watch=$watch")
            processJmxApi.execute(ProcessJmxApi.LIST_NEW_PIPELINES, watch.totalTimeMillis)
        }
    }

    private fun filterViewPipelines(
        userId: String,
        projectId: String,
        pipelines: List<Pipeline>,
        viewId: String
    ): List<Pipeline> {
        val view = pipelineViewService.getView(userId, projectId, viewId)
        val filters = pipelineViewService.getFilters(view)

        return filterViewPipelines(pipelines, view.logic, filters.first, filters.second, filters.third)
    }

    /**
     * 视图的基础上增加简单过滤
     */
    private fun filterViewPipelines(
        pipelines: List<Pipeline>,
        filterByName: String?,
        filterByCreator: String?,
        filterByLabels: String?
    ): List<Pipeline> {
        logger.info("filter view pipelines $filterByName $filterByCreator $filterByLabels")

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
            userId, projectId, page, pageSize,
            PipelineSortType.CREATE_TIME, ChannelCode.BS, currentViewId, true
        )

        return PipelineViewAndPipelines(currentViewId, currentViewList, pipelinePage)
    }

    fun listPipelines(projectId: Set<String>, channelCode: ChannelCode): List<Pipeline> {

        val watch = StopWatch()
        val pipelines = mutableListOf<Pipeline>()
        watch.start("s_s_r_summary")
        projectId.forEach { project_id ->
            val pipelineBuildSummary = pipelineRuntimeService.getBuildSummaryRecords(project_id, channelCode)
            if (pipelineBuildSummary.isNotEmpty)
                pipelines.addAll(buildPipelines(pipelineBuildSummary, emptyList(), emptyList()))
        }
        watch.stop()
        logger.info("listPipelines|size=${pipelines.size}|watch=$watch")
        return pipelines
    }

    fun isPipelineRunning(projectId: String, buildId: String, channelCode: ChannelCode): Boolean {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
        return buildInfo != null && buildInfo.status == BuildStatus.RUNNING
    }

    fun isPipelineExist(
        projectId: String,
        pipelineId: String?,
        name: String,
        channelCode: ChannelCode
    ): Boolean {
        return pipelineRepositoryService.isPipelineExist(projectId, name, channelCode, pipelineId)
    }

    fun getPipelineStatus(userId: String, projectId: String, pipelines: Set<String>): List<Pipeline> {
        val watch = StopWatch()
        val channelCode = ChannelCode.BS
        try {
            watch.start("s_r_summary")
            val pipelineBuildSummary = pipelineRuntimeService.getBuildSummaryRecords(projectId, channelCode, pipelines)
            watch.stop()

            watch.start("perm_r_perm")
            val pipelinesPermissions = pipelinePermissionService.getResourceByPermission(
                userId = userId, projectId = projectId, permission = BkAuthPermission.LIST
            )
            watch.stop()

            watch.start("s_r_fav")
            val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
            val pipelineList = buildPipelines(pipelineBuildSummary, favorPipelines, pipelinesPermissions)
            sortPipelines(pipelineList, PipelineSortType.UPDATE_TIME)
            watch.stop()
            return pipelineList
        } finally {
            processJmxApi.execute(ProcessJmxApi.LIST_NEW_PIPELINES_STATUS, watch.totalTimeMillis)
            logger.info("getPipelineStatus[$projectId]|userId=$userId|watch=$watch")
        }
    }

    // 获取单条流水线的运行状态
    fun getSinglePipelineStatus(userId: String, projectId: String, pipeline: String): Pipeline? {
        val pipelines = setOf(pipeline)
        val pipelineList = getPipelineStatus(userId, projectId, pipelines)
        return if (pipelineList.isEmpty()) {
            null
        } else {
            pipelineList[0]
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

    fun getPipelineNameByIds(projectId: String, pipelineIds: Set<String>): Map<String, String> {

        if (pipelineIds.isEmpty()) return mapOf()
        if (projectId.isBlank()) return mapOf()

        val watch = StopWatch()
        watch.start("s_r_list_b_ps")
        val map = pipelineRepositoryService.listPipelineNameByIds(projectId, pipelineIds)
        watch.stop()
        logger.info("getPipelineNameByIds|[$projectId]|watch=$watch")
        return map
    }

    fun getBuildNoByBuildIds(projectId: String, pipelineId: String, buildIds: Set<String>): Map<String, Int> {
        if (buildIds.isEmpty()) return mapOf()
        if (projectId.isBlank()) return mapOf()
        if (pipelineId.isBlank()) return mapOf()

        val watch = StopWatch()
        watch.start("s_r_list_b_bs")
        val result = pipelineRuntimeService.listBuildInfoByBuildIds(buildIds)
        watch.stop()
        logger.info("getBuildNoByBuildIds|[$projectId]|$pipelineId|size=${buildIds.size}|result=${result.size}|watch=$watch")
        return result
    }

    fun getBuildNoByByPair(buildIds: Set<String>): Map<String, String> {
        logger.info("getBuildNoByByPair| buildIds=$buildIds")
        if (buildIds.isEmpty()) return mapOf()

        val watch = StopWatch()
        watch.start("s_r_bs")
        val result = pipelineRuntimeService.getBuildNoByByPair(buildIds)
        watch.stop()
        logger.info("getBuildNoByByPair|size=${buildIds.size}|result=${result.size}|watch=$watch")
        return result
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
                    instanceFromTemplate = templatePipelines.contains(pipelineId)
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

    fun listPermissionPipelineName(projectId: String, userId: String): List<Map<String, String>> {
        val pipelines = pipelinePermissionService.getResourceByPermission(
            userId = userId, projectId = projectId, permission = BkAuthPermission.EXECUTE
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
        val watch = StopWatch()
        watch.start("s_r_all_bn")
        val newBuildNums = pipelineRuntimeService.getAllBuildNum(projectId, pipelineId)

        val result = mutableListOf<Map<String, String>>()
        newBuildNums.forEach {
            result.add(mapOf(Pair("key", it.toString())))
        }
        watch.stop()
        return result
    }

    // 旧接口
    fun getPipelineIdAndProjectIdByBuildId(buildId: String): Pair<String, String> {
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
            ?: throw NotFoundException("构建任务${buildId}不存在")
        return Pair(buildInfo.pipelineId, buildInfo.projectId)
    }

    /**
     * 还原已经删除的流水线
     */
    fun restorePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ) {
        val watch = StopWatch()
        try {

            watch.start("s_r_restore")
            val model = pipelineRepositoryService.restorePipeline(projectId, pipelineId, userId, channelCode)
            watch.stop()

            watch.start("perm_c_perm")
            pipelinePermissionService.createResource(userId, projectId, pipelineId, model.name)
            watch.stop()
        } finally {
            logger.info("restore|[$projectId]|[$pipelineId]|$userId|watch=$watch")
        }
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
        val pipelines = pipelineRepositoryService.listDeletePipelineIdByProject(projectId)
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

    private fun isTemplatePipeline(pipelineId: String): Boolean {
        return templatePipelineDao.listByPipeline(dslContext, pipelineId) != null
    }

    private fun getTemplatePipelines(pipelineIds: Set<String>): Set<String> {
        val records = templatePipelineDao.listByPipelines(dslContext, pipelineIds)
        return records.map { it.pipelineId }.toSet()
    }
}
