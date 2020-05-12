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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.dao.*
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.pojo.PipelineWebhook
import com.tencent.devops.process.engine.pojo.event.PipelineDeleteEvent
import com.tencent.devops.process.plugin.load.ElementBizRegistrar
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import org.joda.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger
import javax.ws.rs.NotFoundException

/**
 *
 *
 * @version 1.0
 */
@Service
class PipelineRepositoryVersionService constructor(
        private val pipelineEventDispatcher: PipelineEventDispatcher,
        private val pipelineWebhookService: PipelineWebhookService,
        private val modelContainerIdGenerator: ModelContainerIdGenerator,
        private val modelTaskIdGenerator: ModelTaskIdGenerator,
        private val objectMapper: ObjectMapper,
        private val dslContext: DSLContext,
        private val pipelineInfoDao: PipelineInfoDao,
        private val pipelineInfoVersionDao: PipelineInfoVersionDao,
        private val pipelineResVersionDao: PipelineResVersionDao,
        private val pipelineModelTaskVersionDao: PipelineModelTaskVersionDao,
        private val pipelineSettingDao: PipelineSettingDao,
        private val pipelineSettingVersionDao: PipelineSettingVersionDao,
        private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
        private val templatePipelineDao: TemplatePipelineDao
) {

    private fun initTriggerContainer(
            s: Stage,
            containerSeqId: AtomicInteger,
            projectId: String,
            pipelineId: String,
            model: Model,
            userId: String,
            modelTasks: MutableSet<PipelineModelTask>,
            channelCode: ChannelCode,
            create: Boolean
    ) {
        if (s.containers.size != 1) {
            logger.warn("The trigger stage contain more than one container (${s.containers.size})")
            throw OperationException("非法的流水线编排")
        }
        val c = (s.containers.getOrNull(0) ?: throw OperationException("第一阶段的环境不能为空")) as TriggerContainer
        c.id = containerSeqId.get().toString()
        if (c.containerId.isNullOrBlank()) {
            c.containerId = modelContainerIdGenerator.getNextId()
        }

        val variables = c.params.map {
            it.id to it.defaultValue.toString()
        }.toMap()

        var taskSeq = 0
        pipelineWebhookService.deleteWebhook(pipelineId, userId) // 先删除再添加，避免添加没有删除
        c.elements.forEach { e ->
            if (e.id.isNullOrBlank()) {
                e.id = modelTaskIdGenerator.getNextId()
            }
            val (repositoryConfig, scmType, eventType) = when (e) {
                is CodeGitWebHookTriggerElement -> Triple(RepositoryConfigUtils.buildConfig(e), ScmType.CODE_GIT, e.eventType)
                is CodeGitlabWebHookTriggerElement -> Triple(RepositoryConfigUtils.buildConfig(e), ScmType.CODE_GITLAB, null)
                is CodeSVNWebHookTriggerElement -> Triple(RepositoryConfigUtils.buildConfig(e), ScmType.CODE_SVN, null)
                is CodeGithubWebHookTriggerElement -> Triple(RepositoryConfigUtils.buildConfig(e), ScmType.GITHUB, null)
//                is CodeTfsGitWebHookTriggerElement -> Triple(RepositoryConfigUtils.buildConfig(e), ScmType.CODE_TFS_GIT, null)
                else -> Triple(null, null, null)
            }

            if (repositoryConfig != null && scmType != null) {
                logger.info("[$pipelineId]| Trying to add the $scmType web hook for repo($repositoryConfig)")
                pipelineWebhookService.saveWebhook(
                        pipelineWebhook = PipelineWebhook(
                                projectId = projectId,
                                pipelineId = pipelineId,
                                repositoryType = scmType,
                                repoType = repositoryConfig.repositoryType,
                                repoHashId = repositoryConfig.repositoryHashId,
                                repoName = repositoryConfig.repositoryName
                        ), codeEventType = eventType, variables = variables, createPipelineFlag = create
                )
            }
            if (e is TimerTriggerElement && !(e.additionalOptions?.enable ?: false)) {
                ElementBizRegistrar.getPlugin(e)?.beforeDelete(e, userId, pipelineId)
            } else {
                ElementBizRegistrar.getPlugin(e)?.afterCreate(e, projectId, pipelineId, model.name, userId, channelCode, create)
            }
            modelTasks.add(
                PipelineModelTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    stageId = s.id!!,
                    containerId = c.id!!,
                    taskId = e.id!!,
                    taskSeq = ++taskSeq,
                    taskName = e.name,
                    atomCode = e.getAtomCode(),
                    classType = e.getClassType(),
                    taskAtom = e.getTaskAtom(),
                    taskParams = e.genTaskParams(),
                    additionalOptions = e.additionalOptions
                )
            )
        }
    }

    fun getPipelineInfo(projectId: String?, pipelineId: String, channelCode: ChannelCode? = null): PipelineInfo? {
        val template = templatePipelineDao.get(dslContext, pipelineId)
        val templateId = template?.templateId
        return pipelineInfoDao.convert(pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId, channelCode), templateId)
    }

    fun getPipelineInfoVersion(projectId: String?, pipelineId: String, version: Int, channelCode: ChannelCode? = null): PipelineInfo? {
        val template = templatePipelineDao.get(dslContext, pipelineId)
        val templateId = template?.templateId
        return pipelineInfoVersionDao.convert(pipelineInfoVersionDao.getPipelineInfo(dslContext, projectId, pipelineId, version, channelCode), templateId)
    }

    fun getModel(pipelineId: String, version: Int? = null): Model? {
        val modelString = pipelineResVersionDao.getVersionModelString(dslContext, pipelineId, version)
        return try {
            objectMapper.readValue(modelString, Model::class.java)
        } catch (e: Exception) {
            logger.error("get process($pipelineId) model fail", e)
            null
        }
    }

    fun deletePipeline(
        projectId: String,
        pipelineId: String,
        userId: String,
        version: Int,
        channelCode: ChannelCode?,
        delete: Boolean
    ) {

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            val record =
                (pipelineInfoVersionDao.getPipelineInfo(transactionContext, projectId, pipelineId, version, channelCode, delete)
                    ?: throw NotFoundException("要删除的流水线版本不存在"))
            if (delete) {
                pipelineInfoVersionDao.delete(transactionContext, projectId, pipelineId)
            } else {
                // 删除前改名，防止名称占用
                val deleteTime = LocalDateTime.now().toString("yyyyMMddHHmm")
                var deleteName = "${record.pipelineName}[$deleteTime]"
                if (deleteName.length > 255) { // 超过截断，且用且珍惜
                    deleteName = deleteName.substring(0, 255)
                }

                pipelineInfoVersionDao.softDelete(
                    transactionContext,
                    projectId,
                    pipelineId,
                    deleteName,
                    userId,
                    version,
                    channelCode
                )
                // 同时要对Setting中的name做设置
                pipelineSettingVersionDao.updateSetting(
                    transactionContext,
                    pipelineId,
                    version,
                    deleteName,
                    "DELETE BY $userId in $deleteTime"
                )
            }

            pipelineModelTaskVersionDao.deletePipelineTasks(transactionContext, projectId, pipelineId, version)

            pipelineEventDispatcher.dispatch(
                PipelineDeleteEvent(
                    source = "delete_pipeline",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    clearUpModel = delete
                ),
                PipelineModelAnalysisEvent(
                    source = "delete_pipeline",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    model = "",
                    channelCode = record.channel
                )
            )
        }
    }

    fun isPipelineExist(
        projectId: String,
        pipelineName: String,
        channelCode: ChannelCode,
        excludePipelineId: String?
    ): Boolean {
        return pipelineInfoDao.isNameExist(dslContext, projectId, pipelineName, channelCode, excludePipelineId)
    }

    fun countByProjectIds(projectIds: Set<String>, channelCode: ChannelCode?): Int {
        return pipelineInfoDao.countByProjectIds(dslContext, projectIds, channelCode)
    }

    fun listPipelineNameByIds(projectId: String, pipelineIds: Set<String>): MutableMap<String, String> {
        val listInfoByPipelineIds = pipelineInfoDao.listInfoByPipelineIds(dslContext, projectId, pipelineIds)
        val map = mutableMapOf<String, String>()
        listInfoByPipelineIds.forEach {
            map[it.pipelineId] = it.pipelineName
        }
        return map
    }

    fun getBuildNo(projectId: String, pipelineId: String): Int? {
        return pipelineBuildSummaryDao.get(dslContext, pipelineId)?.buildNo
    }

    fun getSetting(pipelineId: String): PipelineSetting? {
        val t = pipelineSettingDao.getSetting(dslContext, pipelineId)
        return if (t != null) {
            PipelineSetting(
                t.projectId,
                t.pipelineId,
                t.name,
                t.desc,
                PipelineRunLockType.valueOf(t.runLockType),
                Subscription(), Subscription(),
                emptyList(),
                t.waitQueueTimeSecond / 60,
                t.maxQueueSize
            )
        } else null
    }

    fun listPipelineVersion(projectId: String, pipelineId: String): List<PipelineInfo> {
        val result = pipelineInfoVersionDao.listPipelineVersion(dslContext, projectId, pipelineId)
        val list = mutableListOf<PipelineInfo>()

        result?.forEach {
            if (it != null) {
                val template = templatePipelineDao.get(dslContext, pipelineId)
                val templateId = template?.templateId
                list.add(pipelineInfoVersionDao.convert(it, templateId)!!)
            }
        }
        return list
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRepositoryVersionService::class.java)
    }
}
