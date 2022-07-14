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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.model.process.tables.records.TPipelineViewGroupRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.dao.label.PipelineViewTopDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.classify.PipelineNewViewCreate
import com.tencent.devops.process.pojo.classify.PipelineNewViewUpdate
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.view.lock.PipelineViewGroupLock
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineViewGroupService @Autowired constructor(
    private val pipelineViewService: PipelineViewService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val pipelineViewTopDao: PipelineViewTopDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation
) {
    fun addViewGroup(projectId: String, userId: String, pipelineView: PipelineNewViewCreate): String {
        checkPermission(userId, projectId, pipelineView.projected)
        var viewId = 0L
        dslContext.transaction { t ->
            val context = DSL.using(t)
            viewId = pipelineViewService.addView(userId, projectId, pipelineView, context)
            initViewGroup(
                context = context,
                viewType = pipelineView.viewType,
                pipelineIds = pipelineView.pipelineIds,
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
        pipelineView: PipelineNewViewUpdate
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
                defaultMessage = "view scope can`t change , user:$userId , view:$viewIdEncode , project:${projectId}"
            )
        }
        // 更新视图
        var result = false
        dslContext.transaction { t ->
            val context = DSL.using(t)
            result = pipelineViewService.updateView(userId, projectId, viewId, pipelineView, context)
            if (result) {
                pipelineViewGroupDao.remove(context, projectId, viewId)
                redisOperation.delete(firstInitMark(projectId, viewId))
                initViewGroup(
                    context = context,
                    viewType = pipelineView.viewType,
                    pipelineIds = pipelineView.pipelineIds,
                    projectId = projectId,
                    viewId = viewId,
                    userId = userId
                )
            }
        }
        return result
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

    fun listPipelineIdsByViewId(projectId: String, viewIdEncode: String): List<String> {
        val viewId = HashUtil.decodeIdToLong(viewIdEncode)
        val view = pipelineViewDao.get(dslContext, projectId, viewId)
        if (view == null) {
            logger.warn("null view , project:$projectId , view:$viewId")
            return emptyList()
        }
        val isStatic = view.viewType == PipelineViewType.STATIC
        val viewGroups = pipelineViewGroupDao.listByViewId(dslContext, projectId, viewId)
        if (viewGroups.isEmpty()) {
            return if (isStatic) emptyList() else initDynamicViewGroup(view, view.createUser)
        }
        return viewGroups.map { it.pipelineId }.toList()
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
                pipelineViewGroupDao.batchCreate(dslContext, matchViewIds.map {
                    val viewGroup = TPipelineViewGroupRecord()
                    viewGroup.projectId = projectId
                    viewGroup.pipelineId = pipelineId
                    viewGroup.viewId = it
                    viewGroup.creator = userId
                    viewGroup
                })
            }
        }
    }

    fun updateGroupAfterPipelineDelete(projectId: String, pipelineId: String) {
        PipelineViewGroupLock(redisOperation, projectId).lockAround {
            logger.info("updateGroupAfterPipelineDelete, projectId:$projectId, pipelineId:$pipelineId")
            pipelineViewGroupDao.listByPipelineId(dslContext, projectId, pipelineId).forEach {
                pipelineViewGroupDao.remove(dslContext, it.projectId, it.viewId, it.pipelineId)
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
            pipelineViewGroupDao.batchCreate(dslContext, matchViewIds.filterNot { baseViewIds.contains(it) }.map {
                val viewGroup = TPipelineViewGroupRecord()
                viewGroup.projectId = projectId
                viewGroup.pipelineId = pipelineId
                viewGroup.viewId = it
                viewGroup.creator = userId
                viewGroup
            })
            // 删除未命中的老项目组
            baseViewGroups.filterNot { matchViewIds.contains(it.viewId) }.forEach {
                pipelineViewGroupDao.remove(dslContext, it.projectId, it.viewId, it.pipelineId)
            }
        }
    }

    private fun initViewGroup(
        context: DSLContext,
        viewType: Int,
        pipelineIds: List<String>,
        projectId: String,
        viewId: Long,
        userId: String,
    ) {
        val watcher = Watcher("initViewGroup|$projectId|$viewId|$userId")
        if (viewType == PipelineViewType.DYNAMIC) {
            watcher.start("initDynamicViewGroup")
            initDynamicViewGroup(pipelineViewDao.get(context, projectId, viewId)!!, userId, context)
            watcher.stop()
        } else {
            watcher.start("initStaticViewGroup")
            pipelineViewGroupDao.batchCreate(context, pipelineIds.map {
                val viewGroup = TPipelineViewGroupRecord()
                viewGroup.projectId = projectId
                viewGroup.viewId = viewId
                viewGroup.pipelineId = it
                viewGroup.creator = userId
                viewGroup
            })
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
        return PipelineViewGroupLock(redisOperation, projectId).lockAround {
            val firstInit = redisOperation.setIfAbsent(firstInitMark(projectId, view.id), "1")
            if (!firstInit) {
                return@lockAround emptyList()
            }
            var hasNext = true
            val result = mutableListOf<String>()
            val step = 1000
            while (hasNext) {
                val pipelineInfos = pipelineInfoDao.listPipelineInfoByProject(
                    dslContext = context ?: dslContext,
                    projectId = projectId,
                    offset = 0,
                    limit = step
                )
                result.addAll(
                    pipelineInfos!!.asSequence()
                        .filter { pipelineViewService.matchView(view, it) }
                        .map { it.pipelineId }
                        .toList()
                )
                hasNext = pipelineInfos.size == step
            }
            pipelineViewGroupDao.batchCreate(dslContext, result.map {
                val viewGroup = TPipelineViewGroupRecord()
                viewGroup.projectId = projectId
                viewGroup.viewId = view.id
                viewGroup.pipelineId = it
                viewGroup.creator = userId
                viewGroup
            })
            return@lockAround result
        }
    }

    private fun firstInitMark(
        projectId: String?,
        viewId: Long
    ) = "initDynamicViewGroup:$projectId:$viewId"

    private fun checkPermission(userId: String, projectId: String, isProject: Boolean, creator: String? = null) {
        if (isProject) {
            if (!pipelinePermissionService.checkProjectManager(userId, projectId)) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_VIEW_GROUP_NO_PERMISSON,
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

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineViewGroupService::class.java)
    }
}
