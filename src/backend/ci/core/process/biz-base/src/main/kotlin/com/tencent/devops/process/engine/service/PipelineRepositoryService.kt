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

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.DependNotFoundException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.MatrixPipelineInfo
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_FIRST_STAGE_ENV_NOT_EMPTY
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.engine.cfg.VersionConfigure
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.lock.PipelineModelLock
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.pojo.event.PipelineCreateEvent
import com.tencent.devops.process.engine.pojo.event.PipelineDeleteEvent
import com.tencent.devops.process.engine.pojo.event.PipelineRestoreEvent
import com.tencent.devops.process.engine.pojo.event.PipelineUpdateEvent
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.plugin.load.ElementBizRegistrar
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.pipeline.DeletePipelineResult
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.pipeline.TemplateInfo
import com.tencent.devops.process.pojo.setting.PipelineModelVersion
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.process.service.PipelineOperationLogService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.util.NotifyTemplateUtils
import com.tencent.devops.process.utils.PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PipelineVersionUtils
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import javax.ws.rs.core.Response

@Suppress(
    "LongParameterList",
    "LargeClass",
    "TooManyFunctions",
    "LongMethod",
    "ReturnCount",
    "ComplexMethod",
    "ThrowsCount"
)
@Service
class PipelineRepositoryService constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val modelContainerIdGenerator: ModelContainerIdGenerator,
    private val pipelineIdGenerator: PipelineIdGenerator,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineResourceDao: PipelineResourceDao,
    private val pipelineModelTaskDao: PipelineModelTaskDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineJobMutexGroupService: PipelineJobMutexGroupService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val templatePipelineDao: TemplatePipelineDao,
    private val templateDao: TemplateDao,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val versionConfigure: VersionConfigure,
    private val pipelineInfoExtService: PipelineInfoExtService,
    private val operationLogService: PipelineOperationLogService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val transferService: PipelineTransferYamlService,
    private val redisOperation: RedisOperation
) {

    companion object {
        private const val MAX_LEN_FOR_NAME = 255
        private val logger = LoggerFactory.getLogger(PipelineRepositoryService::class.java)
        private const val PIPELINE_SETTING_VERSION_BIZ_TAG_NAME = "PIPELINE_SETTING_VERSION"

        @Suppress("ALL")
        fun PipelineSetting.checkParam() {
            if (maxPipelineResNum < 1) {
                throw InvalidParamException(
                    message = I18nUtil.getCodeLanMessage(ProcessMessageCode.PIPELINE_ORCHESTRATIONS_NUMBER_ILLEGAL),
                    params = arrayOf("maxPipelineResNum")
                )
            }
            if (runLockType == PipelineRunLockType.SINGLE ||
                runLockType == PipelineRunLockType.SINGLE_LOCK || runLockType == PipelineRunLockType.GROUP_LOCK
            ) {
                if (waitQueueTimeMinute < PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN ||
                    waitQueueTimeMinute > PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
                ) {
                    throw InvalidParamException(
                        I18nUtil.getCodeLanMessage(ProcessMessageCode.MAXIMUM_QUEUE_LENGTH_ILLEGAL),
                        params = arrayOf("waitQueueTimeMinute")
                    )
                }
                if (maxQueueSize < PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN ||
                    maxQueueSize > PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
                ) {
                    throw InvalidParamException(
                        I18nUtil.getCodeLanMessage(ProcessMessageCode.MAXIMUM_NUMBER_QUEUES_ILLEGAL),
                        params = arrayOf("maxQueueSize")
                    )
                }
            }
            if (maxConRunningQueueSize != null && (
                    this.maxConRunningQueueSize!! <= PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN ||
                        this.maxConRunningQueueSize!! > PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
                    )
            ) {
                throw InvalidParamException(
                    I18nUtil.getCodeLanMessage(ProcessMessageCode.MAXIMUM_NUMBER_CONCURRENCY_ILLEGAL),
                    params = arrayOf("maxConRunningQueueSize")
                )
            }
        }
    }

    fun deployPipeline(
        model: Model,
        projectId: String,
        signPipelineId: String?,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean,
        yamlStr: String? = null,
        baseVersion: Int? = null,
        useSubscriptionSettings: Boolean? = false,
        useLabelSettings: Boolean? = false,
        useConcurrencyGroup: Boolean? = false,
        templateId: String? = null,
        updateLastModifyUser: Boolean? = true,
        savedSetting: PipelineSetting? = null,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        branchName: String? = null,
        description: String? = null,
        pipelineAsCodeSettings: PipelineAsCodeSettings? = null
    ): DeployPipelineResult {

        // 生成流水线ID,新流水线以p-开头，以区分以前旧数据
        val pipelineId = signPipelineId ?: pipelineIdGenerator.getNextId()

        val modelTasks = initModel(
            model = model,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            create = create,
            channelCode = channelCode
        )

        val buildNo = (model.stages[0].containers[0] as TriggerContainer).buildNo
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        var canManualStartup = false
        var canElementSkip = false
        run lit@{
            triggerContainer.elements.forEach {
                if (it is ManualTriggerElement && it.isElementEnable()) {
                    canManualStartup = true
                    canElementSkip = it.canElementSkip ?: false
                    return@lit
                }
            }
            // 保存时将别名name补全为id
            triggerContainer.params.forEach { param ->
                param.name = param.name ?: param.id
            }
        }

        // 检查jobId长度，并打日志方便后续填充和报错
        model.stages.forEach { stage ->
            stage.containers.forEach { con ->
                if ((con.jobId?.length ?: 0) > 32) {
                    // TODO: 会在issue #9810 中改为在DefaultModelCheckPlugin中填充和限制jobId的逻辑，这里先打印统计日志
                    logger.warn("deployPipeline|#9810|$pipelineId|${con.jobId!!.length}")
                }
            }
        }

        return if (!create) {
            val pipelineSetting = savedSetting
                ?: pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)
            // 只在更新操作时检查stage数量不为1
            if (model.stages.size <= 1) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_WITH_EMPTY_STAGE,
                params = arrayOf()
            )
            val result = update(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                model = model,
                yamlStr = yamlStr,
                canManualStartup = canManualStartup,
                canElementSkip = canElementSkip,
                buildNo = buildNo,
                modelTasks = modelTasks,
                channelCode = channelCode,
                setting = pipelineSetting?.copy(pipelineAsCodeSettings = pipelineAsCodeSettings),
                updateLastModifyUser = updateLastModifyUser,
                versionStatus = versionStatus,
                branchName = branchName,
                description = description,
                baseVersion = baseVersion
            )
            result
        } else {
            val result = create(
                projectId = projectId,
                pipelineId = pipelineId,
                model = model,
                userId = userId,
                channelCode = channelCode,
                canManualStartup = canManualStartup,
                canElementSkip = canElementSkip,
                buildNo = buildNo,
                modelTasks = modelTasks,
                useSubscriptionSettings = useSubscriptionSettings,
                useLabelSettings = useLabelSettings,
                useConcurrencyGroup = useConcurrencyGroup,
                templateId = templateId,
                versionStatus = versionStatus,
                branchName = branchName,
                description = description,
                baseVersion = baseVersion,
                pipelineAsCodeSettings = pipelineAsCodeSettings
            )
            operationLogService.addOperationLog(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = result.version,
                operationLogType = if (versionStatus != VersionStatus.RELEASED) {
                    OperationLogType.CREATE_PIPELINE_AND_DRAFT
                } else {
                    OperationLogType.NORMAL_SAVE_OPERATION
                },
                params = result.versionName ?: "init",
                description = null
            )
            result
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
    ): List<PipelineModelTask> {

        val metaSize = modelCheckPlugin.checkModelIntegrity(model, projectId)
        // 去重id
        val distinctIdSet = HashSet<String>(metaSize, 1F /* loadFactor */)

        // 初始化ID 该构建环境下的ID,旧流水引擎数据无法转换为String，仍然是序号的方式
        val modelTasks = ArrayList<PipelineModelTask>(metaSize)
        // 初始化ID 该构建环境下的ID,旧流水引擎数据无法转换为String，仍然是序号的方式
        val containerSeqId = AtomicInteger(0)
        model.stages.forEachIndexed { index, s ->
            s.id = VMUtils.genStageId(index + 1)
            // #4531 对存量的stage审核数据做兼容处理
            s.resetBuildOption(true)
            s.timeCost = null
            if (index == 0) { // 在流程模型中初始化触发类容器
                initTriggerContainer(
                    stage = s,
                    containerSeqId = containerSeqId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    model = model,
                    userId = userId,
                    modelTasks = modelTasks,
                    channelCode = channelCode,
                    create = create,
                    distIds = distinctIdSet
                )
            } else {
                initOtherContainer(
                    stage = s,
                    projectId = projectId,
                    containerSeqId = containerSeqId,
                    userId = userId,
                    pipelineId = pipelineId,
                    model = model,
                    modelTasks = modelTasks,
                    channelCode = channelCode,
                    create = create,
                    distIds = distinctIdSet
                )
            }
        }

        return modelTasks
    }

    private fun initTriggerContainer(
        stage: Stage,
        containerSeqId: AtomicInteger,
        projectId: String,
        pipelineId: String,
        model: Model,
        userId: String,
        modelTasks: MutableList<PipelineModelTask>,
        channelCode: ChannelCode,
        create: Boolean,
        distIds: HashSet<String>
    ) {
        if (stage.containers.size != 1) {
            logger.warn("The trigger stage contain more than one container (${stage.containers.size})")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ILLEGAL_PIPELINE_MODEL_JSON
            )
        }
        val c = (
            stage.containers.getOrNull(0)
                ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB
                )
            ) as TriggerContainer

        // #4518 各个容器ID的初始化
        c.id = containerSeqId.get().toString()
        c.containerId = c.id
        if (c.containerHashId.isNullOrBlank() || distIds.contains(c.containerHashId)) {
            c.containerHashId = modelContainerIdGenerator.getNextId()
        }
        distIds.add(c.containerHashId!!)

        // 清理无用的options
        c.params = PipelineUtils.cleanOptions(c.params)

        var taskSeq = 0
        c.elements.forEach { e ->
            if (e.id.isNullOrBlank() || distIds.contains(e.id)) {
                e.id = modelTaskIdGenerator.getNextId()
            }
            distIds.add(e.id!!)
            ElementBizRegistrar.getPlugin(e)?.afterCreate(
                element = e,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = model.name,
                userId = userId,
                channelCode = channelCode,
                create = create,
                container = c
            )

            modelTasks.add(
                PipelineModelTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    stageId = stage.id!!,
                    containerId = c.id!!,
                    taskId = e.id!!,
                    taskSeq = ++taskSeq,
                    taskName = e.name,
                    atomCode = e.getAtomCode(),
                    atomVersion = e.version,
                    classType = e.getClassType(),
                    taskAtom = e.getTaskAtom(),
                    taskParams = e.genTaskParams(),
                    additionalOptions = e.additionalOptions
                )
            )
        }
    }

    private fun initOtherContainer(
        stage: Stage,
        projectId: String,
        containerSeqId: AtomicInteger,
        userId: String,
        pipelineId: String,
        model: Model,
        modelTasks: MutableList<PipelineModelTask>,
        channelCode: ChannelCode,
        create: Boolean,
        distIds: HashSet<String>
    ) {
        if (stage.containers.isEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB,
                params = arrayOf(
                    MessageUtil.getMessageByLocale(BK_FIRST_STAGE_ENV_NOT_EMPTY, I18nUtil.getLanguage(userId))
                )
            )
        }
        stage.containers.forEach { c ->

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

            var taskSeq = 0
            c.id = containerSeqId.incrementAndGet().toString()
            try {
                when {
                    c.matrixGroupFlag != true -> {
                        // c.matrixGroupFlag 不为 true 时 不需要做yaml检查
                    }

                    c is NormalContainer -> {
                        matrixYamlCheck(c.matrixControlOption)
                    }

                    c is VMBuildContainer -> {
                        matrixYamlCheck(c.matrixControlOption)
                    }
                }
            } catch (ignore: Exception) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_JOB_MATRIX_YAML_CONFIG_ERROR,
                    params = arrayOf(c.name, ignore.message ?: "")
                )
            }

            // #4518 Model中的containerId 和T_PIPELINE_BUILD_CONTAINER表的containerId保持一致，同为seq id
            c.id = containerSeqId.get().toString()
            c.containerId = c.id
            c.timeCost = null
            if (c.containerHashId.isNullOrBlank() || distIds.contains(c.containerHashId)) {
                c.containerHashId = modelContainerIdGenerator.getNextId()
            }
            distIds.add(c.containerHashId!!)
            c.elements.forEach { e ->
                if (e.id.isNullOrBlank() || distIds.contains(e.id)) {
                    e.id = modelTaskIdGenerator.getNextId()
                }
                e.timeCost = null
                distIds.add(e.id!!)
                when (e) {
                    is SubPipelineCallElement -> { // 子流水线循环依赖检查
                        val existPipelines = HashSet<String>()
                        existPipelines.add(pipelineId)
                        checkSubpipeline(projectId, e.subPipelineId, existPipelines)
                    }
                }

                // 补偿动作--未来拆分出来，针对复杂的东西异步处理
                ElementBizRegistrar.getPlugin(e)?.afterCreate(
                    element = e,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = model.name,
                    userId = userId,
                    channelCode = channelCode,
                    create = create,
                    container = c
                )

                modelTasks.add(
                    PipelineModelTask(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        stageId = stage.id!!,
                        containerId = c.id!!,
                        taskId = e.id!!,
                        taskSeq = ++taskSeq,
                        taskName = e.name,
                        atomCode = e.getAtomCode(),
                        atomVersion = e.version,
                        classType = e.getClassType(),
                        taskAtom = e.getTaskAtom(),
                        taskParams = e.genTaskParams(),
                        additionalOptions = e.additionalOptions
                    )
                )
            }
        }
    }

    private fun matrixYamlCheck(option: MatrixControlOption?) {
        if (option == null) throw DependNotFoundException("matrix option not found")
        if ((option.maxConcurrency ?: 0) > PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX) {
            throw InvalidParamException(
                "matrix maxConcurrency number(${option.maxConcurrency}) " +
                    "exceed $PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX /" +
                    "matrix maxConcurrency(${option.maxConcurrency}) " +
                    "is larger than $PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX"
            )
        }
        MatrixContextUtils.schemaCheck(
            JsonUtil.toJson(
                MatrixPipelineInfo(
                    include = option.includeCaseStr,
                    exclude = option.excludeCaseStr,
                    strategy = option.strategyStr
                ).toMatrixConvert()
            )
        )
    }

    private fun create(
        projectId: String,
        pipelineId: String,
        model: Model,
        userId: String,
        channelCode: ChannelCode,
        canManualStartup: Boolean,
        canElementSkip: Boolean,
        buildNo: BuildNo?,
        modelTasks: Collection<PipelineModelTask>,
        baseVersion: Int?,
        useSubscriptionSettings: Boolean? = false,
        useLabelSettings: Boolean? = false,
        useConcurrencyGroup: Boolean? = false,
        templateId: String? = null,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        branchName: String?,
        description: String?,
        pipelineAsCodeSettings: PipelineAsCodeSettings?
    ): DeployPipelineResult {
        val modelVersion = 1
        val pipelineVersion = 1
        val triggerVersion = 1
        val settingVersion = 1
        val taskCount: Int = model.taskCount()
        val id = client.get(ServiceAllocIdResource::class).generateSegmentId("PIPELINE_INFO").data
        val lock = PipelineModelLock(redisOperation, pipelineId)
        var versionName: String? = null
        try {
            lock.lock()
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineInfoDao.create(
                    dslContext = transactionContext,
                    pipelineId = pipelineId,
                    projectId = projectId,
                    version = modelVersion,
                    pipelineName = model.name,
                    pipelineDesc = model.desc ?: model.name,
                    userId = userId,
                    channelCode = channelCode,
                    manualStartup = canManualStartup,
                    canElementSkip = canElementSkip,
                    taskCount = taskCount,
                    id = id,
                    onlyDraft = versionStatus == VersionStatus.COMMITTING
                )
                model.latestVersion = modelVersion
                var savedSetting = PipelineSetting(
                    pipelineId = pipelineId,
                    pipelineName = model.name,
                    desc = model.desc ?: "",
                    pipelineAsCodeSettings = pipelineAsCodeSettings
                )

                if (model.instanceFromTemplate != true) {
                    if (null == pipelineSettingDao.getSetting(transactionContext, projectId, pipelineId)) {
                        if (templateId != null && (useSubscriptionSettings == true || useConcurrencyGroup == true)) {
                            // 沿用模板的配置
                            val setting = getSetting(projectId, templateId)
                                ?: throw ErrorCodeException(errorCode = ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS)
                            setting.pipelineId = pipelineId
                            setting.pipelineName = model.name
                            setting.version = settingVersion
                            if (useSubscriptionSettings != true) {
                                setting.successSubscription = Subscription(
                                    types = setOf(),
                                    groups = emptySet(),
                                    users = "\${$PIPELINE_START_USER_NAME}",
                                    content = NotifyTemplateUtils.getCommonShutdownSuccessContent()
                                )
                                setting.successSubscriptionList = listOf(setting.successSubscription)
                                setting.failSubscription = Subscription(
                                    types = setOf(PipelineSubscriptionType.EMAIL, PipelineSubscriptionType.RTX),
                                    groups = emptySet(),
                                    users = "\${$PIPELINE_START_USER_NAME}",
                                    content = NotifyTemplateUtils.getCommonShutdownFailureContent()
                                )
                                setting.failSubscriptionList = listOf(setting.failSubscription)
                            }
                            if (useConcurrencyGroup != true) {
                                setting.concurrencyGroup = null
                                setting.concurrencyCancelInProgress = false
                                setting.maxConRunningQueueSize = PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
                            }
                            if (useLabelSettings != true) {
                                setting.labels = listOf()
                            }
                            setting.pipelineAsCodeSettings = null
                            pipelineSettingDao.saveSetting(dslContext, setting)
                            savedSetting = setting
                        } else {
                            // #3311
                            // 蓝盾正常的BS渠道的默认没设置setting的，将发通知改成失败才发通知
                            // 而其他渠道的默认没设置则什么通知都设置为不发
                            val notifyTypes = if (channelCode == ChannelCode.BS) {
                                pipelineInfoExtService.failNotifyChannel()
                            } else {
                                ""
                            }

                            // 特定渠道保留特定版本
                            val filterList = versionConfigure.specChannels.split(",")
                            val maxPipelineResNum = if (channelCode.name in filterList) {
                                versionConfigure.specChannelMaxKeepNum
                            } else {
                                versionConfigure.maxKeepNum
                            }
                            pipelineSettingDao.insertNewSetting(
                                dslContext = transactionContext,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                pipelineName = model.name,
                                failNotifyTypes = notifyTypes,
                                maxPipelineResNum = maxPipelineResNum,
                                pipelineAsCodeSettings = pipelineAsCodeSettings,
                                settingVersion = settingVersion
                            )?.let { setting ->
                                pipelineSettingVersionDao.saveSetting(
                                    dslContext = transactionContext,
                                    setting = setting,
                                    id = client.get(ServiceAllocIdResource::class)
                                        .generateSegmentId(PIPELINE_SETTING_VERSION_BIZ_TAG_NAME).data,
                                    version = settingVersion
                                )
                                savedSetting = setting
                            }
                        }
                    } else {
                        pipelineSettingDao.updateSetting(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            name = model.name,
                            desc = model.desc ?: ""
                        )?.let { savedSetting = it }
                    }
                }
                // 如果不是草稿保存，最新版本永远是新增逻辑
                versionName = if (versionStatus == VersionStatus.BRANCH) {
                    branchName
                } else {
                    PipelineVersionUtils.getVersionName(pipelineVersion, triggerVersion, settingVersion)
                }
                val yamlStr = try {
                    transferService.transfer(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = null,
                        actionType = TransferActionType.FULL_MODEL2YAML,
                        data = TransferBody(
                            modelAndSetting = PipelineModelAndSetting(
                                model = model,
                                setting = savedSetting
                            )
                        )
                    ).newYaml ?: ""
                } catch (ignore: Throwable) {
                    // 旧流水线可能无法转换，用空YAML代替
                    logger.warn("TRANSFER_YAML|$projectId|$userId", ignore)
                    null
                }
                pipelineResourceDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    creator = userId,
                    version = 1,
                    model = model,
                    yamlStr = yamlStr,
                    versionName = versionName,
                    pipelineVersion = modelVersion,
                    triggerVersion = triggerVersion,
                    settingVersion = settingVersion
                )
                // 同步记录到历史版本表
                pipelineResourceVersionDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    creator = userId,
                    version = 1,
                    model = model,
                    yaml = yamlStr,
                    baseVersion = baseVersion,
                    versionName = versionName ?: "",
                    pipelineVersion = modelVersion,
                    triggerVersion = triggerVersion,
                    settingVersion = settingVersion,
                    versionStatus = versionStatus,
                    branchAction = if (versionStatus == VersionStatus.BRANCH) {
                        BranchVersionAction.ACTIVE
                    } else null,
                    description = description
                )
                // 初始化流水线构建统计表
                pipelineBuildSummaryDao.create(dslContext, projectId, pipelineId, buildNo)
                pipelineModelTaskDao.batchSave(transactionContext, modelTasks)
            }
        } finally {
            lock.unlock()
        }

        pipelineEventDispatcher.dispatch(
            PipelineCreateEvent(
                source = "create_pipeline",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildNo = buildNo
            ),
            PipelineModelAnalysisEvent(
                source = "create_pipeline",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                model = JsonUtil.toJson(model, formatted = false),
                channelCode = channelCode.name
            )
        )
        return DeployPipelineResult(pipelineId, pipelineName = model.name, version = 1, versionName)
    }

    private fun update(
        projectId: String,
        pipelineId: String,
        userId: String,
        model: Model,
        yamlStr: String?,
        canManualStartup: Boolean,
        canElementSkip: Boolean,
        buildNo: BuildNo?,
        modelTasks: Collection<PipelineModelTask>,
        channelCode: ChannelCode,
        setting: PipelineSetting? = null,
        updateLastModifyUser: Boolean? = true,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        baseVersion: Int?,
        branchName: String?,
        description: String?
    ): DeployPipelineResult {
        val taskCount: Int = model.taskCount()
        var version = 0
        val lock = PipelineModelLock(redisOperation, pipelineId)
        val watcher = Watcher(id = "updatePipeline#$pipelineId#$versionStatus")
        var versionName = ""
        var realBaseVersion = baseVersion
        var operationLogType = OperationLogType.NORMAL_SAVE_OPERATION
        var operationLogParams = versionName
        var branchAction: BranchVersionAction? = null

        try {
            lock.lock()
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                var pipelineVersion = 1
                var triggerVersion = 1
                var settingVersion = setting?.version ?: 1
                watcher.start("updatePipelineInfo")
                // 旧逻辑 bak —— 写入INFO表后进行了version的自动+1
                // 新逻辑 #8161
                when (versionStatus) {
                    // 1 草稿版本保存 —— 寻找当前草稿，存在则同版本更新，不存在则新建
                    VersionStatus.COMMITTING -> {
                        val draftVersion = pipelineResourceVersionDao.getDraftVersionResource(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId
                        )
                        version = if (draftVersion == null) {
                            val latestVersion = pipelineResourceVersionDao.getVersionResource(
                                dslContext = transactionContext,
                                projectId = projectId,
                                pipelineId = pipelineId
                            )
                            operationLogType = OperationLogType.CREATE_DRAFT_VERSION
                            operationLogParams = latestVersion?.versionName ?: latestVersion?.version.toString()
                            (latestVersion?.version ?: 0) + 1
                        } else {
                            operationLogType = OperationLogType.UPDATE_DRAFT_VERSION
                            draftVersion.version
                        }
                    }
                    // 2 分支版本保存 —— 取当前流水线的最新VERSION+1，不关心其他草稿和正式版本
                    VersionStatus.BRANCH -> {
                        // 查询同名分支的最新active版本，存在则更新，否则新增一个版本
                        branchName?.let { versionName = branchName }
                        val activeBranchVersion = pipelineResourceVersionDao.getVersionResource(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            branchName = branchName
                        )
                        if (activeBranchVersion != null) {
                            // 更新
                            operationLogType = OperationLogType.UPDATE_BRANCH_VERSION
                            operationLogParams = activeBranchVersion.versionName ?: activeBranchVersion.version.toString()
                            branchAction = BranchVersionAction.ACTIVE
                            version = activeBranchVersion.version
                        } else {
                            // 创建
                            val latestVersion = pipelineResourceVersionDao.getVersionResource(
                                dslContext = transactionContext,
                                projectId = projectId,
                                pipelineId = pipelineId
                            )
                            branchAction = BranchVersionAction.ACTIVE
                            operationLogType = OperationLogType.CREATE_BRANCH_VERSION
                            operationLogParams = versionName
                            version = (latestVersion?.version ?: 0) + 1
                        }
                    }
                    // 3 正式版本保存 —— 寻找当前草稿，存在草稿版本则报错，不存在则直接取最新VERSION+1，同时更新INFO、RESOURCE表
                    else -> {
                        watcher.start("getOriginModel")
                        val draftVersion = pipelineResourceVersionDao.getDraftVersionResource(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId
                        )
                        val releaseVersion = pipelineResourceVersionDao.getReleaseVersionRecord(
                            transactionContext, projectId, pipelineId
                        )
                        pipelineVersion = releaseVersion?.pipelineVersion ?: 1
                        triggerVersion = releaseVersion?.triggerVersion ?: 1
                        settingVersion = releaseVersion?.settingVersion ?: 1
                        releaseVersion?.let {
                            pipelineVersion = PipelineVersionUtils.getPipelineVersion(
                                pipelineVersion, it.model, model
                            )
                            triggerVersion = PipelineVersionUtils.getTriggerVersion(
                                triggerVersion, it.model, model
                            )
                        }
                        operationLogType = OperationLogType.RELEASE_MASTER_VERSION
                        val newVersionName = PipelineVersionUtils.getVersionName(
                            pipelineVersion, triggerVersion, settingVersion
                        )
                        versionName = newVersionName
                        operationLogParams = newVersionName
                        version = if (draftVersion == null || baseVersion == null) {
                            // 没有已有草稿或者旧接口保存时，直接增加正式版本，基准为上一个发布版本
                            val latestVersion = pipelineResourceVersionDao.getVersionResource(
                                dslContext = transactionContext,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                version = null
                            )
                            realBaseVersion = realBaseVersion ?: latestVersion?.version ?: 0
                            (latestVersion?.version ?: 0) + 1
                        } else {
                            if (draftVersion.baseVersion != baseVersion) throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_PIPELINE_IS_NOT_THE_LATEST
                            )
                            draftVersion.version
                        }

                        pipelineInfoDao.update(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            userId = if (updateLastModifyUser == false) null else userId,
                            version = version,
                            pipelineName = null,
                            pipelineDesc = null,
                            manualStartup = canManualStartup,
                            canElementSkip = canElementSkip,
                            taskCount = taskCount,
                            latestVersion = model.latestVersion,
                            onlyDraft = false
                        )
                        model.latestVersion = version
                    }
                }

                watcher.start("updatePipelineResource")
                if (versionStatus == VersionStatus.RELEASED) {
                    pipelineResourceDao.deleteEarlyVersion(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        beforeVersion = version
                    )
                    pipelineResourceDao.create(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        creator = userId,
                        version = version,
                        model = model,
                        yamlStr = yamlStr,
                        versionName = versionName,
                        pipelineVersion = pipelineVersion,
                        triggerVersion = triggerVersion,
                        settingVersion = settingVersion
                    )
                }
                // 对于新保存的版本如果没有指定基准版本则默认为上一个版本
                pipelineResourceVersionDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    creator = userId,
                    version = version,
                    model = model,
                    yaml = yamlStr,
                    versionName = versionName,
                    pipelineVersion = pipelineVersion,
                    triggerVersion = triggerVersion,
                    settingVersion = settingVersion,
                    versionStatus = versionStatus,
                    branchAction = branchAction,
                    description = description,
                    baseVersion = realBaseVersion ?: (version - 1)
                )
                // 此前先增后删是为了保证没有产生历史版本的数据能记录最后一个编排
                // 针对新增version表做的数据迁移，双写后已经不需要
