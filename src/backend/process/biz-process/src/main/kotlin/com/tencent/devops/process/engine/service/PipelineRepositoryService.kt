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
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.element.service.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.pojo.event.PipelineCreateEvent
import com.tencent.devops.process.engine.pojo.event.PipelineDeleteEvent
import com.tencent.devops.process.engine.pojo.event.PipelineUpdateEvent
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
class PipelineRepositoryService constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val modelContainerIdGenerator: ModelContainerIdGenerator,
    private val pipelineIdGenerator: PipelineIdGenerator,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineResDao: PipelineResDao,
    private val pipelineModelTaskDao: PipelineModelTaskDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineJobMutexGroupService: PipelineJobMutexGroupService,
    private val modelCheckPlugin: ModelCheckPlugin
) {

    fun deployPipeline(
        model: Model,
        projectId: String,
        signPipelineId: String?,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean
    ): String {

        // 生成流水线ID,新流水线以p-开头，以区分以前旧数据
        val pipelineId = signPipelineId ?: pipelineIdGenerator.getNextId()

        val modelTasks = initModel(model, projectId, pipelineId, userId, create, channelCode)

        val buildNo = (model.stages[0].containers[0] as TriggerContainer).buildNo
        val container = model.stages[0].containers[0] as TriggerContainer
        var canManualStartup = false
        var canElementSkip = false
        run lit@{
            container.elements.forEach {
                if (it is ManualTriggerElement && it.isElementEnable()) {
                    canManualStartup = true
                    canElementSkip = it.canElementSkip ?: false
                    return@lit
                }
            }
        }

        return if (!create) {
            update(
                projectId,
                pipelineId,
                userId,
                model,
                canManualStartup,
                canElementSkip,
                buildNo,
                modelTasks,
                channelCode
            )
        } else {
            val version = 1
            create(
                projectId,
                pipelineId,
                version,
                model,
                userId,
                channelCode,
                canManualStartup,
                canElementSkip,
                buildNo,
                modelTasks
            )
        }
    }

    /**
     * 初始化并检查合法性
     */
    fun initModel(
        model: Model,
        projectId: String,
        pipelineId: String,
        userId: String,
        create: Boolean = true,
        channelCode: ChannelCode
    ): Set<PipelineModelTask> {

        modelCheckPlugin.checkModelIntegrity(model)

        // 初始化ID TODO 该构建环境下的ID,旧流水引擎数据无法转换为String，仍然是序号的方式
        val modelTasks = mutableSetOf<PipelineModelTask>()
        // 初始化ID 该构建环境下的ID,旧流水引擎数据无法转换为String，仍然是序号的方式
        val containerSeqId = AtomicInteger(0)
        model.stages.forEachIndexed { index, s ->
            s.id = "stage-${index + 1}"
            if (index == 0) { // 在流程模型中初始化触发类容器
                initTriggerContainer(s, containerSeqId, projectId, pipelineId, model, userId, modelTasks, channelCode)
            } else {
                initOtherContainer(s, projectId, containerSeqId, userId, pipelineId, model, modelTasks, channelCode)
            }
        }

        return modelTasks
    }

    private fun initTriggerContainer(
        s: Stage,
        containerSeqId: AtomicInteger,
        projectId: String,
        pipelineId: String,
        model: Model,
        userId: String,
        modelTasks: MutableSet<PipelineModelTask>,
        channelCode: ChannelCode
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

        var taskSeq = 0
        c.elements.forEach { e ->
            if (e.id.isNullOrBlank()) {
                e.id = modelTaskIdGenerator.getNextId()
            }

            ElementBizRegistrar.getPlugin(e)?.afterCreate(e, projectId, pipelineId, model.name, userId, channelCode)

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

    private fun initOtherContainer(
        s: Stage,
        projectId: String,
        containerSeqId: AtomicInteger,
        userId: String,
        pipelineId: String,
        model: Model,
        modelTasks: MutableSet<PipelineModelTask>,
        channelCode: ChannelCode
    ) {
        if (s.containers.isEmpty()) {
            throw OperationException("阶段的环境不能为空")
        }
        s.containers.forEach { c ->

            if (c is TriggerContainer) {
                return@forEach
            }

            val mutexGroup = when (c) {
                is VMBuildContainer -> c.mutexGroup
                is NormalContainer -> c.mutexGroup
                else -> null
            }

            // 当mutexGroupName不为空的时候
            if (!mutexGroup?.mutexGroupName.isNullOrBlank()) {
                pipelineJobMutexGroupService.create(
                    projectId = projectId,
                    jobMutexGroupName = mutexGroup!!.mutexGroupName!!
                )
            }

            modelCheckPlugin.checkJob(
                projectId = projectId, pipelineId = pipelineId, jobContainer = c, userId = userId
            )

            var taskSeq = 0
            c.id = containerSeqId.incrementAndGet().toString()
            if (c.containerId.isNullOrBlank()) {
                c.containerId = UUIDUtil.generate()
            }
            c.elements.forEach { e ->
                if (e.id.isNullOrBlank()) {
                    e.id = modelTaskIdGenerator.getNextId()
                }

                when (e) {
                    is SubPipelineCallElement -> { // 子流水线循环依赖检查
                        val existPipelines = HashSet<String>()
                        existPipelines.add(pipelineId)
                        checkSubpipeline(projectId, e.subPipelineId, existPipelines)
                    }
                }

                // 补偿动作--未来拆分出来，针对复杂的东西异步处理
                ElementBizRegistrar.getPlugin(e)?.afterCreate(e, projectId, pipelineId, model.name, userId, channelCode)

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
    }

    private fun create(
        projectId: String,
        pipelineId: String,
        version: Int,
        model: Model,
        userId: String,
        channelCode: ChannelCode,
        canManualStartup: Boolean,
        canElementSkip: Boolean,
        buildNo: BuildNo?,
        modelTasks: Set<PipelineModelTask>
    ): String {

        val taskCount: Int = model.taskCount()
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineInfoDao.create(
                transactionContext,
                pipelineId,
                projectId,
                version,
                model.name,
                userId,
                channelCode,
                canManualStartup,
                canElementSkip,
                taskCount
            )
            pipelineResDao.create(transactionContext, pipelineId, version, model)
            if (model.instanceFromTemplate == null ||
                !model.instanceFromTemplate!!
            ) {
                if (null == pipelineSettingDao.getSetting(transactionContext, pipelineId)) {
                    pipelineSettingDao.insertNewSetting(transactionContext, projectId, pipelineId, model.name)
                } else {
                    pipelineSettingDao.updateSetting(transactionContext, pipelineId, model.name, model.desc ?: "")
                }
            }
            pipelineModelTaskDao.batchSave(transactionContext, modelTasks)
        }

        pipelineEventDispatcher.dispatch(
            PipelineCreateEvent("create_pipeline", projectId, pipelineId, userId, buildNo),
            PipelineModelAnalysisEvent(
                "create_pipeline",
                projectId,
                pipelineId,
                userId,
                JsonUtil.toJson(model),
                channelCode.name
            )
        )
        return pipelineId
    }

    private fun update(
        projectId: String,
        pipelineId: String,
        userId: String,
        model: Model,
        canManualStartup: Boolean,
        canElementSkip: Boolean,
        buildNo: BuildNo?,
        modelTasks: Set<PipelineModelTask>,
        channelCode: ChannelCode
    ): String {
        val taskCount: Int = model.taskCount()
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val version = pipelineInfoDao.update(
                transactionContext,
                pipelineId,
                userId,
                true,
                null,
                null,
                canManualStartup,
                canElementSkip,
                buildNo,
                taskCount
            )
            pipelineResDao.create(transactionContext, pipelineId, version, model)
            pipelineModelTaskDao.deletePipelineTasks(transactionContext, projectId, pipelineId)
            pipelineModelTaskDao.batchSave(transactionContext, modelTasks)
//            pipelineSettingDao.updateSetting(transactionContext, pipelineId, model.name, model.desc ?: "")
        }

        pipelineEventDispatcher.dispatch(
            PipelineUpdateEvent("update_pipeline", projectId, pipelineId, userId, buildNo),
            PipelineModelAnalysisEvent(
                "update_pipeline",
                projectId,
                pipelineId,
                userId,
                JsonUtil.toJson(model),
                channelCode.name
            )
        )
        return pipelineId
    }

    fun getPipelineInfo(projectId: String?, pipelineId: String, channelCode: ChannelCode? = null): PipelineInfo? {
        return pipelineInfoDao.convert(pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId, channelCode))
    }

    fun getPipelineInfo(pipelineId: String, channelCode: ChannelCode? = null): PipelineInfo? {
        return getPipelineInfo(projectId = null, pipelineId = pipelineId, channelCode = channelCode)
    }

    fun getModel(pipelineId: String, version: Int? = null): Model? {
        val modelString = pipelineResDao.getVersionModelString(dslContext, pipelineId, version)
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
        channelCode: ChannelCode?,
        delete: Boolean
    ) {

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            val record =
                (pipelineInfoDao.getPipelineInfo(transactionContext, projectId, pipelineId, channelCode, delete)
                    ?: throw NotFoundException("要删除的流水线不存在"))
            if (delete) {
                pipelineInfoDao.delete(transactionContext, projectId, pipelineId)
            } else {
                // 删除前改名，防止名称占用
                val deleteTime = LocalDateTime.now().toString("yyyyMMddHHmm")
                var deleteName = "${record.pipelineName}[$deleteTime]"
                if (deleteName.length > 255) { // 超过截断，且用且珍惜
                    deleteName = deleteName.substring(0, 255)
                }

                pipelineInfoDao.softDelete(
                    transactionContext,
                    projectId,
                    pipelineId,
                    deleteName,
                    userId,
                    channelCode
                )
                // 同时要对Setting中的name做设置
                pipelineSettingDao.updateSetting(
                    transactionContext,
                    pipelineId,
                    deleteName,
                    "DELETE BY $userId in $deleteTime"
                )
            }

            pipelineModelTaskDao.deletePipelineTasks(transactionContext, projectId, pipelineId)

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

    private fun checkSubpipeline(projectId: String, pipelineId: String, existPipelines: HashSet<String>) {

        if (existPipelines.contains(pipelineId)) {
            logger.info("[$projectId|$pipelineId] Sub pipeline call [$existPipelines|$pipelineId]")
            throw OperationException("子流水线不允许循环调用")
        }
        existPipelines.add(pipelineId)
        val pipeline = getPipelineInfo(projectId, pipelineId)
        if (pipeline == null) {
            logger.warn("The sub pipeline($pipelineId) is not exist")
            return
        }

        val existModel = getModel(pipelineId, pipeline.version)

        if (existModel == null) {
            logger.warn("The pipeline($pipelineId) is not exist")
            return
        }

        val currentExistPipelines = HashSet<String>(existPipelines)
        existModel.stages.forEachIndexed stage@{ index, stage ->
            if (index == 0) {
                // Ignore the trigger container
                return@stage
            }
            stage.containers.forEach container@{ container ->
                if (container !is NormalContainer) {
                    // 只在无构建环境中
                    return@container
                }

                container.elements.forEach element@{ element ->
                    if (element !is SubPipelineCallElement) {
                        return@element
                    }
                    val subpipelineId = element.subPipelineId
                    if (subpipelineId.isBlank()) {
                        logger.warn("The sub pipeline id of pipeline($pipeline) is blank")
                        return@element
                    }
                    val exist = HashSet<String>(currentExistPipelines)
                    checkSubpipeline(projectId, subpipelineId, exist)
                    existPipelines.addAll(exist)
                }
            }
        }
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

    /**
     * 列出已经删除的流水线
     */
    fun listDeletePipelineIdByProject(projectId: String): List<PipelineInfo> {
        val result = pipelineInfoDao.listDeletePipelineIdByProject(dslContext, projectId)
        val list = mutableListOf<PipelineInfo>()
        result?.forEach {
            if (it != null)
                list.add(pipelineInfoDao.convert(it)!!)
        }
        return list
    }

    fun restorePipeline(projectId: String, pipelineId: String, userId: String, channelCode: ChannelCode): Model {
        val existModel = getModel(pipelineId) ?: throw NotFoundException("指定的流水线-模型不存在")
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            val pipeline = pipelineInfoDao.getPipelineInfo(transactionContext, projectId, pipelineId, null, true)
                ?: throw NotFoundException("要还原的流水线不存在，可能已经被删除或还原了")
            if (pipeline.channel != channelCode.name) {
                throw NotFoundException("指定编辑的流水线渠道来源${pipeline.channel}不符合${channelCode.name}")
            }

            pipelineInfoDao.restore(transactionContext, projectId, pipelineId, userId, channelCode)
            // 只初始化相关信息
            val tasks = initModel(existModel, projectId, pipelineId, userId, false, channelCode)
            pipelineModelTaskDao.batchSave(transactionContext, tasks)
        }
        return existModel
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRepositoryService::class.java)
    }
}
