/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.ElementProp
import com.tencent.devops.common.pipeline.pojo.element.ElementPropType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.constant.RepositoryMessageCode.TRIGGER_CONDITION_PREFIX
import com.tencent.devops.repository.dao.RepoPipelineRefDao
import com.tencent.devops.repository.pojo.RepoPipelineRef
import com.tencent.devops.repository.pojo.RepoPipelineRefInfo
import com.tencent.devops.repository.pojo.RepoPipelineRefRequest
import com.tencent.devops.repository.pojo.RepoPipelineRefVo
import com.tencent.devops.repository.pojo.RepoTriggerRefVo
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.store.api.atom.ServiceAtomResource
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
    private val repoPipelineRefDao: RepoPipelineRefDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RepoPipelineService::class.java)
    }

    fun updatePipelineRef(
        userId: String,
        projectId: String,
        request: RepoPipelineRefRequest
    ) {
        logger.info(
            "updatePipelineRef: [$userId|$projectId|${request.action}" +
                    "|${request.pipelineId}|${request.channel}]"
        )
        when (request.action) {
            "create_pipeline", "update_pipeline", "restore_pipeline", "op" -> {
                if (request.pipelineRefInfos.isEmpty()) {
                    cleanPipelineRef(projectId, request.pipelineId)
                    return
                }
                savePipelineRefInfo(projectId, request.pipelineId, request.pipelineRefInfos, request.channel)
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
        pipelineRefInfos: List<RepoPipelineRefInfo>,
        channel: String?
    ) {
        val repoPipelineRefs = mutableListOf<RepoPipelineRef>()
        val repoBuffer = mutableMapOf<String, Repository>()
        pipelineRefInfos.forEach { refInfo ->
            val repository = repoBuffer[refInfo.repositoryConfig.getRepositoryId()] ?: run {
                // 保存流水线关联信息时，若其中一条记录对应的代码库被删除，不影响其余代码库信息保存
                val repo = try {
                    repositoryService.serviceGet(
                        projectId = projectId,
                        repositoryConfig = refInfo.repositoryConfig
                    )
                } catch (ignored: Exception) {
                    logger.warn(
                        "fail to get repository|projectId[$projectId]|" +
                            "repoConfig[${refInfo.repositoryConfig}]", ignored
                    )
                    return@forEach
                }
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
                    taskRepoType = refInfo.repositoryConfig.repositoryType.name,
                    taskRepoHashId = refInfo.repositoryConfig.repositoryHashId,
                    taskRepoRepoName = refInfo.repositoryConfig.repositoryName,
                    triggerType = refInfo.triggerType,
                    eventType = refInfo.eventType,
                    triggerCondition = refInfo.triggerCondition,
                    triggerConditionMd5 = refInfo.triggerCondition?.let {
                        DigestUtils.md5Hex(JsonUtil.toJson(it))
                    },
                    channel = channel ?: "BS"
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
        eventType: String?,
        triggerConditionMd5: String?,
        taskRepoType: RepositoryType?,
        limit: Int,
        offset: Int
    ): SQLPage<RepoPipelineRefVo> {
        val repositoryId = HashUtil.decodeOtherIdToLong((repositoryHashId))
        // 仅展示蓝盾平台流水线关联的数据
        val count = repoPipelineRefDao.countByRepo(
            dslContext = dslContext,
            projectId = projectId,
            repositoryId = repositoryId,
            eventType = eventType,
            triggerConditionMd5 = triggerConditionMd5,
            taskRepoType = taskRepoType,
            channel = ChannelCode.BS.name
        )
        val records = repoPipelineRefDao.listByRepo(
            dslContext = dslContext,
            projectId = projectId,
            repositoryId = repositoryId,
            eventType = eventType,
            triggerConditionMd5 = triggerConditionMd5,
            channel = ChannelCode.BS.name,
            taskRepoType = taskRepoType,
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
        val pipelineRefRecords = repoPipelineRefDao.listByIds(
            dslContext = dslContext,
            ids = pipelineRefCountMap.keys.toList()
        )
        val atomProps = getAtomProp(pipelineRefRecords.map { it.atomCode }.toSet())
        val triggerRecords = pipelineRefRecords.map {
            RepoTriggerRefVo(
                projectId = it.projectId,
                repositoryHashId = HashUtil.encodeOtherLongId(it.repositoryId),
                atomCode = it.atomCode,
                triggerType = it.triggerType,
                eventTypeDesc = I18nUtil.getCodeLanMessage(
                    messageCode = "${CodeEventType.MESSAGE_CODE_PREFIX}_${it.eventType}",
                    defaultMessage = it.eventType
                ),
                taskParams = JsonUtil.to(it.taskParams, object : TypeReference<Map<String, Any>>() {}),
                triggerCondition = it.triggerCondition?.let { condition ->
                    translateCondition(condition)
                },
                triggerConditionMd5 = it.triggerConditionMd5,
                pipelineRefCount = pipelineRefCountMap[it.id] ?: 0,
                atomLogo = atomProps?.run {
                    this[it.atomCode]?.logoUrl ?: ""
                },
                eventType = it.eventType
            )
        }
        return SQLPage(count = count, records = triggerRecords)
    }

    fun countPipelineRefs(
        projectId: String,
        repositoryIds: List<Long>
    ): Map<Long, Int> {
        return repoPipelineRefDao.countPipelineRefs(
            dslContext = dslContext,
            projectId = projectId,
            repositoryIds = repositoryIds
        )
    }

    private fun translateCondition(triggerCondition: String): Map<String, Any> {
        val elementProps = JsonUtil.to(triggerCondition, object : TypeReference<List<ElementProp>>() {})
        return elementProps.associateBy(
            { I18nUtil.getCodeLanMessage("$TRIGGER_CONDITION_PREFIX.${it.name}") },
            {
                if (it.type == ElementPropType.SELECTOR) {
                    (it.value as List<*>).map { value ->
                        I18nUtil.getCodeLanMessage("$TRIGGER_CONDITION_PREFIX.${it.name}.$value")
                    }
                } else {
                    it.value
                }
            }
        )
    }

    private fun getAtomProp(atomCodes: Set<String>) = try {
        client.get(ServiceAtomResource::class).getAtomProps(atomCodes).data
    } catch (ignored: Exception) {
        logger.warn("fail to get atom prop|atomCodes[$atomCodes]")
        null
    }
}