//                watcher.start("updatePipelineResourceVersion")
//                val lastVersionRecord = pipelineResourceVersionDao.getVersionResource(
//                    dslContext = transactionContext,
//                    projectId = projectId,
//                    pipelineId = pipelineId,
//                    version = version - 1
//                )
//                if (version > 1 && lastVersionRecord == null) {
//                    // 当ResVersion表中缺失上一个有效版本时需从Res表迁移数据（版本间流水线模型对比有用）
//                    val lastVersionModelStr = pipelineResourceDao.getVersionModelString(
//                        dslContext = dslContext,
//                        projectId = projectId,
//                        pipelineId = pipelineId,
//                        version = version - 1
//                    )
//                    if (!lastVersionModelStr.isNullOrEmpty()) {
//                        pipelineResourceVersionDao.create(
//                            dslContext = transactionContext,
//                            projectId = projectId,
//                            pipelineId = pipelineId,
//                            creator = userId,
//                            version = version - 1,
//                            modelStr = lastVersionModelStr,
//                            yamlStr = yamlStr,
//                            versionName = null,
//                            pipelineVersion = null,
//                            triggerVersion = null,
//                            settingVersion = null,
//                            versionStatus = VersionStatus.RELEASED,
//                            description = description,
//                            baseVersion = (version - 1).coerceAtLeast(0)
//                        )
//                    }
//                }
                watcher.start("deleteEarlyVersion")
                pipelineModelTaskDao.deletePipelineTasks(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId
                )
                setting?.maxPipelineResNum?.let {
                    pipelineResourceVersionDao.deleteEarlyVersion(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        currentVersion = version,
                        maxPipelineResNum = it
                    )
                }
                pipelineModelTaskDao.batchSave(transactionContext, modelTasks)
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
            lock.unlock()
        }

        pipelineEventDispatcher.dispatch(
            PipelineUpdateEvent(
                source = "update_pipeline",
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                userId = userId,
                buildNo = buildNo
            ),
            PipelineModelAnalysisEvent(
                source = "update_pipeline",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                model = JsonUtil.toJson(model, formatted = false),
                channelCode = channelCode.name
            )
        )
        operationLogService.addOperationLog(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            operationLogType = operationLogType,
            params = operationLogParams,
            description = null
        )
        return DeployPipelineResult(
            pipelineId,
            pipelineName = model.name,
            version = version,
            versionName = versionName
        )
    }

    fun getPipelineInfo(
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode? = null,
        delete: Boolean? = false,
        queryDslContext: DSLContext? = null
    ): PipelineInfo? {
        val finalDslContext = queryDslContext ?: dslContext
        val template = templatePipelineDao.get(finalDslContext, projectId, pipelineId)
        val srcTemplate = template?.let { t ->
            templateDao.getTemplate(
                dslContext = finalDslContext, templateId = t.templateId
            )
        }
        val templateId = template?.templateId
        val info = pipelineInfoDao.convert(
            t = pipelineInfoDao.getPipelineInfo(
                dslContext = finalDslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                delete = delete
            ),
            templateId = templateId
        )
        if (info != null && srcTemplate != null) {
            info.templateInfo = TemplateInfo(
                templateId = template.templateId,
                templateName = srcTemplate.templateName,
                version = template.version,
                versionName = template.versionName,
                instanceType = PipelineInstanceTypeEnum.valueOf(template.instanceType)
            )
        }
        return info
    }

    /**
     * 批量获取model
     */
    fun listModel(projectId: String, pipelineIds: Collection<String>): Map<String, Model?> {
        return pipelineResourceDao.listModelString(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        ).map { it.key to str2model(it.value, it.key) }.toMap()
    }

    @Deprecated("废弃，改用getPipelineResourceVersion()，流水线多版本的信息需要一起获取")
    fun getModel(
        projectId: String,
        pipelineId: String,
        version: Int? = null,
        includeDraft: Boolean? = false
    ): Model? {
        return if (version == null) { // 取最新版，直接从旧版本表读
            val latestVersion = pipelineResourceDao.getLatestVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: pipelineResourceVersionDao.getDraftVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            latestVersion?.model
        } else {
            val targetVersion = pipelineResourceVersionDao.getVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                includeDraft = includeDraft
            ) ?: pipelineResourceDao.getLatestVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            targetVersion?.model
        }
    }

    fun getPipelineResourceVersion(
        projectId: String,
        pipelineId: String,
        version: Int? = null,
        includeDraft: Boolean? = false
    ): PipelineResourceVersion? {
        val resource = if (version == null) { // 取最新版，直接从旧版本表读
            includeDraft?.let {
                if (includeDraft) pipelineResourceVersionDao.getDraftVersionResource(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId
                ) else null
            } ?: pipelineResourceDao.getLatestVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
        } else {
            pipelineResourceVersionDao.getVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                includeDraft = includeDraft
            )
        }
        // 返回时将别名name补全为id
        resource?.let {
            (resource.model.stages[0].containers[0] as TriggerContainer).params.forEach { param ->
                param.name = param.name ?: param.id
            }
        }
        return resource
    }

    fun getDraftVersionResource(
        projectId: String,
        pipelineId: String
    ): PipelineResourceVersion? {
        val resource = pipelineResourceVersionDao.getDraftVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        // 返回时将别名name补全为id
        resource?.let {
            (resource.model.stages[0].containers[0] as TriggerContainer).params.forEach { param ->
                param.name = param.name ?: param.id
            }
        }
        return resource
    }

    fun rollbackDraftFromVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineResourceVersion {
        var resultVersion: PipelineResourceVersion? = null
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)

            // 获取最新的版本用于比较差异
            val latestVersion = pipelineResourceVersionDao.getVersionResource(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(version.toString())
            )

            // 获取目标的版本用于更新草稿
            val targetVersion = pipelineResourceVersionDao.getVersionResource(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            ) ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
                params = arrayOf(version.toString())
            )

            // 计算版本号
            val pipelineVersion = PipelineVersionUtils.getPipelineVersion(
                currVersion = latestVersion.pipelineVersion ?: 1,
                originModel = latestVersion.model,
                newModel = targetVersion.model
            )
            val triggerVersion = PipelineVersionUtils.getTriggerVersion(
                currVersion = latestVersion.pipelineVersion ?: 1,
                originModel = latestVersion.model,
                newModel = targetVersion.model
            )
            val now = LocalDateTime.now()
            val versionName = PipelineVersionUtils.getVersionName(
                pipelineVersion, triggerVersion, targetVersion.settingVersion
            )
            val newVersion = targetVersion.copy(
                version = latestVersion.version + 1,
                pipelineVersion = pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = targetVersion.settingVersion,
                createTime = now,
                updateTime = now,
                versionName = versionName
            )
            resultVersion = newVersion
            pipelineResourceVersionDao.clearDraftVersion(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId
            )
            pipelineResourceVersionDao.create(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                creator = userId,
                version = newVersion.version,
                versionName = versionName,
                model = newVersion.model,
                baseVersion = targetVersion.version,
                yaml = newVersion.yaml,
                pipelineVersion = newVersion.pipelineVersion,
                triggerVersion = triggerVersion,
                settingVersion = newVersion.settingVersion,
                versionStatus = VersionStatus.COMMITTING,
                branchAction = null,
                description = newVersion.description
            )
        }
        return resultVersion!!
    }

    private fun str2model(
        modelString: String,
        pipelineId: String
    ) = try {
        JsonUtil.to(modelString, Model::class.java)
    } catch (ignore: Exception) {
        logger.warn("get process($pipelineId) model fail", ignore)
        null
    }

    fun deletePipeline(
        projectId: String,
        pipelineId: String,
        userId: String,
        channelCode: ChannelCode?,
        delete: Boolean
    ): DeletePipelineResult {

        val record = pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId, channelCode)
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )

        val pipelineResult = DeletePipelineResult(pipelineId, record.pipelineName, record.version)
        val lock = PipelineModelLock(redisOperation, pipelineId)
        try {
            lock.lock()
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)

                if (delete) {
                    pipelineInfoDao.delete(transactionContext, projectId, pipelineId)
                    pipelineResourceVersionDao.deleteAllVersion(transactionContext, projectId, pipelineId)
                    pipelineSettingVersionDao.deleteAllVersion(transactionContext, projectId, pipelineId)
                    pipelineResourceDao.deleteAllVersion(transactionContext, projectId, pipelineId)
                    pipelineSettingDao.delete(transactionContext, projectId, pipelineId)
                    templatePipelineDao.delete(transactionContext, projectId, pipelineId)
                    pipelineViewGroupDao.delete(transactionContext, projectId, pipelineId)
                } else {
                    // 删除前改名，防止名称占用
                    val deleteTime = org.joda.time.LocalDateTime.now().toString("yyMMddHHmmSS")
                    var deleteName = "${record.pipelineName}[$deleteTime]"
                    if (deleteName.length > MAX_LEN_FOR_NAME) { // 超过截断，且用且珍惜
                        deleteName = deleteName.substring(0, MAX_LEN_FOR_NAME)
                    }
                    pipelineResourceVersionDao.clearDraftVersion(transactionContext, projectId, pipelineId)
                    pipelineResourceVersionDao.clearActiveBranchVersion(transactionContext, projectId, pipelineId)
                    pipelineInfoDao.softDelete(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        changePipelineName = deleteName,
                        userId = userId,
                        channelCode = channelCode
                    )
                    // 同时要对Setting中的name做设置
                    pipelineSettingDao.updateSetting(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        name = deleteName,
                        desc = "DELETE BY $userId in $deleteTime"
                    )
                    // #4201 标志关联模板为删除
                    templatePipelineDao.softDelete(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId
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
        } finally {
            lock.unlock()
        }

        return pipelineResult
    }

    fun isPipelineExist(
        projectId: String,
        pipelineName: String,
        channelCode: ChannelCode = ChannelCode.BS,
        excludePipelineId: String?
    ): Boolean {
        return pipelineInfoDao.isNameExist(dslContext, projectId, pipelineName, channelCode, excludePipelineId)
    }

    fun countByProjectIds(projectIds: Set<String>, channelCode: ChannelCode?): Int {
        return pipelineInfoDao.countByProjectIds(dslContext, projectIds, channelCode)
    }

    fun listPipelineNameByIds(
        projectId: String,
        pipelineIds: Set<String>,
        filterDelete: Boolean = true
    ): Map<String, String> {
        val listInfoByPipelineIds =
            pipelineInfoDao.listInfoByPipelineIds(
                dslContext = dslContext,
                projectId = projectId,
                pipelineIds = pipelineIds,
                filterDelete = filterDelete
            )
        return listInfoByPipelineIds.map {
            it.pipelineId to it.pipelineName
        }.toMap()
    }

    fun listPipelineIdByName(
        projectId: String,
        pipelineNames: Set<String>,
        filterDelete: Boolean = true
    ): Map<String, String> {

        if (pipelineNames.isEmpty() || projectId.isBlank()) return mapOf()

        val listInfoByPipelineName =
            pipelineInfoDao.listInfoByPipelineName(
                dslContext = dslContext,
                projectId = projectId,
                pipelineNames = pipelineNames,
                filterDelete = filterDelete
            )
        return listInfoByPipelineName.map {
            it.pipelineName to it.pipelineId
        }.toMap()
    }

    private fun checkSubpipeline(projectId: String, pipelineId: String, existPipelines: HashSet<String>) {

        if (existPipelines.contains(pipelineId)) {
            logger.info("[$projectId|$pipelineId] Sub pipeline call [$existPipelines|$pipelineId]")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_SUBPIPELINE_CYCLE_CALL
            )
        }
        existPipelines.add(pipelineId)
        val pipeline = getPipelineInfo(projectId, pipelineId)
        if (pipeline == null) {
            logger.warn("The sub pipeline($pipelineId) is not exist")
            return
        }

        val existModel = getPipelineResourceVersion(projectId, pipelineId, pipeline.version)?.model

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
                    checkSubpipeline(projectId = projectId, pipelineId = subpipelineId, existPipelines = exist)
                    existPipelines.addAll(exist)
                }
            }
        }
    }

    fun getBuildNo(projectId: String, pipelineId: String): Int? {
        return pipelineBuildSummaryDao.get(dslContext, projectId, pipelineId)?.buildNo
    }

    fun getSetting(projectId: String, pipelineId: String): PipelineSetting? {
        return pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)
    }

    fun saveSetting(
        userId: String,
        setting: PipelineSetting,
        version: Int,
        versionStatus: VersionStatus,
        updateLastModifyUser: Boolean? = true
    ): PipelineName {
        setting.checkParam()

        if (isPipelineExist(
                projectId = setting.projectId,
                excludePipelineId = setting.pipelineId,
                pipelineName = setting.pipelineName
            )
        ) {
            throw ErrorCodeException(
                statusCode = Response.Status.CONFLICT.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
            )
        }

        var oldName: String = setting.pipelineName
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val old = pipelineSettingDao.getSetting(
                dslContext = context,
                projectId = setting.projectId,
                pipelineId = setting.pipelineId
            )
            if (old?.pipelineName != null) {
                oldName = old.pipelineName
            }
            if (versionStatus == VersionStatus.RELEASED) pipelineInfoDao.update(
                dslContext = context,
                projectId = setting.projectId,
                pipelineId = setting.pipelineId,
                userId = userId,
                pipelineName = setting.pipelineName,
                pipelineDesc = setting.desc,
                updateLastModifyUser = updateLastModifyUser,
                onlyDraft = false
            )
            if (version > 0) { // #671 兼容无版本要求的修改入口，比如改名，或者只读流水线的修改操作, version=0
                if (old?.maxPipelineResNum != null) {
                    pipelineSettingVersionDao.deleteEarlyVersion(
                        dslContext = context,
                        projectId = setting.projectId,
                        pipelineId = setting.pipelineId,
                        currentVersion = version,
                        maxPipelineResNum = old.maxPipelineResNum
                    )
                }
                pipelineSettingVersionDao.saveSetting(
                    dslContext = context,
                    setting = setting,
                    version = version,
                    id = client.get(ServiceAllocIdResource::class).generateSegmentId(
                        PIPELINE_SETTING_VERSION_BIZ_TAG_NAME
                    ).data
                )
            }
            if (versionStatus == VersionStatus.RELEASED) pipelineSettingDao.saveSetting(context, setting).toString()
        }

        return PipelineName(name = setting.pipelineName, oldName = oldName)
    }

    fun batchUpdatePipelineModel(
        userId: String,
        pipelineModelVersionList: List<PipelineModelVersion>
    ) {
        pipelineModelVersionList.forEach { pipelineModelVersion ->
            val lock = PipelineModelLock(redisOperation, pipelineModelVersion.pipelineId)
            try {
                lock.lock()
                // 审计
                ActionAuditContext.current().addInstanceInfo(
                    pipelineModelVersion.pipelineId,
                    pipelineModelVersion.pipelineId,
                    null,
                    pipelineModelVersion.model
                )
                    .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, pipelineModelVersion.projectId)
                    .scopeId = pipelineModelVersion.projectId
                pipelineResourceDao.updatePipelineModel(dslContext, userId, pipelineModelVersion)
            } finally {
                lock.unlock()
            }
        }
    }

    /**
     * 列出已经删除的流水线
     */
    fun listDeletePipelineIdByProject(
        projectId: String,
        days: Long?,
        offset: Int? = null,
        limit: Int? = null,
        sortType: PipelineSortType,
        collation: PipelineCollation
    ): List<PipelineInfo> {
        val result = pipelineInfoDao.listPipelinesByProject(
            dslContext = dslContext,
            projectId = projectId,
            deleteFlag = true,
            days = days,
            offset = offset,
            limit = limit,
            sortType = sortType,
            collation = collation
        )
        val list = mutableListOf<PipelineInfo>()
        result?.forEach {
            if (it != null) {
                list.add(pipelineInfoDao.convert(it, null)!!)
            }
        }
        return list
    }

    @Suppress("ThrowsCount")
    fun restorePipeline(
        projectId: String,
        pipelineId: String,
        userId: String,
        channelCode: ChannelCode,
        days: Long?
    ): Model {
        val existModel = getPipelineResourceVersion(projectId, pipelineId)?.model ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            val pipeline = pipelineInfoDao.getPipelineInfo(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = null,
                delete = true,
                days = days
            ) ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_RESTORE_PIPELINE_NOT_FOUND
            )

            existModel.name = pipeline.pipelineName

            if (pipeline.channel != channelCode.name) {
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                    params = arrayOf(pipeline.channel)
                )
            }

            pipelineInfoDao.restore(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                channelCode = channelCode
            )

            // #4012 还原与模板的绑定关系
            templatePipelineDao.restore(dslContext = transactionContext, projectId = projectId, pipelineId = pipelineId)

            // 只初始化相关信息
            val tasks = initModel(
                model = existModel,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                create = false,
                channelCode = channelCode
            )
            pipelineModelTaskDao.batchSave(transactionContext, tasks)
        }

        val version = pipelineInfoDao.getPipelineVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            channelCode = channelCode
        )
        pipelineEventDispatcher.dispatch(
            PipelineRestoreEvent(
                source = "restore_pipeline",
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                userId = userId
            )
        )

        return existModel
    }

    fun countByPipelineIds(projectId: String, channelCode: ChannelCode, pipelineIds: List<String>): Int {
        return pipelineInfoDao.countByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = channelCode,
            pipelineIds = pipelineIds
        )
    }

    fun updateModelName(
        pipelineId: String,
        projectId: String,
        modelName: String,
        userId: String
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            // 1、update pipelineInf
            pipelineInfoDao.update(
                dslContext = transactionContext,
                pipelineId = pipelineId,
                projectId = projectId,
                pipelineName = modelName,
                userId = null
            )
            // 2、update settingName
            pipelineSettingDao.updateSettingName(
                dslContext = transactionContext,
                pipelineIdList = listOf(pipelineId),
                name = modelName
            )
        }
    }

    fun updateMaxConRunningQueueSize(
        userId: String,
        projectId: String,
        pipelineId: String,
        maxConRunningQueueSize: Int
    ): Int {
        return pipelineSettingDao.updateMaxConRunningQueueSize(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            maxConRunningQueueSize = maxConRunningQueueSize
        )
    }

    fun updateSettingVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        settingVersion: Int
    ) {
        val version = pipelineResourceDao.updateSettingVersion(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            settingVersion = settingVersion
        )
        // 同步刷新流水线版本历史中关联的设置版本号
        if (version != null) {
            pipelineResourceVersionDao.updateSettingVersion(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                settingVersion = settingVersion
            )
        }
    }

    fun updatePipelineBranchVersion(
        projectId: String,
        pipelineId: String,
        branchName: String?,
        branchVersionAction: BranchVersionAction
    ) {
        pipelineResourceVersionDao.updateBranchVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            branchName = branchName,
            branchVersionAction = branchVersionAction
        )
    }
}
