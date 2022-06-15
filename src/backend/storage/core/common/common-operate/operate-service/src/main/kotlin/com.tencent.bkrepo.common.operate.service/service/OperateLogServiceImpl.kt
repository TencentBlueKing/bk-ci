/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.operate.service.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.operate.api.OperateLogService
import com.tencent.bkrepo.common.operate.api.pojo.OpLogListOption
import com.tencent.bkrepo.common.operate.api.pojo.OperateLog
import com.tencent.bkrepo.common.operate.api.pojo.OperateLogResponse
import com.tencent.bkrepo.common.operate.service.config.OperateProperties
import com.tencent.bkrepo.common.operate.service.dao.OperateLogDao
import com.tencent.bkrepo.common.operate.service.model.TOperateLog
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Async
import org.springframework.util.AntPathMatcher
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * OperateLogService 实现类
 */

open class OperateLogServiceImpl(
    private val operateProperties: OperateProperties,
    private val operateLogDao: OperateLogDao
) : OperateLogService {

    @Async
    override fun saveEventAsync(event: ArtifactEvent, address: String) {
        if (notNeedRecord(event)) {
            return
        }
        val log = TOperateLog(
            type = event.type,
            resourceKey = event.resourceKey,
            projectId = event.projectId,
            repoName = event.repoName,
            description = event.data,
            userId = event.userId,
            clientAddress = address
        )
        operateLogDao.insert(log)
    }

    @Async
    override fun saveEventsAsync(eventList: List<ArtifactEvent>, address: String) {
        val logs = mutableListOf<TOperateLog>()
        eventList.forEach {
            if (notNeedRecord(it)) {
                return@forEach
            }
            logs.add(
                TOperateLog(
                    type = it.type,
                    resourceKey = it.resourceKey,
                    projectId = it.projectId,
                    repoName = it.repoName,
                    description = it.data,
                    userId = it.userId,
                    clientAddress = address
                )
            )
        }
        if (logs.isNotEmpty()) {
            operateLogDao.insert(logs)
        }
    }

    override fun listPage(option: OpLogListOption): Page<OperateLog> {
        with(option) {
            val criteria = where(TOperateLog::projectId).isEqualTo(projectId)
                .and(TOperateLog::repoName).isEqualTo(repoName)
                .and(TOperateLog::type).isEqualTo(eventType)
                .and(TOperateLog::createdDate).gte(startTime).lte(endTime)
                .apply {
                    userId?.run { and(TOperateLog::userId).isEqualTo(userId) }
                    sha256?.run { and("${TOperateLog::description.name}.sha256").isEqualTo(sha256) }
                    pipelineId?.run { and("${TOperateLog::description.name}.pipelineId").isEqualTo(pipelineId) }
                    buildId?.run { and("${TOperateLog::description.name}.buildId").isEqualTo(buildId) }
                }
            if (prefixSearch) {
                criteria.and(TOperateLog::resourceKey).regex("^$resourceKey")
            } else {
                criteria.and(TOperateLog::resourceKey).isEqualTo(resourceKey)
            }
            val query = Query(criteria)
            val totalCount = operateLogDao.count(query)
            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val sort = Sort.by(Sort.Direction.valueOf(direction.toString()), TOperateLog::createdDate.name)
            val records = operateLogDao.find(query.with(pageRequest).with(sort)).map { transfer(it) }
            return Pages.ofResponse(pageRequest, totalCount, records)
        }
    }

    override fun page(
        type: String?,
        projectId: String?,
        repoName: String?,
        operator: String?,
        startTime: String?,
        endTime: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<OperateLogResponse?> {
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val query = buildOperateLogPageQuery(type, projectId, repoName, operator, startTime, endTime)
        val totalRecords = operateLogDao.count(query)
        val records = operateLogDao.find(query.with(pageRequest)).map { convert(it) }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    private fun notNeedRecord(event: ArtifactEvent): Boolean {
        val eventType = event.type.name
        val projectRepoKey = "${event.projectId}/${event.repoName}"
        if (match(operateProperties.eventType, eventType)) {
            return true
        }
        if (match(operateProperties.projectRepoKey, projectRepoKey)) {
            return true
        }
        return false
    }

    private fun match(
        rule: List<String>,
        value: String
    ): Boolean {
        rule.forEach {
            val match = if (it.contains("*")) {
                antPathMatcher.match(it, value)
            } else {
                it == value
            }
            if (match) {
                return true
            }
        }
        return false
    }

    private fun buildOperateLogPageQuery(
        type: String?,
        projectId: String?,
        repoName: String?,
        operator: String?,
        startTime: String?,
        endTime: String?
    ): Query {
        val criteria = if (type != null) {
            Criteria.where(TOperateLog::type.name).`in`(getEventList(type))
        } else {
            Criteria.where(TOperateLog::type.name).nin(nodeEvent)
        }

        projectId?.let { criteria.and(TOperateLog::projectId.name).`is`(projectId) }

        repoName?.let { criteria.and(TOperateLog::repoName.name).`is`(repoName) }

        operator?.let { criteria.and(TOperateLog::userId.name).`is`(operator) }
        val localStart = if (startTime != null && startTime.isNotBlank()) {
            LocalDateTime.parse(startTime, formatter)
        } else {
            LocalDateTime.now()
        }
        val localEnd = if (endTime != null && endTime.isNotBlank()) {
            LocalDateTime.parse(endTime, formatter)
        } else {
            LocalDateTime.now().minusMonths(3L)
        }
        criteria.and(TOperateLog::createdDate.name).gte(localStart).lte(localEnd)
        return Query(criteria).with(Sort.by(TOperateLog::createdDate.name).descending())
    }

    private fun getEventList(resourceType: String): List<EventType> {
        return when (resourceType) {
            "PROJECT" -> repositoryEvent
            "PACKAGE" -> packageEvent
            "ADMIN" -> adminEvent
            else -> listOf()
        }
    }

    private fun convert(tOperateLog: TOperateLog): OperateLogResponse {
        val content = if (packageEvent.contains(tOperateLog.type)) {
            val packageName = tOperateLog.description["packageName"] as? String
            val version = tOperateLog.description["packageVersion"] as? String
            val repoType = tOperateLog.description["packageType"] as? String
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId,
                repoType = repoType,
                resKey = "${tOperateLog.repoName}::$packageName::$version"
            )
        } else if (repositoryEvent.contains(tOperateLog.type)) {
            val repoType = tOperateLog.description["repoType"] as? String
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId,
                repoType = repoType,
                resKey = tOperateLog.repoName!!
            )
        } else if (adminEvent.contains(tOperateLog.type)) {
            val list = tOperateLog.resourceKey.readJsonString<List<String>>()
            OperateLogResponse.Content(
                resKey = list.joinToString("::")
            )
        } else if (projectEvent.contains(tOperateLog.type)) {
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId!!,
                resKey = tOperateLog.projectId!!
            )
        } else if (metadataEvent.contains(tOperateLog.type)) {
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId,
                repoType = RepositoryType.GENERIC.name,
                resKey = "${tOperateLog.repoName}::${tOperateLog.resourceKey}",
                des = tOperateLog.description.toJsonString()
            )
        } else {
            OperateLogResponse.Content(
                projectId = tOperateLog.projectId,
                resKey = tOperateLog.resourceKey,
                des = tOperateLog.description.toJsonString()
            )
        }
        return OperateLogResponse(
            createdDate = tOperateLog.createdDate,
            operate = tOperateLog.type.nick,
            userId = tOperateLog.userId,
            clientAddress = tOperateLog.clientAddress,
            result = true,
            content = content
        )
    }

    private fun transfer(tOperateLog: TOperateLog): OperateLog {
        with(tOperateLog) {
            return OperateLog(
                createdDate = createdDate,
                type = type,
                projectId = projectId,
                repoName = repoName,
                resourceKey = resourceKey,
                userId = userId,
                clientAddress = clientAddress,
                description = description
            )
        }
    }

    companion object {
        private val repositoryEvent = listOf(EventType.REPO_CREATED, EventType.REPO_UPDATED, EventType.REPO_DELETED)
        private val packageEvent = listOf(
            EventType.VERSION_CREATED, EventType.VERSION_DELETED,
            EventType.VERSION_DOWNLOAD, EventType.VERSION_UPDATED, EventType.VERSION_STAGED
        )
        private val nodeEvent = listOf(
            EventType.NODE_CREATED, EventType.NODE_DELETED, EventType.NODE_MOVED,
            EventType.NODE_RENAMED, EventType.NODE_COPIED
        )
        private val adminEvent = listOf(EventType.ADMIN_ADD, EventType.ADMIN_DELETE)
        private val projectEvent = listOf(EventType.PROJECT_CREATED)
        private val metadataEvent = listOf(EventType.METADATA_SAVED, EventType.METADATA_DELETED)
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz")
        private val antPathMatcher = AntPathMatcher()
    }
}
