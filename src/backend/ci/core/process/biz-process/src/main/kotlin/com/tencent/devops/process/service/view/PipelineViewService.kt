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

package com.tencent.devops.process.service.view

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.dao.PipelineViewUserLastViewDao
import com.tencent.devops.process.dao.PipelineViewUserSettingsDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewLabelDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.classify.PipelineNewView
import com.tencent.devops.process.pojo.classify.PipelineNewViewCreate
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineNewViewUpdate
import com.tencent.devops.process.pojo.classify.PipelineViewClassify
import com.tencent.devops.process.pojo.classify.PipelineViewFilter
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByCreator
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByLabel
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByName
import com.tencent.devops.process.pojo.classify.PipelineViewIdAndName
import com.tencent.devops.process.pojo.classify.PipelineViewSettings
import com.tencent.devops.process.pojo.classify.enums.Condition
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_FAVORITE_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class PipelineViewService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewLabelDao: PipelineViewLabelDao,
    private val pipelineViewUserSettingDao: PipelineViewUserSettingsDao,
    private val pipelineViewLastViewDao: PipelineViewUserLastViewDao,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelinePermissionService: PipelinePermissionService
) {

    fun addUsingView(userId: String, projectId: String, viewId: String) {
        if (pipelineViewLastViewDao.get(dslContext, userId, projectId) == null) {
            pipelineViewLastViewDao.create(dslContext, userId, projectId, viewId)
        } else {
            pipelineViewLastViewDao.update(dslContext, userId, projectId, viewId)
        }
    }

    fun getUsingView(userId: String, projectId: String): String? {
        return pipelineViewLastViewDao.get(dslContext, userId, projectId)?.viewId
    }

    fun getViewSettings(userId: String, projectId: String): PipelineViewSettings {
        val currentViewListAndView = getCurrentViewIdAndList(userId, projectId)
        val currentViewId = currentViewListAndView.first
        val currentViewList = currentViewListAndView.second
        return PipelineViewSettings(currentViewId, currentViewList, getViewClassifyList(userId, projectId))
    }

    fun getViewClassifyList(userId: String, projectId: String): List<PipelineViewClassify> {
        val systemViewList = listOf(
            PipelineViewIdAndName(PIPELINE_VIEW_FAVORITE_PIPELINES, FAVORITE_PIPELINES_LABEL),
            PipelineViewIdAndName(PIPELINE_VIEW_MY_PIPELINES, MY_PIPELINES_LABEL),
            PipelineViewIdAndName(PIPELINE_VIEW_ALL_PIPELINES, ALL_PIPELINES_LABEL)
        )

        val projectViewRecordList = pipelineViewDao.list(dslContext, projectId, true)
        val projectViewList = projectViewRecordList.map {
            PipelineViewIdAndName(encode(it.id), it.name)
        }

        val personViewRecordList = pipelineViewDao.list(dslContext, projectId, userId, false)
        val personViewList = personViewRecordList.map {
            PipelineViewIdAndName(encode(it.id), it.name)
        }

        val systemPipelineViewClassify = PipelineViewClassify(SYSTEM_VIEW_LABEL, systemViewList)
        val projectPipelineViewClassify = PipelineViewClassify(PROJECT_VIEW_LABEL, projectViewList)
        val personPipelineViewClassify = PipelineViewClassify(PERSON_VIEW_LABEL, personViewList)

        return listOf(systemPipelineViewClassify, projectPipelineViewClassify, personPipelineViewClassify)
    }

    fun getCurrentViewIdAndList(userId: String, projectId: String): Pair<String, List<PipelineViewIdAndName>> {
        val pipelineViewSettingsRecord = pipelineViewUserSettingDao.get(dslContext, userId, projectId)

        val currentViewList = if (pipelineViewSettingsRecord == null) {
            listOf(
                PipelineViewIdAndName(PIPELINE_VIEW_FAVORITE_PIPELINES, FAVORITE_PIPELINES_LABEL),
                PipelineViewIdAndName(PIPELINE_VIEW_MY_PIPELINES, MY_PIPELINES_LABEL),
                PipelineViewIdAndName(PIPELINE_VIEW_ALL_PIPELINES, ALL_PIPELINES_LABEL)
            )
        } else {
            val currentViewIdList = objectMapper.readValue<List<String>>(pipelineViewSettingsRecord.settings)

            val userViewIdList = currentViewIdList.filterNot { it in SYSTEM_VIEW_ID_LIST }.map { decode(it) }
            val pipelineViewRecordList = pipelineViewDao.list(dslContext, userViewIdList.toSet())

            val pipelineViewMap = mutableMapOf<Long, TPipelineViewRecord>()
            pipelineViewRecordList.forEach {
                pipelineViewMap[it.id] = it
            }

            val tmpList = mutableListOf<PipelineViewIdAndName>()
            currentViewIdList.forEach { viewId ->
                if (viewId in SYSTEM_VIEW_ID_LIST) {
                    tmpList.add(PipelineViewIdAndName(viewId, getSystemViewName(viewId)))
                    return@forEach
                }

                val viewLongId = decode(viewId)
                if (pipelineViewMap.containsKey(viewLongId)) {
                    val record = pipelineViewMap[viewLongId]!!
                    tmpList.add(PipelineViewIdAndName(encode(record.id), record.name))
                }
            }
            tmpList
        }

        val usingViewId = getUsingView(userId, projectId)
        val currentViewId = if (usingViewId != null && currentViewList.map { it.id }.contains(usingViewId)) {
            usingViewId
        } else {
            currentViewList.first().id
        }

        return Pair(currentViewId, currentViewList)
    }

    fun updateViewSettings(userId: String, projectId: String, viewIdList: List<String>) {
        if (viewIdList.size > 7) {
            throw CustomException(Response.Status.FORBIDDEN, "最多允许同时保存7个视图")
        }

        val projectViewRecordList = pipelineViewDao.list(dslContext, projectId)
        val projectViewIdList = projectViewRecordList.map { it.id }

        viewIdList.forEach { viewId ->
            if (viewId in SYSTEM_VIEW_ID_LIST) {
                return@forEach
            }
            if (!projectViewIdList.contains(decode(viewId))) {
                logger.warn("Pipeline view($viewId) not exist")
                throw CustomException(Response.Status.BAD_REQUEST, "视图($viewId)不存在")
            }
        }

        if (pipelineViewUserSettingDao.get(dslContext, userId, projectId) == null) {
            pipelineViewUserSettingDao.create(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                settings = objectMapper.writeValueAsString(viewIdList)
            )
        } else {
            pipelineViewUserSettingDao.update(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                settings = objectMapper.writeValueAsString(viewIdList)
            )
        }
    }

    fun getViews(userId: String, projectId: String): List<PipelineNewViewSummary> {
        val views = pipelineViewDao.listProjectOrUser(dslContext, projectId, true, userId)

        return views.map {
            PipelineNewViewSummary(
                id = encode(it.id),
                projectId = it.projectId,
                name = it.name,
                projected = it.isProject,
                createTime = it.createTime.timestamp(),
                updateTime = it.updateTime.timestamp(),
                creator = it.createUser
            )
        }
    }

    fun getView(userId: String, projectId: String, viewId: String): PipelineNewView {
        val viewRecord = pipelineViewDao.get(dslContext, decode(viewId))
            ?: throw CustomException(Response.Status.NOT_FOUND, "视图($viewId)不存在")

        val filters =
            getFilters(
                viewId = viewRecord.id,
                filterByName = viewRecord.filterByPipeineName,
                filterByCreator = viewRecord.filterByCreator,
                filters = viewRecord.filters
            )

        return PipelineNewView(
            id = encode(viewRecord.id),
            projectId = viewRecord.projectId,
            name = viewRecord.name,
            projected = viewRecord.isProject,
            createTime = viewRecord.createTime.timestamp(),
            updateTime = viewRecord.updateTime.timestamp(),
            creator = viewRecord.createUser,
            logic = Logic.valueOf(viewRecord.logic),
            filters = filters
        )
    }

    fun addView(userId: String, projectId: String, pipelineView: PipelineNewViewCreate): String {
        try {
            return dslContext.transactionResult { configuration ->
                val context = DSL.using(configuration)
                val viewId = pipelineViewDao.create(
                    dslContext = context,
                    projectId = projectId,
                    name = pipelineView.name,
                    logic = pipelineView.logic.name,
                    isProject = pipelineView.projected,
                    filters = objectMapper.writerFor(object :
                        TypeReference<List<PipelineViewFilter>>() {}).writeValueAsString(
                        pipelineView.filters
                    ),
                    userId = userId
                )
                encode(viewId)
            }
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to create the pipeline $pipelineView by userId")
            throw CustomException(Response.Status.BAD_REQUEST, "视图(${pipelineView.name})已存在")
        }
    }

    fun deleteView(userId: String, projectId: String, viewId: String): Boolean {
        val id = decode(viewId)
        val viewRecord = pipelineViewDao.get(dslContext, decode(viewId))
            ?: throw CustomException(Response.Status.NOT_FOUND, "视图($viewId)不存在")
        val isUserManager = isUserManager(userId, projectId)

        if (!(userId == viewRecord.createUser || (viewRecord.isProject && isUserManager))) {
            throw CustomException(Response.Status.FORBIDDEN, "用户($userId)无权限删除视图($viewId)")
        }

        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val result = pipelineViewDao.delete(context, id)
            pipelineViewLabelDao.detachLabelByView(context, id, userId)
            result
        }
    }

    fun updateView(userId: String, projectId: String, viewId: String, pipelineView: PipelineNewViewUpdate): Boolean {
        val id = decode(viewId)
        val viewRecord = pipelineViewDao.get(dslContext, decode(viewId))
            ?: throw CustomException(Response.Status.NOT_FOUND, "视图($viewId)不存在")
        val isUserManager = isUserManager(userId, projectId)

        if (!(userId == viewRecord.createUser || (viewRecord.isProject && isUserManager))) {
            throw CustomException(Response.Status.FORBIDDEN, "用户($userId)无权限编辑视图($viewId)")
        }

        try {
            return dslContext.transactionResult { configuration ->
                val context = DSL.using(configuration)
                pipelineViewLabelDao.detachLabelByView(context, id, userId)
                val result = pipelineViewDao.update(
                    dslContext = context,
                    viewId = id,
                    name = pipelineView.name,
                    logic = pipelineView.logic.name,
                    isProject = pipelineView.projected,
                    filters = objectMapper.writerFor(object :
                        TypeReference<List<PipelineViewFilter>>() {}).writeValueAsString(
                        pipelineView.filters
                    )
                )
                result
            }
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to update the pipeline $pipelineView by userId")
            throw CustomException(Response.Status.BAD_REQUEST, "视图(${pipelineView.name})已存在")
        }
    }

    fun getFilters(pipelineNewView: PipelineNewView): Triple<List<PipelineViewFilterByName>, List<PipelineViewFilterByCreator>, List<PipelineViewFilterByLabel>> {
        val filterByNames = mutableListOf<PipelineViewFilterByName>()
        val filterByCreators = mutableListOf<PipelineViewFilterByCreator>()
        val filterByLabels = mutableListOf<PipelineViewFilterByLabel>()
        pipelineNewView.filters.forEach { filter ->
            when (filter) {
                is PipelineViewFilterByName -> {
                    filterByNames.add(filter)
                }
                is PipelineViewFilterByCreator -> {
                    filterByCreators.add(filter)
                }
                is PipelineViewFilterByLabel -> {
                    filterByLabels.add(filter)
                }
            }
        }

        return Triple(filterByNames, filterByCreators, filterByLabels)
    }

    private fun getFilters(
        viewId: Long,
        filterByName: String,
        filterByCreator: String,
        filters: String?
    ): List<PipelineViewFilter> {
        val labels = pipelineViewLabelDao.getLabels(dslContext, viewId)
        val labelIds = labels.map { encode(it.labelId) }

        val allFilters = mutableListOf<PipelineViewFilter>()

        if (filters != null && filters.isNotEmpty()) {
            allFilters.addAll(objectMapper.readValue<List<PipelineViewFilter>>(filters))
        }

        if (filterByName.isNotEmpty()) {
            allFilters.add(PipelineViewFilterByName(Condition.LIKE, filterByName))
        }

        if (filterByCreator.isNotEmpty()) {
            allFilters.add(PipelineViewFilterByCreator(Condition.INCLUDE, filterByCreator.split(",")))
        }

        if (labelIds.isNotEmpty()) {
            val groupToLabelsMap = pipelineGroupService.getGroupToLabelsMap(labelIds)
            groupToLabelsMap.forEach { (groupId, labelIdList) ->
                allFilters.add(PipelineViewFilterByLabel(Condition.INCLUDE, groupId, labelIdList))
            }
        }

        return allFilters
    }

    private fun isUserManager(userId: String, projectId: String): Boolean {
        return pipelinePermissionService.isProjectUser(userId, projectId, BkAuthGroup.MANAGER)
    }

    private fun getSystemViewName(viewId: String): String {
        return when (viewId) {
            PIPELINE_VIEW_FAVORITE_PIPELINES -> FAVORITE_PIPELINES_LABEL
            PIPELINE_VIEW_MY_PIPELINES -> MY_PIPELINES_LABEL
            PIPELINE_VIEW_ALL_PIPELINES -> ALL_PIPELINES_LABEL
            else -> throw CustomException(Response.Status.NOT_FOUND, "视图($viewId)不存在")
        }
    }

    private fun encode(id: Long) = HashUtil.encodeLongId(id)

    private fun decode(id: String) = HashUtil.decodeIdToLong(id)

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineViewService::class.java)
        private const val SYSTEM_VIEW_LABEL = "系统视图"
        private const val PROJECT_VIEW_LABEL = "项目视图"
        private const val PERSON_VIEW_LABEL = "个人视图"
        private const val FAVORITE_PIPELINES_LABEL = "我的收藏"
        private const val MY_PIPELINES_LABEL = "我的流水线"
        private const val ALL_PIPELINES_LABEL = "全部流水线"
        private val SYSTEM_VIEW_ID_LIST =
            listOf(PIPELINE_VIEW_FAVORITE_PIPELINES, PIPELINE_VIEW_MY_PIPELINES, PIPELINE_VIEW_ALL_PIPELINES)
    }
}