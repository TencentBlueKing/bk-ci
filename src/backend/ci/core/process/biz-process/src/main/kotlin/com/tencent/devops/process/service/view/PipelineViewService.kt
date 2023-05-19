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
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CREATOR
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PIPELINE_NAME
import com.tencent.devops.process.dao.PipelineViewUserLastViewDao
import com.tencent.devops.process.dao.PipelineViewUserSettingsDao
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineLabelPipelineDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewTopDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelineGroupPermissionService
import com.tencent.devops.process.pojo.classify.PipelineNewView
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineViewClassify
import com.tencent.devops.process.pojo.classify.PipelineViewFilter
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByCreator
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByLabel
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByName
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.PipelineViewHitFilters
import com.tencent.devops.process.pojo.classify.PipelineViewIdAndName
import com.tencent.devops.process.pojo.classify.PipelineViewMatchDynamic
import com.tencent.devops.process.pojo.classify.PipelineViewSettings
import com.tencent.devops.process.pojo.classify.enums.Condition
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_FAVORITE_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineViewService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineLabelPipelineDao: PipelineLabelPipelineDao,
    private val pipelineViewTopDao: PipelineViewTopDao,
    private val pipelineViewUserSettingDao: PipelineViewUserSettingsDao,
    private val pipelineViewLastViewDao: PipelineViewUserLastViewDao,
    private val pipelineGroupService: PipelineGroupService,
    private val client: Client,
    private val pipelineGroupPermissionService: PipelineGroupPermissionService
) {
    fun addUsingView(userId: String, projectId: String, viewId: String) {
        pipelineViewLastViewDao.save(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            viewId = viewId
        )
    }

    fun getUsingView(userId: String, projectId: String): String? {
        return pipelineViewLastViewDao.get(dslContext = dslContext, userId = userId, projectId = projectId)?.viewId
    }

    fun getViewSettings(userId: String, projectId: String): PipelineViewSettings {
        val currentViewListAndView = getCurrentViewIdAndList(userId = userId, projectId = projectId)
        val currentViewId = currentViewListAndView.first
        val currentViewList = currentViewListAndView.second
        return PipelineViewSettings(
            currentViewId = currentViewId,
            currentViews = currentViewList,
            viewClassifies = getViewClassifyList(userId = userId, projectId = projectId)
        )
    }

    fun getViewClassifyList(userId: String, projectId: String): List<PipelineViewClassify> {
        val systemViewList = listOf(
            PipelineViewIdAndName(
                id = PIPELINE_VIEW_FAVORITE_PIPELINES,
                name = I18nUtil.getCodeLanMessage(
                    ProcessMessageCode.FAVORITE_PIPELINES_LABEL, language = I18nUtil.getLanguage(userId)
                )
            ),
            PipelineViewIdAndName(
                id = PIPELINE_VIEW_MY_PIPELINES,
                name = I18nUtil.getCodeLanMessage(
                    ProcessMessageCode.MY_PIPELINES_LABEL, language = I18nUtil.getLanguage(userId)
                )
            ),
            PipelineViewIdAndName(
                id = PIPELINE_VIEW_ALL_PIPELINES,
                name = I18nUtil.getCodeLanMessage(
                    ProcessMessageCode.ALL_PIPELINES_LABEL, language = I18nUtil.getLanguage(userId)
                )
            )
        )

        val projectViewRecordList = pipelineViewDao.list(
            dslContext = dslContext,
            projectId = projectId,
            isProject = true
        )
        val projectViewList = projectViewRecordList.map {
            PipelineViewIdAndName(id = encode(it.id), name = it.name)
        }

        val personViewRecordList = pipelineViewDao.list(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId,
            isProject = false
        )
        val personViewList = personViewRecordList.map {
            PipelineViewIdAndName(id = encode(it.id), name = it.name)
        }

        val systemPipelineViewClassify = PipelineViewClassify(
            label = I18nUtil.getCodeLanMessage(
                ProcessMessageCode.SYSTEM_VIEW_LABEL, language = I18nUtil.getLanguage(userId)
            ),
            viewList = systemViewList
        )
        val projectPipelineViewClassify = PipelineViewClassify(
            label = I18nUtil.getCodeLanMessage(
                ProcessMessageCode.PROJECT_VIEW_LABEL, language = I18nUtil.getLanguage(userId)
            ),
            viewList = projectViewList
        )
        val personPipelineViewClassify = PipelineViewClassify(
            label = I18nUtil.getCodeLanMessage(
                ProcessMessageCode.PERSON_VIEW_LABEL, language = I18nUtil.getLanguage(userId)
            ),
            viewList = personViewList
        )

        return listOf(systemPipelineViewClassify, projectPipelineViewClassify, personPipelineViewClassify)
    }

    fun getCurrentViewIdAndList(userId: String, projectId: String): Pair<String, List<PipelineViewIdAndName>> {
        val pipelineViewSettingsRecord = pipelineViewUserSettingDao.get(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId
        )

        val currentViewList = if (pipelineViewSettingsRecord == null) {
            listOf(
                PipelineViewIdAndName(
                    id = PIPELINE_VIEW_FAVORITE_PIPELINES,
                    name = I18nUtil.getCodeLanMessage(
                        ProcessMessageCode.FAVORITE_PIPELINES_LABEL, language = I18nUtil.getLanguage(userId)
                    )
                ),
                PipelineViewIdAndName(
                    id = PIPELINE_VIEW_MY_PIPELINES,
                    name = I18nUtil.getCodeLanMessage(
                        ProcessMessageCode.MY_PIPELINES_LABEL, language = I18nUtil.getLanguage(userId)
                    )
                ),
                PipelineViewIdAndName(
                    id = PIPELINE_VIEW_ALL_PIPELINES,
                    name = I18nUtil.getCodeLanMessage(
                        ProcessMessageCode.ALL_PIPELINES_LABEL, language = I18nUtil.getLanguage(userId)
                    )
                )
            )
        } else {
            val currentViewIdList = objectMapper.readValue<List<String>>(pipelineViewSettingsRecord.settings)

            val userViewIdList = currentViewIdList.filterNot { it in SYSTEM_VIEW_ID_LIST }.map { decode(it) }
            val pipelineViewRecordList = pipelineViewDao.list(dslContext, projectId, userViewIdList.toSet())

            val pipelineViewMap = mutableMapOf<Long, TPipelineViewRecord>()
            pipelineViewRecordList.forEach {
                pipelineViewMap[it.id] = it
            }

            val tmpList = mutableListOf<PipelineViewIdAndName>()
            currentViewIdList.forEach { viewId ->
                if (viewId in SYSTEM_VIEW_ID_LIST) {
                    tmpList.add(PipelineViewIdAndName(id = viewId, name = getSystemViewName(viewId)))
                    return@forEach
                }

                val viewLongId = decode(viewId)
                if (pipelineViewMap.containsKey(viewLongId)) {
                    val record = pipelineViewMap[viewLongId]!!
                    tmpList.add(PipelineViewIdAndName(id = encode(record.id), name = record.name))
                }
            }
            // If tmpList is empty, then the view is delete, so return the default view list
            if (tmpList.isEmpty()) {
                tmpList.addAll(
                    listOf(
                        PipelineViewIdAndName(
                            id = PIPELINE_VIEW_FAVORITE_PIPELINES,
                            name = I18nUtil.getCodeLanMessage(
                                ProcessMessageCode.FAVORITE_PIPELINES_LABEL, language = I18nUtil.getLanguage(userId)
                            )
                        ),
                        PipelineViewIdAndName(
                            id = PIPELINE_VIEW_MY_PIPELINES,
                            name = I18nUtil.getCodeLanMessage(
                                ProcessMessageCode.MY_PIPELINES_LABEL, language = I18nUtil.getLanguage(userId)
                            )
                        ),
                        PipelineViewIdAndName(
                            id = PIPELINE_VIEW_ALL_PIPELINES,
                            name = I18nUtil.getCodeLanMessage(
                                ProcessMessageCode.ALL_PIPELINES_LABEL, language = I18nUtil.getLanguage(userId)
                            )
                        )
                    )
                )
            }
            tmpList
        }

        val usingViewId = getUsingView(userId = userId, projectId = projectId)
        val currentViewId = if (usingViewId != null && currentViewList.map { it.id }.contains(usingViewId)) {
            usingViewId
        } else {
            // 如果之前没有用过， 那就先选`我的流水线`， 如果`我的流水线`也没有， 那就直接选第一个
            if (currentViewList.map { it.id }.contains(PIPELINE_VIEW_MY_PIPELINES)) {
                PIPELINE_VIEW_MY_PIPELINES
            } else {
                currentViewList.first().id
            }
        }

        return Pair(first = currentViewId, second = currentViewList)
    }

    fun updateViewSettings(userId: String, projectId: String, viewIdList: List<String>) {
        if (viewIdList.size > 30) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_VIEW_MAX_LIMIT)
        }

        val projectViewRecordList = pipelineViewDao.list(dslContext, projectId)
        val projectViewIdList = projectViewRecordList.map { it.id }

        viewIdList.forEach { viewId ->
            if (viewId in SYSTEM_VIEW_ID_LIST) {
                return@forEach
            }
            if (!projectViewIdList.contains(decode(viewId))) {
                logger.warn("[$projectId]| Pipeline view($viewId) not exist")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_VIEW_NOT_FOUND,
                    params = arrayOf(viewId)
                )
            }
        }

        if (pipelineViewUserSettingDao.get(dslContext, userId, projectId) == null) {
            pipelineViewUserSettingDao.create(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                settings = JsonUtil.toJson(viewIdList, formatted = false)
            )
        } else {
            pipelineViewUserSettingDao.update(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                settings = JsonUtil.toJson(viewIdList, formatted = false)
            )
        }
    }

    fun getViews(userId: String, projectId: String): List<PipelineNewViewSummary> {
        val views = pipelineViewDao.listAll(
            dslContext = dslContext,
            projectId = projectId,
            isProject = true,
            userId = userId
        )

        return views.map {
            PipelineNewViewSummary(
                id = encode(it.id),
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

    fun addView(
        userId: String,
        projectId: String,
        pipelineView: PipelineViewForm,
        context: DSLContext? = null
    ): Long {
        try {
            pipelineView.name = pipelineView.name.trim()
            checkForUpset(context, projectId, userId, pipelineView, true)
            val filters = if (pipelineView.viewType == PipelineViewType.DYNAMIC) {
                objectMapper.writerFor(object :
                    TypeReference<List<PipelineViewFilter>>() {}).writeValueAsString(pipelineView.filters)
            } else {
                ""
            }
            val logic = if (pipelineView.viewType == PipelineViewType.DYNAMIC) pipelineView.logic.name else ""
            val viewId = pipelineViewDao.create(
                dslContext = context ?: dslContext,
                projectId = projectId,
                name = pipelineView.name,
                logic = logic,
                isProject = pipelineView.projected,
                filters = filters,
                userId = userId,
                id = client.get(ServiceAllocIdResource::class).generateSegmentId("PIPELINE_VIEW").data,
                viewType = pipelineView.viewType
            )
            if (pipelineView.projected) {
                // 个人流水线组不需要权限管理
                pipelineGroupPermissionService.createResource(
                    userId = userId,
                    projectId = projectId,
                    viewId = viewId,
                    viewName = pipelineView.name
                )
            }
            return viewId
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to create the pipeline $pipelineView by userId")
            throw throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_VIEW_HAD_EXISTS,
                params = arrayOf(pipelineView.name)
            )
        }
    }

    fun deleteView(userId: String, projectId: String, viewId: Long, context: DSLContext? = null): Boolean {
        val success = pipelineViewDao.delete(context ?: dslContext, projectId, viewId)
        if (success) {
            pipelineGroupPermissionService.deleteResource(
                projectId = projectId,
                viewId = viewId
            )
        }
        return success
    }

    fun updateView(
        userId: String,
        projectId: String,
        viewId: Long,
        pipelineView: PipelineViewForm,
        context: DSLContext? = null
    ): Boolean {
        try {
            checkForUpset(context, projectId, userId, pipelineView, false, viewId)
            val success = pipelineViewDao.update(
                dslContext = context ?: dslContext,
                projectId = projectId,
                viewId = viewId,
                name = pipelineView.name,
                logic = pipelineView.logic.name,
                isProject = pipelineView.projected,
                filters = objectMapper.writerFor(object :
                    TypeReference<List<PipelineViewFilter>>() {}).writeValueAsString(
                    pipelineView.filters
                ),
                viewType = pipelineView.viewType
            )
            if (success && pipelineView.projected) {
                pipelineGroupPermissionService.modifyResource(
                    userId = userId,
                    projectId = projectId,
                    viewId = viewId,
                    viewName = pipelineView.name
                )
            }
            return success
        } catch (t: DuplicateKeyException) {
            logger.warn("Fail to update the pipeline $pipelineView by userId")
            throw throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_VIEW_HAD_EXISTS,
                params = arrayOf(pipelineView.name)
            )
        }
    }

    private fun checkForUpset(
        context: DSLContext?,
        projectId: String,
        userId: String,
        pipelineView: PipelineViewForm,
        isCreate: Boolean,
        viewId: Long? = null
    ) {
        if (pipelineView.name.isEmpty() || pipelineView.name.length > 16) {
            logger.warn("pipeline view name is illegal , user:$userId , project:$projectId")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_VIEW_NAME_ILLEGAL,
                defaultMessage = "pipeline group name is illegal , the length is limited to 1~16"
            )
        }
        if (isCreate) {
            val countForLimit = pipelineViewDao.countForLimit(
                dslContext = context ?: dslContext,
                projectId = projectId,
                isProject = pipelineView.projected,
                userId = userId
            )
            val limit = if (pipelineView.projected) PROJECT_VIEW_LIMIT else PERSONAL_VIEW_LIMIT

            if (countForLimit + 1 >= limit) {
                logger.warn("exceed the limit for create , project:$projectId , user:$userId , view:$pipelineView")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_VIEW_EXCEED_THE_LIMIT,
                    defaultMessage = "exceed the limit for create , the limit is : $limit"
                )
            }
        }
        val excludeIds = viewId?.let { setOf(viewId) } ?: emptySet()
        val hasSameName = if (pipelineView.projected) {
            pipelineViewDao.countByName(
                dslContext = context ?: dslContext,
                projectId = projectId,
                name = pipelineView.name,
                isProject = true,
                excludeIds = excludeIds
            ) > 0
        } else {
            pipelineViewDao.countByName(
                dslContext = context ?: dslContext,
                projectId = projectId,
                name = pipelineView.name,
                creator = userId,
                isProject = false,
                excludeIds = excludeIds
            ) > 0
        }

        if (hasSameName) {
            logger.warn("duplicate name , project:$projectId , user:$userId , view:$pipelineView")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_VIEW_DUPLICATE_NAME,
                defaultMessage = "view name is duplicate , name:${pipelineView.name}"
            )
        }
    }

    fun getFilters(pipelineNewView: PipelineNewView):
        Triple<List<PipelineViewFilterByName>, List<PipelineViewFilterByCreator>, List<PipelineViewFilterByLabel>> {
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

        return Triple(first = filterByNames, second = filterByCreators, third = filterByLabels)
    }

    fun matchView(
        pipelineView: TPipelineViewRecord,
        pipelineInfo: TPipelineInfoRecord
    ): Boolean {
        val filters = getFilters(
            filterByName = pipelineView.filterByPipeineName,
            filterByCreator = pipelineView.filterByCreator,
            filters = pipelineView.filters
        )
        for (filter in filters) {
            val match = if (filter is PipelineViewFilterByName) {
                StringUtils.containsIgnoreCase(pipelineInfo.pipelineName, filter.pipelineName)
            } else if (filter is PipelineViewFilterByCreator) {
                filter.userIds.contains(pipelineInfo.creator)
            } else if (filter is PipelineViewFilterByLabel) {
                pipelineGroupService.getViewLabelToPipelinesMap(
                    pipelineInfo.projectId,
                    filter.labelIds
                ).values.asSequence().flatten().contains(pipelineInfo.pipelineId)
            } else {
                continue
            }

            if (pipelineView.logic == Logic.OR.name && match) {
                return true
            }
            if (pipelineView.logic == Logic.AND.name && !match) {
                return false
            }
        }
        return pipelineView.logic == Logic.AND.name
    }

    fun getFilters(
        filterByName: String,
        filterByCreator: String,
        filters: String?
    ): List<PipelineViewFilter> {

        val allFilters = mutableListOf<PipelineViewFilter>()

        if (filters != null && filters.isNotEmpty()) {
            allFilters.addAll(objectMapper.readValue<List<PipelineViewFilter>>(filters))
        }

        if (filterByName.isNotEmpty()) {
            allFilters.add(PipelineViewFilterByName(condition = Condition.LIKE, pipelineName = filterByName))
        }

        if (filterByCreator.isNotEmpty()) {
            allFilters.add(
                PipelineViewFilterByCreator(
                    condition = Condition.INCLUDE,
                    userIds = filterByCreator.split(",")
                )
            )
        }

        return allFilters
    }

    private fun getSystemViewName(viewId: String): String {
        return when (viewId) {
            PIPELINE_VIEW_FAVORITE_PIPELINES -> {
                I18nUtil.getCodeLanMessage(ProcessMessageCode.FAVORITE_PIPELINES_LABEL)
            }
            PIPELINE_VIEW_MY_PIPELINES -> I18nUtil.getCodeLanMessage(ProcessMessageCode.MY_PIPELINES_LABEL)
            PIPELINE_VIEW_ALL_PIPELINES -> I18nUtil.getCodeLanMessage(ProcessMessageCode.ALL_PIPELINES_LABEL)
            else -> throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_VIEW_NOT_FOUND, params = arrayOf(viewId)
            )
        }
    }

    private fun encode(id: Long) = HashUtil.encodeLongId(id)

    private fun decode(id: String) = HashUtil.decodeIdToLong(id)

    fun topView(userId: String, projectId: String, viewId: String, enabled: Boolean): Boolean {
        val viewIdLong = decode(viewId)
        val view = pipelineViewDao.get(dslContext, projectId, viewIdLong) ?: return false
        if (!view.isProject && view.createUser != userId) {
            logger.info("top view failed because no permission")
            return false
        }
        if (enabled) {
            pipelineViewTopDao.add(
                dslContext = dslContext,
                projectId = projectId,
                viewId = viewIdLong,
                userId = userId
            )
        } else {
            pipelineViewTopDao.remove(
                dslContext = dslContext,
                projectId = projectId,
                viewId = viewIdLong,
                userId = userId
            )
        }
        return true
    }

    fun getHitFilters(userId: String, projectId: String, pipelineId: String, viewId: String): PipelineViewHitFilters {
        val pipelineView = pipelineViewDao.get(dslContext, projectId, decode(viewId))
        if (null == pipelineView || pipelineView.viewType == PipelineViewType.STATIC) {
            return PipelineViewHitFilters.EMPTY
        }
        val pipelineInfo = pipelineInfoDao.getPipelineId(dslContext, projectId, pipelineId)
            ?: return PipelineViewHitFilters.EMPTY

        val filters = getFilters(
            filterByName = pipelineView.filterByPipeineName,
            filterByCreator = pipelineView.filterByCreator,
            filters = pipelineView.filters
        )
        val hitFilters = PipelineViewHitFilters(filters = mutableListOf(), logic = pipelineView.logic)

        for (filter in filters) {
            if (filter is PipelineViewFilterByName) {
                hitFilters.filters.add(
                    PipelineViewHitFilters.FilterInfo(
                        key = MessageUtil.getMessageByLocale(BK_PIPELINE_NAME, I18nUtil.getLanguage(userId)),
                        hits = mutableListOf(
                            PipelineViewHitFilters.FilterInfo.Hit(
                                hit = StringUtils.containsIgnoreCase(pipelineInfo.pipelineName, filter.pipelineName),
                                value = filter.pipelineName
                            )
                        )
                    )
                )
            } else if (filter is PipelineViewFilterByCreator) {
                filter.userIds.forEach {
                    hitFilters.filters.add(
                        PipelineViewHitFilters.FilterInfo(
                            key = MessageUtil.getMessageByLocale(BK_CREATOR, I18nUtil.getLanguage(userId)),
                            hits = mutableListOf(
                                PipelineViewHitFilters.FilterInfo.Hit(
                                    hit = it == pipelineInfo.creator,
                                    value = it
                                )
                            )
                        )
                    )
                }
            } else if (filter is PipelineViewFilterByLabel) {
                val group = pipelineGroupDao.get(dslContext, decode(filter.groupId)) ?: continue
                val labels =
                    pipelineLabelDao.getByIds(dslContext, projectId, filter.labelIds.map { decode(it) }.toSet())
                val labelIds = pipelineLabelPipelineDao.listLabels(dslContext, projectId, pipelineId).map { it.labelId }
                labels.forEach {
                    hitFilters.filters.add(
                        PipelineViewHitFilters.FilterInfo(
                            key = group.name,
                            hits = mutableListOf(
                                PipelineViewHitFilters.FilterInfo.Hit(
                                    hit = labelIds.contains(it.id),
                                    value = it.name
                                )
                            )
                        )
                    )
                }
            } else {
                continue
            }
        }
        return hitFilters
    }

    fun matchDynamicView(
        userId: String,
        projectId: String,
        pipelineViewMatchDynamic: PipelineViewMatchDynamic
    ): List<String> {
        val viewList = pipelineViewDao.list(dslContext, projectId)
        val labelGroupMap = pipelineViewMatchDynamic.labels.associate { it.groupId to it.labelIds.toSet() }
        val result = mutableListOf<String>()
        for (view in viewList) {
            if (!view.isProject && view.createUser != userId) {
                continue
            }
            if (view.viewType == PipelineViewType.STATIC) {
                continue
            }
            val filters = getFilters(view.filterByPipeineName, view.filterByCreator, view.filters)
            var isMatch = view.logic == Logic.AND.name
            for (filter in filters) {
                val match = if (filter is PipelineViewFilterByName) {
                    StringUtils.containsIgnoreCase(pipelineViewMatchDynamic.pipelineName, filter.pipelineName)
                } else if (filter is PipelineViewFilterByCreator) {
                    filter.userIds.contains(userId)
                } else if (filter is PipelineViewFilterByLabel) {
                    val newLabels = labelGroupMap[filter.groupId]
                    if (newLabels != null) {
                        val oldLabels = filter.labelIds.toMutableSet()
                        oldLabels.retainAll(newLabels)
                        oldLabels.isNotEmpty()
                    } else {
                        false
                    }
                } else {
                    continue
                }

                if (view.logic == Logic.OR.name && match) {
                    isMatch = true
                    break
                }
                if (view.logic == Logic.AND.name && !match) {
                    isMatch = false
                    break
                }
            }
            if (isMatch) {
                result.add(encode(view.id))
            }
        }
        return result
    }

    fun viewName2viewId(
        projectId: String,
        name: String,
        isProject: Boolean
    ): String? {
        return pipelineViewDao.fetchAnyByName(
            dslContext = dslContext, projectId = projectId, name = name, isProject = isProject
        )?.id?.let { encode(it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineViewService::class.java)
        private val SYSTEM_VIEW_ID_LIST =
            listOf(PIPELINE_VIEW_FAVORITE_PIPELINES, PIPELINE_VIEW_MY_PIPELINES, PIPELINE_VIEW_ALL_PIPELINES)
        private const val PROJECT_VIEW_LIMIT = 200
        private const val PERSONAL_VIEW_LIMIT = 100
    }
}
