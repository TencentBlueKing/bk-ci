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
 *
 */

package com.tencent.devops.repository.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.repository.dao.RepoPipelineRefDao
import com.tencent.devops.repository.pojo.RepoPipelineRef
import com.tencent.devops.repository.pojo.RepoPipelineRefInfo
import com.tencent.devops.repository.pojo.RepoPipelineRefRequest
import com.tencent.devops.repository.pojo.RepoPipelineRefVo
import com.tencent.devops.repository.pojo.RepoTriggerRefVo
import com.tencent.devops.repository.pojo.Repository
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepoPipelineService @Autowired constructor(
    private val repositoryService: RepositoryService,
    private val dslContext: DSLContext,
    private val repoPipelineRefDao: RepoPipelineRefDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RepoPipelineService::class.java)
    }

    fun updatePipelineRef(
        userId: String,
        projectId: String,
        request: RepoPipelineRefRequest
    ) {
        logger.info("updatePipelineRef: [$userId|$projectId|${request.action}|${request.pipelineId}}]")
        when (request.action) {
            "create_pipeline", "update_pipeline", "restore_pipeline", "op" -> {
                if (request.pipelineRefInfos.isEmpty()) {
                    cleanPipelineRef(projectId, request.pipelineId)
                    return
                }
                savePipelineRefInfo(projectId, request.pipelineId, request.pipelineRefInfos)
            }
            "delete_pipeline" -> {
                cleanPipelineRef(projectId, request.pipelineId)
            }
            else -> {
                logger.warn("action(${request.action}) not supported")
            }
        }
    }

    private fun savePipelineRefInfo(
        projectId: String,
        pipelineId: String,
        pipelineRefInfos: List<RepoPipelineRefInfo>
    ) {
        val repoPipelineRefs = mutableListOf<RepoPipelineRef>()
        val repoBuffer = mutableMapOf<String, Repository>()
        pipelineRefInfos.forEach { refInfo ->
            val repository = repoBuffer[refInfo.repositoryConfig.getRepositoryId()] ?: run {
                val repo = repositoryService.serviceGet(
                    projectId = projectId,
                    repositoryConfig = refInfo.repositoryConfig
                )
                repoBuffer[refInfo.repositoryConfig.getRepositoryId()] = repo
                repo
            }
            repoPipelineRefs.add(
                RepoPipelineRef(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = refInfo.pipelineName,
                    repositoryId = HashUtil.decodeOtherIdToLong((repository.repoHashId!!)),
                    taskId = refInfo.taskId,
                    taskName = refInfo.taskName,
                    atomCode = refInfo.atomCode,
                    atomVersion = refInfo.atomVersion,
                    atomCategory = refInfo.atomCategory,
                    taskParams = refInfo.taskParams,
                    triggerType = refInfo.triggerType,
                    eventType = refInfo.eventType,
                    triggerCondition = refInfo.triggerCondition,
                    triggerConditionMd5 = refInfo.triggerCondition?.let {
                        DigestUtils.md5Hex(JsonUtil.toJson(it))
                    },
                )
            )
        }
        savePipelineRef(projectId = projectId, pipelineId = pipelineId, repoPipelineRefs = repoPipelineRefs)
    }

    private fun savePipelineRef(
        projectId: String,
        pipelineId: String,
        repoPipelineRefs: List<RepoPipelineRef>
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val existPipelineRefs = repoPipelineRefDao.listByPipeline(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            val existRefMap = existPipelineRefs.associateBy { it.taskId }
            val refMap = repoPipelineRefs.associateBy { it.taskId }
            val toDeleteRefMap = existRefMap.filterKeys { !refMap.containsKey(it) }

            repoPipelineRefDao.batchDelete(dslContext = transactionContext, ids = toDeleteRefMap.values.map { it.id })
            repoPipelineRefDao.batchAdd(dslContext = transactionContext, repoPipelineRefs)
        }
    }

    fun cleanPipelineRef(projectId: String, pipelineId: String) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val existPipelineRefs = repoPipelineRefDao.listByPipeline(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            repoPipelineRefDao.batchDelete(dslContext = transactionContext, ids = existPipelineRefs.map { it.id })
        }
    }

    fun listPipelineRef(
        projectId: String,
        repositoryHashId: String,
        limit: Int,
        offset: Int
    ): SQLPage<RepoPipelineRefVo> {
        val repositoryId = HashUtil.decodeOtherIdToLong((repositoryHashId))
        val count = repoPipelineRefDao.countByRepo(
            dslContext = dslContext,
            projectId = projectId,
            repositoryId = repositoryId
        )
        val records = repoPipelineRefDao.listByRepo(
            dslContext = dslContext,
            projectId = projectId,
            repositoryId = repositoryId,
            limit = limit,
            offset = offset
        )
        return SQLPage(count = count, records = records)
    }

    fun listTriggerRef(
        projectId: String,
        repositoryHashId: String,
        triggerType: String?,
        eventType: String?,
        limit: Int,
        offset: Int
    ): SQLPage<RepoTriggerRefVo> {
        val repositoryId = HashUtil.decodeOtherIdToLong((repositoryHashId))
        val count = repoPipelineRefDao.countTriggerRef(
            dslContext = dslContext,
            projectId = projectId,
            repositoryId = repositoryId,
            triggerType = triggerType,
            eventType = eventType
        )
        val pipelineRefCountMap = repoPipelineRefDao.listTriggerRefIds(
            dslContext = dslContext,
            projectId = projectId,
            repositoryId = repositoryId,
            triggerType = triggerType,
            eventType = eventType,
            limit = limit,
            offset = offset
        )
        val records = repoPipelineRefDao.listByIds(
            dslContext = dslContext,
            ids = pipelineRefCountMap.keys.toList()
        ).map {
            RepoTriggerRefVo(
                projectId = it.projectId,
                repositoryHashId = HashUtil.encodeOtherLongId(it.repositoryId),
                atomCode = it.atomCode,
                triggerType = it.triggerType,
                eventType = it.eventType,
                taskParams = JsonUtil.to(it.taskParams, object : TypeReference<Map<String, Any>>() {}),
                triggerCondition = it.triggerCondition?.let { condition ->
                    JsonUtil.to(
                        condition,
                        object : TypeReference<Map<String, Any>>() {})
                },
                triggerConditionMd5 = it.triggerConditionMd5,
                pipelineRefCount = pipelineRefCountMap[it.id] ?: 0
            )
        }
        return SQLPage(count = count, records = records)
    }
}
