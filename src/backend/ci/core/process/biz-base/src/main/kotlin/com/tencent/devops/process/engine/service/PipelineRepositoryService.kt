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
import com.tencent.devops.common.api.exception.DependNotFoundException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.MatrixPipelineInfo
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
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.PipelineResVersionDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.pojo.event.PipelineCreateEvent
import com.tencent.devops.process.engine.pojo.event.PipelineDeleteEvent
import com.tencent.devops.process.engine.pojo.event.PipelineRestoreEvent
import com.tencent.devops.process.engine.pojo.event.PipelineUpdateEvent
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.plugin.load.ElementBizRegistrar
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.pipeline.DeletePipelineResult
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.setting.PipelineModelVersion
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.process.service.PipelineOperationLogService
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
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.joda.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
    private val pipelineResDao: PipelineResDao,
    private val pipelineModelTaskDao: PipelineModelTaskDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineJobMutexGroupService: PipelineJobMutexGroupService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineResVersionDao: PipelineResVersionDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val versionConfigure: VersionConfigure,
    private val pipelineInfoExtService: PipelineInfoExtService,
    private val operationLogService: PipelineOperationLogService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
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
        useSubscriptionSettings: Boolean? = false,
        useLabelSettings: Boolean? = false,
        useConcurrencyGroup: Boolean? = false,
        templateId: String? = null,
        updateLastModifyUser: Boolean? = true,
        savedSetting: PipelineSetting? = null,
        saveDraft: Boolean? = false,
        description: String? = null
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
        // TODO 增加互转处理
        val yamlStr = null

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
        }

        return if (!create) {
            val pipelineSetting = savedSetting
                ?: pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)
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
                setting = pipelineSetting,
                updateLastModifyUser = updateLastModifyUser,
                saveDraft = saveDraft,
                description = description
            )
            operationLogService.addOperationLog(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = result.version,
                operationLogType = if (saveDraft == true) {
                    OperationLogType.UPDATE_DRAFT_VERSION
                } else {
                    OperationLogType.NORMAL_SAVE_OPERATION
                },
                params = result.versionName ?: "init",
                description = null
            )
            result
        } else {
            val result = create(
                projectId = projectId,
                pipelineId = pipelineId,
                model = model,
                yamlStr = yamlStr,
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
                saveDraft = saveDraft,
                description = description
            )
            operationLogService.addOperationLog(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = result.version,
                operationLogType = if (saveDraft == true) {
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
        yamlStr: String?,
        userId: String,
        channelCode: ChannelCode,
        canManualStartup: Boolean,
        canElementSkip: Boolean,
        buildNo: BuildNo?,
        modelTasks: Collection<PipelineModelTask>,
        useSubscriptionSettings: Boolean? = false,
        useLabelSettings: Boolean? = false,
        useConcurrencyGroup: Boolean? = false,
        templateId: String? = null,
        saveDraft: Boolean? = false,
        description: String?
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
                    id = id
                )
                model.latestVersion = modelVersion
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
                                pipelineAsCodeSettings = try {
                                    client.get(ServiceProjectResource::class).get(projectId).data
                                        ?.properties?.pipelineAsCodeSettings
                                } catch (ignore: Throwable) {
                                    logger.warn("[$projectId]|Failed to sync project|pipelineId=$pipelineId", ignore)
                                    null
                                },
                                settingVersion = settingVersion
                            )
                            pipelineSettingVersionDao.insertNewSetting(
                                dslContext = transactionContext,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                failNotifyTypes = notifyTypes,
                                id = client.get(ServiceAllocIdResource::class)
                                    .generateSegmentId(PIPELINE_SETTING_VERSION_BIZ_TAG_NAME).data,
                                settingVersion = settingVersion
                            )
                        }
                    } else {
                        pipelineSettingDao.updateSetting(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            name = model.name,
                            desc = model.desc ?: ""
                        )
                    }
                }
                // 如果不是草稿保存，最新版本永远是新增逻辑
                versionName = PipelineVersionUtils.getVersionName(
                    pipelineVersion, triggerVersion, settingVersion
                )
                if (saveDraft != true) pipelineResDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    creator = userId,
                    version = 1,
                    model = model,
                    versionName = versionName ?: "init",
                    pipelineVersion = modelVersion,
                    triggerVersion = triggerVersion,
                    settingVersion = settingVersion
                )
                // 同步记录到历史版本表
                pipelineResVersionDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    creator = userId,
                    version = 1,
                    model = model,
                    yaml = yamlStr,
                    versionName = versionName ?: "init",
                    pipelineVersion = modelVersion,
                    triggerVersion = triggerVersion,
                    settingVersion = settingVersion,
                    status = if (saveDraft == true) VersionStatus.COMMITTING else VersionStatus.RELEASED,
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
        saveDraft: Boolean? = false,
        description: String?
    ): DeployPipelineResult {
        val taskCount: Int = model.taskCount()
        var version = 0
        val lock = PipelineModelLock(redisOperation, pipelineId)
        val watcher = Watcher(id = "updatePipeline#$pipelineId#$saveDraft")
        var versionName: String? = null
        try {
            lock.lock()
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                watcher.start("updatePipelineInfo")
                version = if (updateLastModifyUser != null && updateLastModifyUser == false) {
                    pipelineInfoDao.update(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        userId = null,
                        updateVersion = true,
                        pipelineName = null,
                        pipelineDesc = null,
                        manualStartup = canManualStartup,
                        canElementSkip = canElementSkip,
                        taskCount = taskCount,
                        latestVersion = model.latestVersion
                    )
                } else {
                    pipelineInfoDao.update(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        userId = userId,
                        updateVersion = true,
                        pipelineName = null,
                        pipelineDesc = null,
                        manualStartup = canManualStartup,
                        canElementSkip = canElementSkip,
                        taskCount = taskCount,
                        latestVersion = model.latestVersion
                    )
                }
                if (version == 0) {
                    // 传过来的latestVersion已经不是最新
                    throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_IS_NOT_THE_LATEST)
                }
                model.latestVersion = version
                // 如果不是草稿保存，最新版本永远是新增逻辑
                watcher.start("getOriginModel")
                val latestResRecord = pipelineResDao.getLatestVersionRecord(
                    transactionContext, projectId, pipelineId
                )
                var pipelineVersion = latestResRecord?.pipelineVersion ?: version
                var triggerVersion = latestResRecord?.triggerVersion ?: version
                val settingVersion = setting?.version ?: latestResRecord?.settingVersion ?: version
                latestResRecord?.let {
                    val originModel = try {
                        objectMapper.readValue(it.model, Model::class.java)
                    } catch (ignored: Exception) {
                        logger.error("parse process($pipelineId) model fail", ignored)
                        null
                    } ?: return@let
                    pipelineVersion = PipelineVersionUtils.getPipelineVersion(
                        pipelineVersion, originModel, model
                    )
                    triggerVersion = PipelineVersionUtils.getTriggerVersion(
                        pipelineVersion, originModel, model
                    )
                }
                versionName = PipelineVersionUtils.getVersionName(
                    pipelineVersion, triggerVersion, settingVersion
                )
                watcher.start("updatePipelineResource")
                if (saveDraft != true) pipelineResDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    creator = userId,
                    version = version,
                    model = model,
                    versionName = versionName ?: "init",
                    pipelineVersion = pipelineVersion,
                    triggerVersion = triggerVersion,
                    settingVersion = settingVersion
                )
                pipelineResVersionDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    creator = userId,
                    version = version,
                    model = model,
                    yaml = yamlStr,
                    versionName = versionName ?: "init",
                    pipelineVersion = pipelineVersion,
                    triggerVersion = triggerVersion,
                    settingVersion = settingVersion,
                    status = if (saveDraft == true) VersionStatus.COMMITTING else VersionStatus.RELEASED,
                    description = description
                )
                // 针对新增version表做的数据迁移
                watcher.start("updatePipelineResourceVersion")
                if (version > 1 && pipelineResVersionDao.getVersionModelString(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        version = version - 1
                    ) == null
                ) {
                    // 当ResVersion表中缺失上一个有效版本时需从Res表迁移数据（版本间流水线模型对比有用）
                    val lastVersionModelStr = pipelineResDao.getVersionModelString(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        version = version - 1
                    )
                    if (!lastVersionModelStr.isNullOrEmpty()) {
                        pipelineResVersionDao.create(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            creator = userId,
                            version = version - 1,
                            modelStr = lastVersionModelStr,
                            yamlStr = yamlStr,
                            pipelineVersion = null,
                            triggerVersion = null,
                            settingVersion = null,
                            status = VersionStatus.RELEASED,
                            description = description
                        )
                    }
                }
                watcher.start("deleteEarlyVersion")
                pipelineModelTaskDao.deletePipelineTasks(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId
                )
                pipelineResDao.deleteEarlyVersion(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    beforeVersion = version
                )
                setting?.maxPipelineResNum?.let {
                    pipelineResVersionDao.deleteEarlyVersion(
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
        delete: Boolean? = false
    ): PipelineInfo? {
        val template = templatePipelineDao.get(dslContext, projectId, pipelineId)
        val templateId = template?.templateId
        return pipelineInfoDao.convert(
            t = pipelineInfoDao.getPipelineInfo(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                delete = delete
            ),
            templateId = templateId
        )
    }

    /**
     * 批量获取model
     */
    fun listModel(projectId: String, pipelineIds: Collection<String>): Map<String, Model?> {
        return pipelineResDao.listModelString(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        ).map { it.key to str2model(it.value, it.key) }.toMap()
    }

    fun getModel(
        projectId: String,
        pipelineId: String,
        version: Int? = null,
        includeDraft: Boolean? = false
    ): Model? {
        var modelString: String?
        if (version == null) { // 取最新版，直接从旧版本表读
            modelString = pipelineResDao.getVersionModelString(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = null
            ) ?: return null
        } else {
            modelString = pipelineResVersionDao.getVersionModelString(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                includeDraft = includeDraft
            )
            if (modelString.isNullOrBlank()) {
                // 兼容处理：取不到再从旧的版本表取
                modelString = pipelineResDao.getVersionModelString(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = version
                ) ?: return null
            }
        }
        return str2model(modelString, pipelineId)
    }

    fun getPipelineResourceVersion(
        projectId: String,
        pipelineId: String,
        version: Int? = null,
        includeDraft: Boolean? = false
    ): PipelineResourceVersion? {
        return if (version == null) { // 取最新版，直接从旧版本表读
            pipelineResDao.getLatestVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: return null
        } else {
            pipelineResVersionDao.getVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                includeDraft = includeDraft
            )
        }
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
                    pipelineResVersionDao.deleteAllVersion(transactionContext, projectId, pipelineId)
                    pipelineSettingVersionDao.deleteAllVersion(transactionContext, projectId, pipelineId)
                    pipelineResDao.deleteAllVersion(transactionContext, projectId, pipelineId)
                    pipelineSettingDao.delete(transactionContext, projectId, pipelineId)
                    templatePipelineDao.delete(transactionContext, projectId, pipelineId)
                    pipelineViewGroupDao.delete(transactionContext, projectId, pipelineId)
                } else {
                    // 删除前改名，防止名称占用
                    val deleteTime = LocalDateTime.now().toString("yyMMddHHmmSS")
                    var deleteName = "${record.pipelineName}[$deleteTime]"
                    if (deleteName.length > MAX_LEN_FOR_NAME) { // 超过截断，且用且珍惜
                        deleteName = deleteName.substring(0, MAX_LEN_FOR_NAME)
                    }

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

        val existModel = getModel(projectId, pipelineId, pipeline.version)

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
            pipelineInfoDao.update(
                dslContext = context,
                projectId = setting.projectId,
                pipelineId = setting.pipelineId,
                userId = userId,
                updateVersion = false,
                pipelineName = setting.pipelineName,
                pipelineDesc = setting.desc,
                updateLastModifyUser = updateLastModifyUser
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
            pipelineSettingDao.saveSetting(context, setting).toString()
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
                pipelineResDao.updatePipelineModel(dslContext, userId, pipelineModelVersion)
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
        val result = pipelineInfoDao.listDeletePipelineIdByProject(
            dslContext = dslContext,
            projectId = projectId,
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
        val existModel = getModel(projectId, pipelineId) ?: throw ErrorCodeException(
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
        val version = pipelineResDao.updateSettingVersion(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            settingVersion = settingVersion
        )
        // 同步刷新流水线版本历史中关联的设置版本号
        if (version != null) {
            pipelineResVersionDao.updateSettingVersion(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                settingVersion = settingVersion
            )
        }
    }
}
