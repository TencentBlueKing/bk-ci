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

package com.tencent.devops.process.service.view

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.dao.label.PipelineViewTopDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.pojo.classify.PipelineNewView
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineViewBulkAdd
import com.tencent.devops.process.pojo.classify.PipelineViewBulkRemove
import com.tencent.devops.process.pojo.classify.PipelineViewDict
import com.tencent.devops.process.pojo.classify.PipelineViewFilter
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.PipelineViewPipelineCount
import com.tencent.devops.process.pojo.classify.PipelineViewPreview
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.service.view.lock.PipelineViewGroupLock
import com.tencent.devops.process.utils.PIPELINE_VIEW_UNCLASSIFIED
import java.text.Collator
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("LoopWithTooManyJumpStatements", "LongParameterList", "TooManyFunctions", "ReturnCount")
class PipelineViewGroupService @Autowired constructor(
    private val pipelineViewService: PipelineViewService,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val pipelineViewTopDao: PipelineViewTopDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val clientTokenService: ClientTokenService
) {
    private val allPipelineInfoCache = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build<String, List<TPipelineInfoRecord>>()

    fun getViewNameMap(
        projectId: String,
        pipelineIds: MutableSet<String>
    ): Map<String/*pipelineId*/, MutableList<String>/*viewNames*/> {
        val pipelineViewGroups = pipelineViewGroupDao.listByPipelineIds(dslContext, projectId, pipelineIds)
        if (pipelineViewGroups.isEmpty()) {
            return emptyMap()
        }
        val viewIds = pipelineViewGroups.map { it.viewId }.toSet()
        val views = pipelineViewDao.list(dslContext, projectId, viewIds)
        if (views.isEmpty()) {
            return emptyMap()
        }
        val viewId2Name = views.filter { it.isProject }.associate { it.id to it.name }
        val result = mutableMapOf<String, MutableList<String>>()
        for (p in pipelineViewGroups) {
            if (!viewId2Name.containsKey(p.viewId)) {
                continue
            }
            if (!result.containsKey(p.pipelineId)) {
                result[p.pipelineId] = mutableListOf()
            }
            result[p.pipelineId]!!.add(viewId2Name[p.viewId]!!)
        }

        return result
    }

    fun addViewGroup(projectId: String, userId: String, pipelineView: PipelineViewForm): String {
        checkPermission(userId, projectId, pipelineView.projected)
        var viewId = 0L
        dslContext.transaction { t ->
            val context = DSL.using(t)
            viewId = pipelineViewService.addView(userId, projectId, pipelineView, context)
            initViewGroup(
                context = context,
                pipelineView = pipelineView,
                projectId = projectId,
                viewId = viewId,
                userId = userId
            )
        }
        return HashUtil.encodeLongId(viewId)
    }

    fun updateViewGroup(
        projectId: String,
        userId: String,
        viewIdEncode: String,
        pipelineView: PipelineViewForm
    ): Boolean {
        // 获取老视图
        val viewId = HashUtil.decodeIdToLong(viewIdEncode)
        val oldView = pipelineViewDao.get(dslContext, projectId, viewId) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_VIEW_NOT_FOUND,
            params = arrayOf(viewIdEncode)
        )
        // 校验
        checkPermission(userId, projectId, pipelineView.projected, oldView.createUser)
        if (pipelineView.projected != oldView.isProject) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_VIEW_GROUP_IS_PROJECT_NO_SAME,
                defaultMessage = "view scope can`t change , user:$userId , view:$viewIdEncode , project:$projectId"
            )
        }
        if (pipelineView.viewType == PipelineViewType.UNCLASSIFIED) {
            pipelineView.viewType = oldView.viewType
        }
        // 更新视图
        var result = false
        dslContext.transaction { t ->
            val context = DSL.using(t)
            result = pipelineViewService.updateView(userId, projectId, viewId, pipelineView, context)
            if (result) {
                if (pipelineView.pipelineIds != null) {
                    pipelineViewGroupDao.remove(context, projectId, viewId)
                }
                redisOperation.delete(firstInitMark(projectId, viewId))
                initViewGroup(
                    context = context,
                    pipelineView = pipelineView,
                    projectId = projectId,
                    viewId = viewId,
                    userId = userId
                )
            }
        }
        return result
    }

    fun getView(userId: String, projectId: String, viewId: String): PipelineNewView {
        val viewRecord = pipelineViewDao.get(
            dslContext = dslContext,
            projectId = projectId,
            viewId = HashUtil.decodeIdToLong(viewId)
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_VIEW_NOT_FOUND,
            params = arrayOf(viewId)
        )

        val filters = pipelineViewService.getFilters(
            filterByName = viewRecord.filterByPipeineName,
            filterByCreator = viewRecord.filterByCreator,
            filters = viewRecord.filters
        )

        val pipelineIds = pipelineViewGroupDao.listByViewId(dslContext, projectId, viewRecord.id).map { it.pipelineId }

        return PipelineNewView(
            id = viewId,
            projectId = viewRecord.projectId,
            name = viewRecord.name,
            projected = viewRecord.isProject,
            createTime = viewRecord.createTime.timestamp(),
            updateTime = viewRecord.updateTime.timestamp(),
            creator = viewRecord.createUser,
            logic = Logic.of(viewRecord.logic),
            filters = filters,
            viewType = viewRecord.viewType,
            pipelineIds = pipelineIds
        )
    }

    fun deleteViewGroup(
        projectId: String,
        userId: String,
        viewIdEncode: String
    ): Boolean {
        val viewId = HashUtil.decodeIdToLong(viewIdEncode)
        val oldView = pipelineViewDao.get(dslContext, projectId, viewId) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_VIEW_NOT_FOUND,
            params = arrayOf(viewIdEncode)
        )
        checkPermission(userId, projectId, oldView.isProject, oldView.createUser)
        var result = false
        dslContext.transaction { t ->
            val context = DSL.using(t)
            result = pipelineViewService.deleteView(userId, projectId, viewId)
            if (result) {
                pipelineViewGroupDao.remove(context, projectId, viewId)
            }
        }
        return result
    }

    fun getClassifiedPipelineIds(projectId: String): List<String> {
        val projectViews = pipelineViewDao.list(dslContext = dslContext, projectId = projectId, isProject = true)
        if (projectViews.isEmpty()) {
            return emptyList()
        }
        return pipelineViewGroupDao.distinctPipelineIds(dslContext, projectId, projectViews.map { it.id })
    }

    fun listPipelineIdsByViewIds(projectId: String, viewIdsEncode: List<String>): List<String> {
        val viewIds = viewIdsEncode.map { HashUtil.decodeIdToLong(it) }
        val pipelineIds = mutableListOf<String>()
        val viewGroups = pipelineViewGroupDao.listByViewIds(dslContext, projectId, viewIds)
        if (viewGroups.isEmpty()) {
            pipelineIds.addAll(emptyList())
        } else {
            pipelineIds.addAll(viewGroups.map { it.pipelineId }.toList())
        }
        if (pipelineIds.isEmpty()) {
            pipelineIds.add("##NONE##") // 特殊标志,避免有些判空逻辑导致过滤器没有执行
        }
        return pipelineIds
    }

    fun listPipelineIdsByViewId(projectId: String, viewIdEncode: String): List<String> {
        return listPipelineIdsByViewIds(projectId, listOf(viewIdEncode))
    }

    fun updateGroupAfterPipelineCreate(projectId: String, pipelineId: String, userId: String) {
        PipelineViewGroupLock(redisOperation, projectId).lockAround {
            logger.info("updateGroupAfterPipelineCreate, projectId:$projectId, pipelineId:$pipelineId , userId:$userId")
            val pipelineInfo = pipelineInfoDao.getPipelineId(dslContext, projectId, pipelineId)!!
            val viewGroupCount =
                pipelineViewGroupDao.countByPipelineId(dslContext, pipelineInfo.projectId, pipelineInfo.pipelineId)
            if (viewGroupCount == 0) {
                val dynamicProjectViews =
                    pipelineViewDao.list(dslContext, pipelineInfo.projectId, PipelineViewType.DYNAMIC)
                val matchViewIds = dynamicProjectViews.asSequence()
                    .filter { pipelineViewService.matchView(it, pipelineInfo) }
                    .map { it.id }
                    .toSet()
                matchViewIds.forEach {
                    pipelineViewGroupDao.create(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        viewId = it,
                        userId = userId
                    )
                }
            }
        }
    }

    fun updateGroupAfterPipelineUpdate(projectId: String, pipelineId: String, userId: String) {
        PipelineViewGroupLock(redisOperation, projectId).lockAround {
            logger.info("updateGroupAfterPipelineUpdate, projectId:$projectId, pipelineId:$pipelineId , userId:$userId")
            val pipelineInfo = pipelineInfoDao.getPipelineId(dslContext, projectId, pipelineId)!!
            // 所有的动态项目组
            val dynamicProjectViews = pipelineViewDao.list(dslContext, pipelineInfo.projectId, PipelineViewType.DYNAMIC)
            val dynamicProjectViewIds = dynamicProjectViews.asSequence()
                .map { it.id }
                .toSet()
            // 命中的动态项目组
            val matchViewIds = dynamicProjectViews.asSequence()
                .filter { pipelineViewService.matchView(it, pipelineInfo) }
                .map { it.id }
                .toSet()
            // 已有的动态项目组
            val baseViewGroups =
                pipelineViewGroupDao.listByPipelineId(dslContext, pipelineInfo.projectId, pipelineInfo.pipelineId)
                    .filter { dynamicProjectViewIds.contains(it.viewId) }
                    .toSet()
            val baseViewIds = baseViewGroups.map { it.viewId }.toSet()
            // 新增新命中的项目组
            matchViewIds.filterNot { baseViewIds.contains(it) }.forEach {
                pipelineViewGroupDao.create(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    viewId = it,
                    userId = userId
                )
            }
            // 删除未命中的老项目组
            baseViewGroups.filterNot { matchViewIds.contains(it.viewId) }.forEach {
                pipelineViewGroupDao.remove(dslContext, it.projectId, it.viewId, it.pipelineId)
            }
        }
    }

    private fun initViewGroup(
        context: DSLContext,
        pipelineView: PipelineViewForm,
        projectId: String,
        viewId: Long,
        userId: String
    ) {
        val watcher = Watcher("initViewGroup|$projectId|$viewId|$userId")
        if (pipelineView.viewType == PipelineViewType.DYNAMIC) {
            watcher.start("initDynamicViewGroup")
            initDynamicViewGroup(pipelineViewDao.get(context, projectId, viewId)!!, userId, context)
            watcher.stop()
        } else {
            watcher.start("initStaticViewGroup")
            pipelineView.pipelineIds?.forEach {
                pipelineViewGroupDao.create(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = it,
                    viewId = viewId,
                    userId = userId
                )
            }
            watcher.stop()
        }
        LogUtils.printCostTimeWE(watcher)
    }

    private fun initDynamicViewGroup(
        view: TPipelineViewRecord,
        userId: String,
        context: DSLContext? = null
    ): List<String> {
        val projectId = view.projectId
        val firstInit = redisOperation.setIfAbsent(firstInitMark(projectId, view.id), "1", 30 * 24 * 3600, true)
        if (!firstInit) {
            return emptyList()
        }
        return PipelineViewGroupLock(redisOperation, projectId).lockAround {
            val pipelineIds = allPipelineInfos(projectId, false)
                .filter { pipelineViewService.matchView(view, it) }
                .map { it.pipelineId }
            pipelineIds.forEach {
                pipelineViewGroupDao.create(
                    dslContext = context ?: dslContext,
                    projectId = projectId,
                    pipelineId = it,
                    viewId = view.id,
                    userId = userId
                )
            }
            return@lockAround pipelineIds
        }
    }

    private fun firstInitMark(
        projectId: String?,
        viewId: Long
    ) = "initDynamicViewGroup:$projectId:$viewId"

    private fun checkPermission(userId: String, projectId: String, isProject: Boolean, creator: String? = null) {
        if (isProject) {
            if (!hasPermission(userId, projectId)) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_VIEW_GROUP_NO_PERMISSION,
                    defaultMessage = "user:$userId has no permission to edit view group, project:$projectId"
                )
            }
        } else {
            if (creator != null && userId != creator) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_DEL_PIPELINE_VIEW_NO_PERM,
                    defaultMessage = "user:$userId has no permission to edit view group, project:$projectId"
                )
            }
        }
    }

    fun preview(
        userId: String,
        projectId: String,
        pipelineView: PipelineViewForm
    ): PipelineViewPreview {
        // 获取所有流水线信息
        val allPipelineInfoMap = allPipelineInfos(projectId, true).associateBy { it.pipelineId }
        if (allPipelineInfoMap.isEmpty()) {
            return PipelineViewPreview.EMPTY
        }

        // 获取老流水线组的流水线
        val viewId = pipelineView.id
        val oldPipelineIds = if (null == viewId) {
            emptyList<String>()
        } else {
            pipelineViewGroupDao
                .listByViewId(dslContext, projectId, HashUtil.decodeIdToLong(viewId))
                .map { it.pipelineId }
                .filter { allPipelineInfoMap.containsKey(it) }
        }

        // 获取新流水线组的流水线
        val newPipelineIds = if (pipelineView.viewType == PipelineViewType.DYNAMIC) {
            val previewCondition = TPipelineViewRecord()
            previewCondition.logic = pipelineView.logic.name
            previewCondition.filterByPipeineName = StringUtils.EMPTY
            previewCondition.filterByCreator = StringUtils.EMPTY
            previewCondition.filters = objectMapper
                .writerFor(object : TypeReference<List<PipelineViewFilter>>() {})
                .writeValueAsString(pipelineView.filters)
            allPipelineInfoMap.values
                .filter { it.delete == false && pipelineViewService.matchView(previewCondition, it) }
                .map { it.pipelineId }
        } else {
            pipelineView.pipelineIds?.filter { allPipelineInfoMap.containsKey(it) } ?: emptyList()
        }

        // 新增流水线 = 新流水线 - 老流水线
        val addedPipelineInfos = newPipelineIds.asSequence()
            .filterNot { oldPipelineIds.contains(it) }
            .map { allPipelineInfoMap[it]!! }
            .map { pipelineRecord2Info(it) }
            .toList()

        // 移除流水线 = 老流水线 - 新流水线
        val removedPipelineInfos = oldPipelineIds.asSequence()
            .filterNot { newPipelineIds.contains(it) }
            .map { allPipelineInfoMap[it]!! }
            .map { pipelineRecord2Info(it) }
            .toList()

        // 保留流水线 = 老流水线 & 新流水线
        val reservePipelineInfos = newPipelineIds.asSequence()
            .filter { oldPipelineIds.contains(it) }
            .map { allPipelineInfoMap[it]!! }
            .map { pipelineRecord2Info(it) }
            .toList()

        return PipelineViewPreview(addedPipelineInfos, removedPipelineInfos, reservePipelineInfos)
    }

    @SuppressWarnings("LongMethod", "ComplexMethod")
    fun dict(userId: String, projectId: String): PipelineViewDict {
        // 流水线信息
        val pipelineInfoMap = allPipelineInfos(projectId, true).associateBy { it.pipelineId }
        if (pipelineInfoMap.isEmpty()) {
            return PipelineViewDict.EMPTY
        }
        // 流水线组信息
        val viewInfoMap = pipelineViewDao.list(dslContext, projectId).associateBy { it.id }
        // 流水线组映射关系
        val viewGroups = pipelineViewGroupDao.listByProjectId(dslContext, projectId)
        val viewGroupMap = viewInfoMap.map { it.key to mutableListOf<String>() }.toMap().toMutableMap()
        val classifiedPipelineIds = mutableSetOf<String>()

        viewGroups.forEach {
            val viewId = it.viewId
            if (!viewInfoMap.containsKey(viewId)) {
                return@forEach
            }
            viewGroupMap[viewId]!!.add(it.pipelineId)
            if (viewInfoMap[it.viewId]?.isProject == true) {
                classifiedPipelineIds.add(it.pipelineId)
            }
        }
        val personalViewList = mutableListOf<PipelineViewDict.ViewInfo>()
        val projectViewList = mutableListOf<PipelineViewDict.ViewInfo>()
        // 未分组数据加入
        projectViewList.add(
            PipelineViewDict.ViewInfo(
                viewId = PIPELINE_VIEW_UNCLASSIFIED,
                viewName = I18nUtil.getCodeLanMessage(PIPELINE_VIEW_UNCLASSIFIED),
                pipelineList = pipelineInfoMap.values
                    .filterNot { classifiedPipelineIds.contains(it.pipelineId) }
                    .map {
                        PipelineViewDict.ViewInfo.PipelineInfo(
                            pipelineId = it.pipelineId,
                            pipelineName = it.pipelineName,
                            viewId = PIPELINE_VIEW_UNCLASSIFIED,
                            delete = it.delete
                        )
                    }
            )
        )
        // 拼装返回结果
        for (view in viewInfoMap.values) {
            if (!view.isProject && view.createUser != userId) {
                continue
            }
            if (!viewGroupMap.containsKey(view.id)) {
                continue
            }
            val viewId = HashUtil.encodeLongId(view.id)
            val pipelineList = viewGroupMap[view.id]!!.filter { pipelineInfoMap.containsKey(it) }.map {
                val pipelineInfo = pipelineInfoMap[it]!!
                PipelineViewDict.ViewInfo.PipelineInfo(
                    pipelineId = pipelineInfo.pipelineId,
                    pipelineName = pipelineInfo.pipelineName,
                    viewId = viewId,
                    delete = pipelineInfo.delete
                )
            }
            val viewList = if (view.isProject) projectViewList else personalViewList
            viewList.add(
                PipelineViewDict.ViewInfo(
                    viewId = viewId,
                    viewName = view.name,
                    pipelineList = pipelineList
                )
            )
        }
        return PipelineViewDict(personalViewList, projectViewList)
    }

    private fun allPipelineInfos(projectId: String, includeDelete: Boolean): List<TPipelineInfoRecord> {
        return allPipelineInfoCache.get("$projectId-$includeDelete") {
            val pipelineInfos = mutableListOf<TPipelineInfoRecord>()
            val step = 200
            var offset = 0
            var hasNext = true
            while (hasNext) {
                val subPipelineInfos = pipelineInfoDao.listPipelineInfoByProject(
                    dslContext = dslContext,
                    projectId = projectId,
                    offset = offset,
                    limit = step,
                    deleteFlag = if (includeDelete) null else false,
                    channelCode = ChannelCode.BS
                ) ?: emptyList<TPipelineInfoRecord>()
                if (subPipelineInfos.isEmpty()) {
                    break
                }
                pipelineInfos.addAll(subPipelineInfos)
                offset += step
                hasNext = subPipelineInfos.size == step
            }
            pipelineInfos
        } ?: emptyList()
    }

    fun bulkAdd(userId: String, projectId: String, bulkAdd: PipelineViewBulkAdd): Boolean {
        val isProjectManager = hasPermission(userId, projectId)
        val viewIds = pipelineViewDao.list(
            dslContext = dslContext,
            projectId = projectId,
            viewIds = bulkAdd.viewIds.map { HashUtil.decodeIdToLong(it) }.toSet()
        ).asSequence()
            .filter { it.viewType == PipelineViewType.STATIC }
            .filter {
                if (isProjectManager) {
                    it.isProject || it.createUser == userId
                } else {
                    !it.isProject && it.createUser == userId
                }
            }.map { it.id }.toList()
        if (viewIds.isEmpty()) {
            logger.warn("bulkAdd , empty viewIds")
            return false
        }
        val pipelineIds = pipelineInfoDao.listInfoByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = bulkAdd.pipelineIds.toSet()
        ).map { it.pipelineId }
        if (pipelineIds.isEmpty()) {
            logger.warn("bulkAdd , empty pipelineIds")
            return false
        }
        PipelineViewGroupLock(redisOperation, projectId).lockAround {
            for (viewId in viewIds) {
                val existPipelineIds =
                    pipelineViewGroupDao.listByViewId(dslContext, projectId, viewId).map { it.pipelineId }.toSet()
                pipelineIds.filterNot { existPipelineIds.contains(it) }.forEach {
                    try {
                        pipelineViewGroupDao.create(
                            dslContext = dslContext,
                            projectId = projectId,
                            pipelineId = it,
                            viewId = viewId,
                            userId = userId
                        )
                    } catch (e: Exception) {
                        logger.info("bulkAdd , ignore exception, viewId:$viewId , pipelineId:$it")
                    }
                }
            }
        }
        return true
    }

    fun bulkRemove(userId: String, projectId: String, bulkRemove: PipelineViewBulkRemove): Boolean {
        val viewId = HashUtil.decodeIdToLong(bulkRemove.viewId)
        val view = pipelineViewDao.get(
            dslContext = dslContext,
            projectId = projectId,
            viewId = viewId
        ) ?: return false
        val isProjectManager = hasPermission(userId, projectId)
        if (isProjectManager && !view.isProject && view.createUser != userId) {
            logger.warn("bulkRemove , $userId is ProjectManager , but can`t remove other view")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_VIEW_GROUP_NO_PERMISSION,
                defaultMessage = "user:$userId has no permission to edit view group, project:$projectId"
            )
        }
        if (!isProjectManager && (view.isProject || view.createUser != userId)) {
            logger.warn("bulkRemove , $userId isn`t ProjectManager , just can remove self view")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_VIEW_GROUP_NO_PERMISSION,
                defaultMessage = "user:$userId has no permission to edit view group, project:$projectId"
            )
        }
        pipelineViewGroupDao.batchRemove(dslContext, projectId, viewId, bulkRemove.pipelineIds)
        return true
    }

    fun hasPermission(userId: String, projectId: String) =
        client.get(ServiceProjectAuthResource::class)
            .checkManager(clientTokenService.getSystemToken(null)!!, userId, projectId).data ?: false

    fun listView(userId: String, projectId: String, projected: Boolean?, viewType: Int?): List<PipelineNewViewSummary> {
        val views = pipelineViewDao.list(dslContext, userId, projectId, projected, viewType)
        val countByViewId = pipelineViewGroupDao.countByViewId(dslContext, projectId, views.map { it.id })
        // 确保数据都初始化一下
        views.filter { it.viewType == PipelineViewType.DYNAMIC }
            .forEach { initDynamicViewGroup(it, userId, dslContext) }
        val summaries = sortViews2Summary(projectId, userId, views, countByViewId)
        if (projected != false) {
            val classifiedPipelineIds = getClassifiedPipelineIds(projectId)
            val unclassifiedCount =
                pipelineInfoDao.countExcludePipelineIds(dslContext, projectId, classifiedPipelineIds, ChannelCode.BS)
            summaries.add(
                0, PipelineNewViewSummary(
                    id = PIPELINE_VIEW_UNCLASSIFIED,
                    projectId = projectId,
                    name = I18nUtil.getCodeLanMessage(PIPELINE_VIEW_UNCLASSIFIED),
                    projected = true,
                    createTime = LocalDateTime.now().timestamp(),
                    updateTime = LocalDateTime.now().timestamp(),
                    creator = "admin",
                    top = false,
                    viewType = PipelineViewType.UNCLASSIFIED,
                    pipelineCount = unclassifiedCount
                )
            )
        }
        return summaries
    }

    fun listViewByPipelineId(userId: String, projectId: String, pipelineId: String): List<PipelineNewViewSummary> {
        val viewGroupRecords = pipelineViewGroupDao.listByPipelineId(dslContext, projectId, pipelineId)
        val viewRecords = pipelineViewDao.list(dslContext, projectId, viewGroupRecords.map { it.viewId }.toSet())
        return viewRecords.filter { it.isProject || it.createUser == userId }.map {
            PipelineNewViewSummary(
                id = HashUtil.encodeLongId(it.id),
                projectId = it.projectId,
                name = it.name,
                projected = it.isProject,
                createTime = it.createTime.timestamp(),
                updateTime = it.updateTime.timestamp(),
                creator = it.createUser,
                viewType = it.viewType,
                pipelineCount = 0
            )
        }
    }

    private fun sortViews2Summary(
        projectId: String,
        userId: String,
        views: List<TPipelineViewRecord>,
        countByViewId: Map<Long, Int>
    ): MutableList<PipelineNewViewSummary> {
        var score = 1
        val viewScoreMap = pipelineViewTopDao.list(dslContext, projectId, userId).associate { it.viewId to score++ }

        return views.sortedWith(Comparator { a, b ->
            Collator.getInstance().compare(a.name, b.name)
        }).sortedBy {
            viewScoreMap[it.id] ?: Int.MAX_VALUE
        }.map {
            PipelineNewViewSummary(
                id = HashUtil.encodeLongId(it.id),
                projectId = it.projectId,
                name = it.name,
                projected = it.isProject,
                createTime = it.createTime.timestamp(),
                updateTime = it.updateTime.timestamp(),
                creator = it.createUser,
                top = viewScoreMap.containsKey(it.id),
                viewType = it.viewType,
                pipelineCount = countByViewId[it.id] ?: 0
            )
        }.toMutableList()
    }

    private fun pipelineRecord2Info(record: TPipelineInfoRecord): PipelineViewPreview.PipelineInfo {
        return PipelineViewPreview.PipelineInfo(
            pipelineId = record.pipelineId,
            pipelineName = record.pipelineName,
            delete = record.delete
        )
    }

    fun pipelineCount(userId: String, projectId: String, viewId: String): PipelineViewPipelineCount {
        val viewGroups = pipelineViewGroupDao.listByViewId(dslContext, projectId, HashUtil.decodeIdToLong(viewId))
        if (viewGroups.isEmpty()) {
            return PipelineViewPipelineCount.DEFAULT
        }
        val pipelineInfos = pipelineInfoDao.listInfoByPipelineIds(
            dslContext,
            projectId,
            viewGroups.map { it.pipelineId }.toSet(),
            false
        )
        val deleteCount = pipelineInfos.count { it.delete }
        return PipelineViewPipelineCount(
            normalCount = pipelineInfos.size - deleteCount,
            deleteCount = deleteCount
        )
    }

    fun initAllView() {
        val dynamicProjectIds = pipelineViewDao.listDynamicProjectId(dslContext)
        logger.info("dynamicProjectIds : $dynamicProjectIds")
        dynamicProjectIds.forEach { projectId ->
            pipelineViewDao.listDynamicViewByProjectId(dslContext, projectId).forEach { view ->
                redisOperation.delete(firstInitMark(view.projectId, view.id))
                logger.info("init start , ${view.projectId} , ${view.id}")
                initDynamicViewGroup(view, "admin")
                logger.info("init finish , ${view.projectId} , ${view.id}")
            }
        }
    }

    fun listViewIdsByPipelineId(projectId: String, pipelineId: String): Set<Long> {
        return pipelineViewGroupDao.listByPipelineId(dslContext, projectId, pipelineId).map { it.viewId }.toSet()
    }

    fun listViewIdsMap(projectId: String, pipelineIds: List<String>): Map<String, List<Long>> {
        return pipelineViewGroupDao.listByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        ).groupBy({ it.pipelineId }, { it.viewId })
    }

    fun listViewIdsByProjectId(projectId: String): Set<Long> {
        return pipelineViewGroupDao.listByProjectId(
            dslContext = dslContext,
            projectId = projectId
        ).map { it.viewId }.toSet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineViewGroupService::class.java)
    }
}
