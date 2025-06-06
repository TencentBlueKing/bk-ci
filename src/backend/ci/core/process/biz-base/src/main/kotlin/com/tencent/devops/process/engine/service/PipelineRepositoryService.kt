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

import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.auth.api.service.ServiceAuthAuthorizationResource
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.DependNotFoundException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.dialect.IPipelineDialect
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.common.pipeline.event.PipelineCallbackEvent
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.MatrixPipelineInfo
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.YamlWithVersion
import com.tencent.devops.common.pipeline.utils.MatrixYamlCheckUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_FIRST_STAGE_ENV_NOT_EMPTY
import com.tencent.devops.process.dao.PipelineCallbackDao
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.engine.cfg.VersionConfigure
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.lock.PipelineModelLock
import com.tencent.devops.process.engine.control.lock.PipelineReleaseLock
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.dao.PipelineYamlInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.pojo.event.PipelineCreateEvent
import com.tencent.devops.process.engine.pojo.event.PipelineDeleteEvent
import com.tencent.devops.process.engine.pojo.event.PipelineRestoreEvent
import com.tencent.devops.process.engine.pojo.event.PipelineUpdateEvent
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.plugin.load.ElementBizRegistrar
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.pipeline.DeletePipelineResult
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.pojo.pipeline.TemplateInfo
import com.tencent.devops.process.pojo.setting.PipelineModelVersion
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.service.PipelineOperationLogService
import com.tencent.devops.process.service.pipeline.PipelineSettingVersionService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.utils.PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN
import com.tencent.devops.process.utils.PipelineVersionUtils
import com.tencent.devops.process.yaml.utils.NotifyTemplateUtils
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import jakarta.ws.rs.core.Response
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

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
    private val pipelineSettingVersionService: PipelineSettingVersionService,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val versionConfigure: VersionConfigure,
    private val pipelineInfoExtService: PipelineInfoExtService,
    private val operationLogService: PipelineOperationLogService,
    private val client: Client,
    private val transferService: PipelineTransferYamlService,
    private val redisOperation: RedisOperation,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val pipelineCallbackDao: PipelineCallbackDao,
    private val subPipelineTaskService: SubPipelineTaskService,
    private val pipelineInfoService: PipelineInfoService
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
                runLockType == PipelineRunLockType.SINGLE_LOCK ||
                runLockType == PipelineRunLockType.GROUP_LOCK ||
                runLockType == PipelineRunLockType.MULTIPLE
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

    @Value("\${project.callback.secretParam.aes-key:project_callback_aes_key}")
    private val aesKey = ""

    fun deployPipeline(
        model: Model,
        projectId: String,
        signPipelineId: String?,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean,
        yaml: YamlWithVersion? = null,
        baseVersion: Int? = null,
        useSubscriptionSettings: Boolean? = false,
        useConcurrencyGroup: Boolean? = false,
        templateId: String? = null,
        updateLastModifyUser: Boolean? = true,
        setting: PipelineSetting? = null,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        branchName: String? = null,
        description: String? = null,
        yamlInfo: PipelineYamlVo? = null,
        pipelineDisable: Boolean? = null
    ): DeployPipelineResult {

        // 生成流水线ID,新流水线以p-开头，以区分以前旧数据
        val pipelineId = signPipelineId ?: pipelineIdGenerator.getNextId()

        val pipelineSetting = if (!create) {
            setting ?: pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)
        } else {
            setting
        }
        val pipelineDialect = pipelineAsCodeService.getPipelineDialect(
            projectId = projectId,
            asCodeSettings = pipelineSetting?.pipelineAsCodeSettings
        )
        val modelTasks = initModel(
            model = model,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            create = create,
            versionStatus = versionStatus,
            channelCode = channelCode,
            yamlInfo = yamlInfo,
            pipelineDialect = pipelineDialect
        )
        val triggerContainer = model.getTriggerContainer()
        val buildNo = triggerContainer.buildNo?.apply {
            // #10958 每次存储model都需要忽略当前的推荐版本号值，在返回前端时重查
            currentBuildNo = null
        }
        var canManualStartup = false
        var canElementSkip = false
        run lit@{
            triggerContainer.elements.forEach {
                if (it is ManualTriggerElement && it.elementEnabled()) {
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

        return if (!create) {
            val result = update(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                model = model,
                yaml = yaml,
                canManualStartup = canManualStartup,
                canElementSkip = canElementSkip,
                buildNo = buildNo,
                modelTasks = modelTasks,
                channelCode = channelCode,
                setting = pipelineSetting,
                updateLastModifyUser = updateLastModifyUser,
                versionStatus = versionStatus,
                branchName = branchName,
                description = description,
                baseVersion = baseVersion,
                pipelineDisable = pipelineDisable
            )
            result
        } else {
            val result = JooqUtils.retryWhenDeadLock(3) {
                create(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    model = model,
                    customSetting = pipelineSetting,
                    yaml = yaml,
                    userId = userId,
                    channelCode = channelCode,
                    canManualStartup = canManualStartup,
                    canElementSkip = canElementSkip,
                    buildNo = buildNo,
                    modelTasks = modelTasks,
                    useSubscriptionSettings = useSubscriptionSettings,
                    useConcurrencyGroup = useConcurrencyGroup,
                    templateId = templateId,
                    versionStatus = versionStatus,
                    branchName = branchName,
                    description = description,
                    baseVersion = baseVersion,
                    pipelineDisable = pipelineDisable
                )
            }
            operationLogService.addOperationLog(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = result.version,
                operationLogType = OperationLogType.fetchType(versionStatus),
                params = result.versionName ?: PipelineVersionUtils.getVersionName(
                    result.version, result.version, 0, 0
                ) ?: "",
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
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        channelCode: ChannelCode,
        yamlInfo: PipelineYamlVo? = null,
        pipelineDialect: IPipelineDialect? = null
    ): List<PipelineModelTask> {
        val metaSize = modelCheckPlugin.checkModelIntegrity(
            model = model,
            projectId = projectId,
            userId = userId,
            oauthUser = getPipelineOauthUser(projectId, pipelineId),
            pipelineDialect = pipelineDialect,
            pipelineId = pipelineId
        )
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
                    distIds = distinctIdSet,
                    versionStatus = versionStatus,
                    yamlInfo = yamlInfo
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
                    distIds = distinctIdSet,
                    versionStatus = versionStatus,
                    yamlInfo = yamlInfo,
                    stageIndex = index
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
        distIds: HashSet<String>,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        yamlInfo: PipelineYamlVo?
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
            if (versionStatus?.isReleasing() == true) {
                ElementBizRegistrar.getPlugin(e)?.afterCreate(
                    element = e,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = model.name,
                    userId = userId,
                    channelCode = channelCode,
                    create = create,
                    container = c,
                    yamlInfo = yamlInfo
                )
            }

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
        distIds: HashSet<String>,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        yamlInfo: PipelineYamlVo?,
        stageIndex: Int
    ) {
        if (stage.containers.isEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB,
                params = arrayOf(
                    MessageUtil.getMessageByLocale(BK_FIRST_STAGE_ENV_NOT_EMPTY, I18nUtil.getLanguage(userId))
                )
            )
        }
        stage.containers.forEachIndexed { containerIndex, c ->

            if (c is TriggerContainer) {
                return@forEachIndexed
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
                // 补偿动作--未来拆分出来，针对复杂的东西异步处理
                if (versionStatus?.isReleasing() == true) {
                    ElementBizRegistrar.getPlugin(e)?.afterCreate(
                        element = e,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        pipelineName = model.name,
                        userId = userId,
                        channelCode = channelCode,
                        create = create,
                        container = c,
                        yamlInfo = yamlInfo
                    )
                }

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
                        additionalOptions = e.additionalOptions,
                        taskPosition = "$stageIndex-${containerIndex + 1}-$taskSeq",
                        containerEnable = c.containerEnabled(),
                        stageEnable = stage.stageEnabled()
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
        MatrixYamlCheckUtils.checkYaml(
            MatrixPipelineInfo(
                include = option.includeCaseStr,
                exclude = option.excludeCaseStr,
                strategy = option.strategyStr
            )
        )
    }

    /**
     * 初始化默认的流水线setting
     */
    fun createDefaultSetting(
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        channelCode: ChannelCode
    ): PipelineSetting {
        // 空白流水线设置初始化
        val maxPipelineResNum = if (
            channelCode.name in versionConfigure.specChannels.split(",")
        ) {
            versionConfigure.specChannelMaxKeepNum
        } else {
            versionConfigure.maxKeepNum
        }
        val notifyTypes = if (channelCode == ChannelCode.BS) {
            pipelineInfoExtService.failNotifyChannel()
        } else {
            ""
        }
        val failType = notifyTypes.split(",").filter { i -> i.isNotBlank() }
            .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
        val failSubscription = Subscription(
            types = failType,
            groups = emptySet(),
            users = "\${{ci.actor}}",
            content = NotifyTemplateUtils.getCommonShutdownFailureContent()
        ).takeIf { failType.isNotEmpty() }
        return PipelineSetting.defaultSetting(
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            maxPipelineResNum = maxPipelineResNum,
            failSubscription = failSubscription
        )
    }

    private fun create(
        projectId: String,
        pipelineId: String,
        model: Model,
        customSetting: PipelineSetting?,
        yaml: YamlWithVersion?,
        userId: String,
        channelCode: ChannelCode,
        canManualStartup: Boolean,
        canElementSkip: Boolean,
        buildNo: BuildNo?,
        modelTasks: Collection<PipelineModelTask>,
        baseVersion: Int?,
        useSubscriptionSettings: Boolean? = false,
        useConcurrencyGroup: Boolean? = false,
        templateId: String? = null,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        branchName: String?,
        description: String?,
        pipelineDisable: Boolean? = null
    ): DeployPipelineResult {
        // #8161 如果只有一个草稿版本的创建操作，流水线状态也为仅有草稿
        val modelVersion = 1
        var versionNum: Int? = 1
        var pipelineVersion: Int? = 1
        var triggerVersion: Int? = 1
        val settingVersion = 1
        // 如果是仅有草稿的状态，resource表的版本号先设为0作为基准
        if (versionStatus == VersionStatus.COMMITTING || versionStatus == VersionStatus.BRANCH) {
            versionNum = null
            pipelineVersion = null
            triggerVersion = null
        }
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
                    latestVersionStatus = versionStatus,
                    pipelineDisable = pipelineDisable
                )
                model.latestVersion = modelVersion
                var newSetting = customSetting?.copy(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = model.name,
                    desc = model.desc ?: ""
                ) ?: run {
                    createDefaultSetting(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        pipelineName = model.name,
                        channelCode = channelCode
                    )
                }

                if (model.instanceFromTemplate != true) {
                    if (null == pipelineSettingDao.getSetting(transactionContext, projectId, pipelineId)) {
                        if (useTemplateSettings(
                                templateId = templateId,
                                useSubscriptionSettings = useSubscriptionSettings,
                                useConcurrencyGroup = useConcurrencyGroup
                            )
                        ) {
                            // 沿用模板的配置
                            val setting = getSetting(projectId, templateId!!)
                                ?: throw ErrorCodeException(errorCode = ProcessMessageCode.PIPELINE_SETTING_NOT_EXISTS)
                            setting.version = settingVersion
                            if (useSubscriptionSettings == true) {
                                newSetting.copySubscriptionSettings(setting)
                            }
                            if (useConcurrencyGroup == true) {
                                newSetting.copyConcurrencyGroup(setting)
                            }
                        }
                        // 如果不需要覆盖模板内容，则直接保存传值或默认值
                        pipelineSettingDao.saveSetting(transactionContext, newSetting)
                        pipelineSettingVersionDao.saveSetting(
                            dslContext = transactionContext,
                            setting = newSetting,
                            id = client.get(ServiceAllocIdResource::class)
                                .generateSegmentId(PIPELINE_SETTING_VERSION_BIZ_TAG_NAME).data,
                            version = settingVersion
                        )
                    } else {
                        pipelineSettingDao.updateSetting(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            name = model.name,
                            desc = model.desc ?: ""
                        )?.let { setting ->
                            pipelineSettingVersionDao.saveSetting(
                                dslContext = transactionContext,
                                setting = setting,
                                id = client.get(ServiceAllocIdResource::class)
                                    .generateSegmentId(PIPELINE_SETTING_VERSION_BIZ_TAG_NAME)
                                    .data,
                                version = settingVersion
                            )
                            newSetting = setting
                        }
                    }
                }
                // 如果不是草稿保存，最新版本永远是新增逻辑
                versionName = if (versionStatus == VersionStatus.BRANCH) {
                    branchName
                } else {
                    PipelineVersionUtils.getVersionName(
                        versionNum, pipelineVersion, triggerVersion, settingVersion
                    )
                }

                pipelineResourceDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    creator = userId,
                    version = 1,
                    model = model,
                    yamlStr = yaml?.yamlStr,
                    yamlVersion = yaml?.versionTag,
                    versionName = versionName,
                    versionNum = versionNum,
                    pipelineVersion = pipelineVersion,
                    triggerVersion = triggerVersion,
                    settingVersion = settingVersion
                )
                // 同步记录到历史版本表
                pipelineResourceVersionDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    version = 1,
                    model = model,
                    yamlStr = yaml?.yamlStr,
                    yamlVersion = yaml?.versionTag,
                    baseVersion = baseVersion,
                    versionName = versionName ?: "",
                    versionNum = versionNum,
                    pipelineVersion = pipelineVersion,
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
                // 初始化流水线单体回调
                savePipelineCallback(
                    events = model.events,
                    pipelineId = pipelineId,
                    userId = userId,
                    projectId = projectId,
                    dslContext = transactionContext
                )
                // 初始化子流水线关联关系
                subPipelineTaskService.batchAdd(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    model = model,
                    channel = channelCode.name,
                    modelTasks = modelTasks.toList()
                )
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
        return DeployPipelineResult(
            pipelineId = pipelineId,
            pipelineName = model.name,
            version = 1,
            versionNum = versionNum,
            versionName = versionName
        )
    }

    private fun useTemplateSettings(
        templateId: String? = null,
        useSubscriptionSettings: Boolean? = false,
        useConcurrencyGroup: Boolean? = false
    ): Boolean {
        return templateId != null &&
                (useSubscriptionSettings == true || useConcurrencyGroup == true)
    }

    private fun update(
        projectId: String,
        pipelineId: String,
        userId: String,
        model: Model,
        yaml: YamlWithVersion?,
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
        description: String?,
        pipelineDisable: Boolean? = null
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
        var versionNum: Int? = null
        var updateBuildNo = false
        try {
            lock.lock()
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                var pipelineVersion: Int? = null
                var triggerVersion: Int? = null
                val settingVersion = (setting?.version ?: 1).coerceAtLeast(1)
                val releaseResource = pipelineResourceDao.getReleaseVersionResource(
                    transactionContext, projectId, pipelineId
                ) ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                    params = arrayOf(pipelineId)
                )
                model.latestVersion = releaseResource.version
                val latestVersion = getLatestVersionResource(
                    transactionContext = transactionContext, projectId = projectId,
                    pipelineId = pipelineId, userId = userId, releaseResource = releaseResource,
                    baseVersion = baseVersion
                )
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
                            // 创建
                            operationLogType = OperationLogType.CREATE_DRAFT_VERSION
                            realBaseVersion = realBaseVersion ?: releaseResource.version
                            operationLogParams = realBaseVersion?.let { base ->
                                pipelineResourceVersionDao.getPipelineVersionSimple(
                                    dslContext, projectId, pipelineId, base
                                )?.versionName
                            } ?: latestVersion.versionName ?: latestVersion.version.toString()
                            latestVersion.version + 1
                        } else {
                            // 更新
                            operationLogType = OperationLogType.UPDATE_DRAFT_VERSION
                            realBaseVersion = draftVersion.baseVersion
                            draftVersion.version
                        }
                        logger.info(
                            "PROCESS|updateDraft|$userId|$projectId|$pipelineId|version=$version|" +
                                "versionName=$versionName|operationLogType=$operationLogType|base=$realBaseVersion"
                        )
                    }
                    // 2 分支版本保存 —— 取当前流水线的最新VERSION+1，不关心其他草稿和正式版本
                    VersionStatus.BRANCH -> {
                        // 查询同名分支的最新active版本，存在则设为不活跃，永远新增一个版本
                        if (branchName.isNullOrBlank()) throw ErrorCodeException(
                            errorCode = CommonMessageCode.PARAMETER_VALIDATE_ERROR,
                            params = arrayOf("branchName")
                        )
                        versionName = branchName
                        val activeBranchVersion = pipelineResourceVersionDao.getBranchVersionResource(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            branchName = branchName
                        )
                        version = latestVersion.version + 1
                        branchAction = BranchVersionAction.ACTIVE
                        operationLogParams = versionName
                        if (activeBranchVersion == null) {
                            // 直接创建
                            operationLogType = OperationLogType.CREATE_BRANCH_VERSION
                        } else {
                            // 更新状态
                            operationLogType = OperationLogType.UPDATE_BRANCH_VERSION
                            pipelineResourceVersionDao.updateBranchVersion(
                                dslContext = transactionContext,
                                userId = userId,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                branchName = branchName,
                                branchVersionAction = BranchVersionAction.INACTIVE
                            )
                        }
                        logger.info(
                            "PROCESS|updateBranch|$userId|$projectId|$pipelineId|version=$version|" +
                                "versionName=$versionName|operationLogType=$operationLogType"
                        )
                    }
                    // 3 通过分支发布 —— 将要通过分支发布的草稿直接更新为分支版本
                    VersionStatus.BRANCH_RELEASE -> {
                        if (branchName.isNullOrBlank()) throw ErrorCodeException(
                            errorCode = CommonMessageCode.PARAMETER_VALIDATE_ERROR,
                            params = arrayOf("branchName")
                        )
                        // 将所有该发布分支的历史版本清理
                        val count = pipelineResourceVersionDao.clearActiveBranchVersion(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            branchName = branchName
                        )
                        val draftVersion = pipelineResourceVersionDao.getDraftVersionResource(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId
                        ) ?: throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_DRAFT_EXISTS
                        )
                        val pipelineInfo = getPipelineInfo(projectId = projectId, pipelineId = pipelineId)
                        // 将草稿版本发布成分支版本,需要把最新状态转换成分支版本
                        if (pipelineInfo != null &&
                            pipelineInfo.version == draftVersion.version &&
                            pipelineInfo.latestVersionStatus == VersionStatus.COMMITTING
                        ) {
                            pipelineInfoDao.update(
                                dslContext = transactionContext,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                userId = userId,
                                // 进行过至少一次发布版本后，取消仅有草稿/分支的状态
                                latestVersionStatus = VersionStatus.BRANCH
                            )
                        }
                        version = draftVersion.version
                        versionName = branchName
                        branchAction = BranchVersionAction.ACTIVE

                        operationLogParams = versionName
                        operationLogType = if (count > 0) {
                            OperationLogType.UPDATE_DRAFT_VERSION
                        } else {
                            OperationLogType.CREATE_BRANCH_VERSION
                        }
                        logger.info(
                            "PROCESS|releaseByBranch|$userId|$projectId|$pipelineId|version=$version|" +
                                "versionName=$versionName|operationLogType=$operationLogType"
                        )
                    }
                    // 4 正式版本保存 —— 寻找当前草稿，存在草稿版本则报错，不存在则直接取最新VERSION+1同时更新INFO、RESOURCE表
                    else -> {
                        watcher.start("getOriginModel")
                        val draftVersion = pipelineResourceVersionDao.getDraftVersionResource(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId
                        )
                        // 如果是仅有草稿的版本发布，则直接从0计算版本号
                        // 对于流水线版本的历史数据，至少取发布版本的版本号
                        // 数据分离：发布记录的版本自增，旧数据保留和版本表中version一致，后续单独用于前端展示
                        if (draftVersion?.version == releaseResource.version) {
                            pipelineVersion = 1
                            versionNum = 1
                        } else {
                            pipelineVersion = PipelineVersionUtils.getPipelineVersion(
                                currVersion = releaseResource.pipelineVersion ?: releaseResource.version,
                                originModel = releaseResource.model,
                                newModel = model
                            ).coerceAtLeast(1)
                            versionNum = (releaseResource.versionNum ?: releaseResource.version) + 1
                        }
                        triggerVersion = PipelineVersionUtils.getTriggerVersion(
                            currVersion = releaseResource.triggerVersion ?: 0,
                            originModel = releaseResource.model,
                            newModel = model
                        ).coerceAtLeast(1)
                        operationLogType = OperationLogType.RELEASE_MASTER_VERSION
                        val newVersionName = PipelineVersionUtils.getVersionName(
                            versionNum!!, pipelineVersion, triggerVersion, settingVersion
                        )
                        newVersionName?.let {
                            versionName = newVersionName
                            operationLogParams = newVersionName
                        }
                        version = if (versionStatus == VersionStatus.DRAFT_RELEASE && draftVersion != null) {
                            // 更新草稿版本为正式版本的草稿检查
                            if (draftVersion.baseVersion != baseVersion) throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_PIPELINE_IS_NOT_THE_LATEST
                            )
                            // 如果当前草稿和正式版本一致则拦截发布
                            if (
                                pipelineVersion == releaseResource.pipelineVersion &&
                                triggerVersion == releaseResource.triggerVersion &&
                                settingVersion == releaseResource.settingVersion
                            ) throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_VERSION_IS_NOT_UPDATED
                            )
                            draftVersion.model.getTriggerContainer().buildNo?.let {
                                val releaseBuildNo = releaseResource.model.getTriggerContainer().buildNo
                                // [关闭变为开启]或[修改buildNo数值]，都属于更新行为，需要提示更新
                                if (releaseBuildNo == null || releaseBuildNo.buildNo != it.buildNo) {
                                    updateBuildNo = true
                                }
                            }
                            draftVersion.version
                        } else {
                            // 兼容逻辑：没有已有草稿保存正式版本时，直接增加正式版本，基准为上一个发布版本
                            // 创建新版本记录
                            realBaseVersion = realBaseVersion ?: releaseResource.version
                            latestVersion.version + 1
                        }
                        logger.info(
                            "PROCESS|$userId|releasePipeline|$projectId|$pipelineId|" +
                                "version=$version|versionName=$versionName"
                        )
                        watcher.start("updatePipelineResource")
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
                            // 进行过至少一次发布版本后，取消仅有草稿/分支的状态
                            latestVersionStatus = VersionStatus.RELEASED,
                            locked = pipelineDisable
                        )
                        pipelineResourceDao.updateReleaseVersion(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            version = version,
                            model = model,
                            yamlStr = yaml?.yamlStr,
                            yamlVersion = yaml?.versionTag,
                            versionName = versionName,
                            versionNum = versionNum,
                            pipelineVersion = pipelineVersion,
                            triggerVersion = triggerVersion,
                            settingVersion = settingVersion
                        )
                        watcher.start("updatePipelineModelTask")
                        pipelineModelTaskDao.deletePipelineTasks(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId
                        )
                        pipelineModelTaskDao.batchSave(transactionContext, modelTasks)
                        // 保存流水线单体回调记录
                        savePipelineCallback(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            userId = userId,
                            events = model.events
                        )
                        watcher.start("updateSubPipelineRef")
                        // 批量保存,禁用态的插件需要被删除
                        subPipelineTaskService.batchAdd(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            model = model,
                            channel = channelCode.name,
                            modelTasks = modelTasks.toList()
                        )
                    }
                }

                watcher.start("updatePipelineResourceVersion")
                pipelineResourceVersionDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    version = version,
                    model = model,
                    yamlStr = yaml?.yamlStr,
                    yamlVersion = yaml?.versionTag,
                    versionName = versionName,
                    versionNum = versionNum,
                    pipelineVersion = pipelineVersion,
                    triggerVersion = triggerVersion,
                    settingVersion = settingVersion,
                    versionStatus = versionStatus?.fix(),
                    branchAction = branchAction,
                    description = description,
                    // 对于新保存的版本如果没有指定基准版本则默认为上一个版本
                    baseVersion = realBaseVersion ?: (version - 1)
                )
                watcher.start("deleteEarlyVersion")
                setting?.maxPipelineResNum?.let {
                    pipelineResourceVersionDao.deleteEarlyVersion(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        currentVersion = version,
                        maxPipelineResNum = it
                    )
                }
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
            lock.unlock()
        }

        // TODO 暂时只有正式发布的版本需要推送，等草稿历史出来后调整消费者再全推送
        if (versionStatus?.fix() == VersionStatus.RELEASED) pipelineEventDispatcher.dispatch(
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
            versionNum = versionNum,
            versionName = versionName,
            updateBuildNo = updateBuildNo
        )
    }

    private fun getLatestVersionResource(
        transactionContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String,
        releaseResource: PipelineResourceVersion,
        baseVersion: Int?
    ): PipelineResourceVersion {
        val latestVersion = pipelineResourceVersionDao.getLatestVersionResource(
            dslContext = transactionContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: run {
            // #8161 没有版本表数据的流水线，兜底增加一个当前版本
            pipelineResourceVersionDao.create(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                version = releaseResource.version,
                model = releaseResource.model,
                yamlVersion = releaseResource.yamlVersion,
                yamlStr = releaseResource.yaml,
                baseVersion = baseVersion,
                versionName = releaseResource.versionName ?: "init",
                versionNum = releaseResource.versionNum,
                pipelineVersion = releaseResource.version,
                triggerVersion = null,
                // 不写入版本状态和关联的setting版本，标识是兼容非常老的数据补全
                settingVersion = null,
                versionStatus = null,
                branchAction = null,
                description = "backup"
            )
            releaseResource
        }
        return latestVersion
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

    /**
     * 获取所有本次构建触发需要的信息
     * 并在获取过程中增加并发锁，防止流水线发布版本期间触发读取脏数据
     */
    fun getBuildTriggerInfo(
        projectId: String,
        pipelineId: String,
        version: Int?
    ): Triple<PipelineInfo, PipelineResourceVersion, Boolean> {
        PipelineReleaseLock(redisOperation, pipelineId).use {
            val pipelineInfo = getPipelineInfo(
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
            if (version == null) {
                val defaultVersion = getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId
                ) ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
                    params = arrayOf(pipelineId)
                )
                // 正式执行时，当前最新版本可能是草稿，则作为调试执行
                return Triple(
                    pipelineInfo,
                    defaultVersion,
                    defaultVersion.status == VersionStatus.COMMITTING
                )
            } else {
                val targetResource = getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = version
                ) ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
                    params = arrayOf(version.toString())
                )
                return when (targetResource.status) {
                    VersionStatus.COMMITTING -> {
                        Triple(pipelineInfo, targetResource, true)
                    }
                    else -> {
                        Triple(pipelineInfo, targetResource, false)
                    }
                }
            }
        }
    }

    /**
     * 批量获取model
     */
    fun getModel(projectId: String, pipelineId: String): Model? {
        return pipelineResourceDao.getLatestVersionModelString(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.let { str2model(it, pipelineId) }
    }

    /**
     * 获取编排版本的通用方法
     * 1 如果指定了[version]则一定按照version号查询版本
     * 2 如果没有指定版本，则通过[includeDraft]控制是否过滤掉草稿，获得最新版本流水线
     * 3 默认情况，[version]=null 且 [includeDraft]=false 时，直接返回当前正式版本
     */
    fun getPipelineResourceVersion(
        projectId: String,
        pipelineId: String,
        version: Int? = null,
        includeDraft: Boolean? = false,
        queryDslContext: DSLContext? = null,
        encryptedFlag: Boolean? = false
    ): PipelineResourceVersion? {
        // TODO 取不到则直接从旧版本表读，待下架
        val resource = if (version == null) {
            if (includeDraft == true) pipelineResourceVersionDao.getDraftVersionResource(
                dslContext = queryDslContext ?: dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            ) else null
        } else {
            pipelineResourceVersionDao.getVersionResource(
                dslContext = queryDslContext ?: dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                includeDraft = includeDraft
            )
        } ?: pipelineResourceDao.getReleaseVersionResource(
            dslContext = queryDslContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        // 历史数据兼容：
        // 1 返回时将别名name补全为id
        // 2 填充所有job没有的job id
        // 3 所有插件ENV配置合并历史值，并过滤掉默认值
        var randomSeed = 1
        val jobIdSet = mutableSetOf<String>()
        val elementSensitiveParamInfos = if (encryptedFlag == true && resource?.model != null) {
            AtomUtils.getModelElementSensitiveParamInfos(projectId, resource.model, client)
        } else {
            null
        }
        resource?.model?.stages?.forEachIndexed { index, s ->
            if (index == 0) (s.containers[0] as TriggerContainer).params.forEach { param ->
                param.name = param.name ?: param.id
            } else {
                s.containers.forEach { c ->
                    if (c.jobId.isNullOrBlank()) c.jobId = VMUtils.getContainerJobId(randomSeed++, jobIdSet)
                    c.jobId?.let { jobIdSet.add(it) }
                    c.elements.forEach { e ->
                        // 保存时将旧customEnv赋值给新的上一级customEnv
                        val oldCustomEnv = e.additionalOptions?.customEnv?.filter {
                            !(it.key == "param1" && it.value == "")
                        }
                        if (!oldCustomEnv.isNullOrEmpty()) {
                            e.customEnv = (e.customEnv ?: emptyList()).plus(oldCustomEnv)
                        }
                        e.additionalOptions?.customEnv = null
                        elementSensitiveParamInfos?.let {
                            pipelineInfoService.transferSensitiveParam(e, elementSensitiveParamInfos)
                        }
                    }
                }
            }
        }
        pipelineCallbackDao.list(
            dslContext = queryDslContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            event = null
        ).let { records ->
            if (records.isNotEmpty) {
                // 填充流水线级别回调
                resource?.model?.events = records.associate {
                    it.name to PipelineCallbackEvent(
                        callbackEvent = CallBackEvent.valueOf(it.eventType),
                        callbackUrl = it.url,
                        secretToken = it.secretToken?.let { AESUtil.decrypt(aesKey, it) },
                        region = CallBackNetWorkRegionType.valueOf(it.region),
                        callbackName = it.name
                    )
                }
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
            resource.model.getTriggerContainer().params.forEach { param ->
                param.name = param.name ?: param.id
            }
        }
        return resource
    }

    fun getBranchVersionResource(
        projectId: String,
        pipelineId: String,
        branchName: String?
    ): PipelineResourceVersion? {
        val resource = pipelineResourceVersionDao.getBranchVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            branchName = branchName
        )
        // 返回时将别名name补全为id
        resource?.let {
            resource.model.getTriggerContainer().params.forEach { param ->
                param.name = param.name ?: param.id
            }
        }
        return resource
    }

    fun getActiveBranchVersionCount(
        projectId: String,
        pipelineId: String,
        queryDslContext: DSLContext? = null
    ): Int {
        return pipelineResourceVersionDao.getActiveBranchVersionCount(
            dslContext = queryDslContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun rollbackDraftFromVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        targetVersion: PipelineResourceVersion,
        ignoreBase: Boolean? = false,
        transactionContext: DSLContext? = null
    ): PipelineResourceVersion {
        var resultVersion: PipelineResourceVersion? = null
        dslContext.transaction { configuration ->
            val context = transactionContext ?: DSL.using(configuration)

            // 获取发布的版本用于比较差异
            val releaseResource = pipelineResourceDao.getReleaseVersionResource(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
            // 删除草稿并获取最新版本用于版本号计算
            pipelineResourceVersionDao.clearDraftVersion(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId
            )
            val latestResource = pipelineResourceVersionDao.getLatestVersionResource(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: releaseResource

            // 计算版本号
            val settingVersion = (latestResource.settingVersion ?: latestResource.version) + 1
            val now = LocalDateTime.now()
            val newDraft = targetVersion.copy(
                version = latestResource.version + 1,
                versionNum = null,
                pipelineVersion = null,
                triggerVersion = null,
                versionName = null,
                settingVersion = settingVersion,
                createTime = now,
                updateTime = now
            )
            resultVersion = newDraft
            pipelineResourceVersionDao.create(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                version = newDraft.version,
                versionName = "",
                model = newDraft.model,
                baseVersion = targetVersion.version.takeIf { ignoreBase != true },
                yamlStr = newDraft.yaml,
                yamlVersion = newDraft.yamlVersion,
                versionNum = null,
                pipelineVersion = newDraft.pipelineVersion,
                triggerVersion = newDraft.triggerVersion,
                settingVersion = newDraft.settingVersion,
                versionStatus = VersionStatus.COMMITTING,
                branchAction = null,
                description = newDraft.description
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
        val pipelineVersionSimple = pipelineResourceVersionDao.getPipelineVersionSimple(
            dslContext, projectId, pipelineId, record.version
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
                    // 同时要对对应setting version中的name做设置,不然恢复时流水线详情展示的名称不对
                    pipelineVersionSimple?.settingVersion?.let {
                        pipelineSettingVersionDao.updateSetting(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            version = it,
                            name = deleteName,
                            desc = "DELETE BY $userId in $deleteTime"
                        )
                    }

                    // #4201 标志关联模板为删除
                    templatePipelineDao.softDelete(
                        dslContext = transactionContext,
                        projectId = projectId,
                        pipelineId = pipelineId
                    )
                }

                pipelineModelTaskDao.deletePipelineTasks(transactionContext, projectId, pipelineId)
                pipelineYamlInfoDao.deleteByPipelineId(transactionContext, projectId, pipelineId)
                subPipelineTaskService.batchDelete(transactionContext, projectId, pipelineId)
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

    fun getBuildNo(projectId: String, pipelineId: String): Int? {
        return pipelineBuildSummaryDao.get(dslContext, projectId, pipelineId)?.buildNo
    }

    fun getSetting(projectId: String, pipelineId: String): PipelineSetting? {
        return pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)
    }

    fun getSettingByPipelineVersion(
        projectId: String,
        pipelineId: String,
        pipelineVersion: Int?
    ): PipelineSetting? {
        val resource = pipelineVersion?.let {
            pipelineResourceVersionDao.getPipelineVersionSimple(
                dslContext, projectId, pipelineId, pipelineVersion
            )
        }
        return resource?.settingVersion?.let {
            pipelineSettingVersionService.getPipelineSetting(
                projectId = projectId,
                pipelineId = pipelineId,
                userId = null,
                detailInfo = null,
                version = it
            )
        } ?: pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)
    }

    fun saveSetting(
        context: DSLContext? = null,
        userId: String,
        setting: PipelineSetting,
        version: Int,
        versionStatus: VersionStatus,
        updateLastModifyUser: Boolean? = true,
        isTemplate: Boolean = false
    ): PipelineName {
        setting.checkParam()

        if (!isTemplate && isPipelineExist(
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

        return JooqUtils.retryWhenDeadLock(3) {
            transactionSaveSetting(
                context = context,
                setting = setting,
                versionStatus = versionStatus,
                userId = userId,
                updateLastModifyUser = updateLastModifyUser,
                version = version,
                isTemplate = isTemplate
            )
        }
    }

    private fun transactionSaveSetting(
        context: DSLContext?,
        setting: PipelineSetting,
        versionStatus: VersionStatus,
        userId: String,
        updateLastModifyUser: Boolean?,
        version: Int,
        isTemplate: Boolean
    ): PipelineName {
        var oldName: String = setting.pipelineName
        setting.pipelineAsCodeSettings?.resetDialect()
        (context ?: dslContext).transaction { t ->
            val transactionContext = DSL.using(t)
            val old = pipelineSettingDao.getSetting(
                dslContext = transactionContext,
                projectId = setting.projectId,
                pipelineId = setting.pipelineId
            )
            if (old?.pipelineName != null) {
                oldName = old.pipelineName
            }
            if (!isTemplate && versionStatus.isReleasing()) pipelineInfoDao.update(
                dslContext = transactionContext,
                projectId = setting.projectId,
                pipelineId = setting.pipelineId,
                userId = userId,
                pipelineName = setting.pipelineName,
                pipelineDesc = setting.desc,
                updateLastModifyUser = updateLastModifyUser,
                // 单独修改流水线配置不影响版本状态
                latestVersionStatus = null
            )
            if (version > 0) { // #671 兼容无版本要求的修改入口，比如改名，或者只读流水线的修改操作, version=0
                if (old?.maxPipelineResNum != null) {
                    pipelineSettingVersionDao.deleteEarlyVersion(
                        dslContext = transactionContext,
                        projectId = setting.projectId,
                        pipelineId = setting.pipelineId,
                        currentVersion = version,
                        maxPipelineResNum = old.maxPipelineResNum
                    )
                }
                pipelineSettingVersionDao.saveSetting(
                    dslContext = transactionContext,
                    setting = setting,
                    version = version,
                    isTemplate = isTemplate,
                    id = client.get(ServiceAllocIdResource::class).generateSegmentId(
                        PIPELINE_SETTING_VERSION_BIZ_TAG_NAME
                    ).data
                )
            }
            if (versionStatus.isReleasing()) {
                pipelineSettingDao.saveSetting(
                    transactionContext, setting, isTemplate
                )
            }
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
                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    pipelineResourceDao.updatePipelineModel(transactionContext, userId, pipelineModelVersion)
                    pipelineResourceVersionDao.updatePipelineModel(transactionContext, userId, pipelineModelVersion)
                }
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
        collation: PipelineCollation,
        filterByPipelineName: String?
    ): List<PipelineInfo> {
        val result = pipelineInfoDao.listPipelinesByProject(
            dslContext = dslContext,
            projectId = projectId,
            deleteFlag = true,
            days = days,
            offset = offset,
            limit = limit,
            sortType = sortType,
            collation = collation,
            filterByPipelineName = filterByPipelineName
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
    ): PipelineResourceVersion {
        val existResource = getPipelineResourceVersion(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )
        val existModel = existResource.model
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

            if (pipeline.channel != channelCode.name) {
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_CHANNEL_CODE,
                    params = arrayOf(pipeline.channel)
                )
            }

            // 如果流水线名称已经重复,则使用含有日期的流水线名,否则使用原来的流水线名
            val existPipelineName = isPipelineExist(
                projectId = projectId,
                pipelineName = existModel.name,
                channelCode = channelCode,
                excludePipelineId = pipelineId
            )
            val pipelineName = if (existPipelineName) {
                existModel.name = pipeline.pipelineName
                pipeline.pipelineName
            } else {
                existModel.name
            }

            pipelineInfoDao.restore(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                userId = userId,
                channelCode = channelCode
            )
            val restoreTime = org.joda.time.LocalDateTime.now().toString("yyMMddHHmmSS")
            // 恢复setting中的名称和描述
            pipelineSettingDao.updateSetting(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                name = pipelineName,
                desc = "RESTORE BY $userId in $restoreTime"
            )
            // 恢复对应setting version中的流水线名称和描述
            existResource.settingVersion?.let {
                pipelineSettingVersionDao.updateSetting(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = it,
                    name = pipelineName,
                    desc = "RESTORE BY $userId in $restoreTime"
                )
            }

            // #4012 还原与模板的绑定关系
            templatePipelineDao.restore(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )

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
            subPipelineTaskService.batchAdd(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                model = existModel,
                channel = channelCode.name,
                modelTasks = tasks
            )
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

        return existResource
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
        savedSetting: PipelineSetting,
        updateLastModifyUser: Boolean?
    ): DeployPipelineResult {
        val lock = PipelineModelLock(redisOperation, pipelineId)
        val watcher = Watcher(id = "updateSettingVersion#$pipelineId#${savedSetting.version}")
        var pipelineName = ""
        var version = 0
        var versionNum = 0
        var versionName = ""
        try {
            lock.lock()
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                watcher.start("getOriginModel")
                val releaseResource = pipelineResourceDao.getReleaseVersionResource(
                    transactionContext, projectId, pipelineId
                ) ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                    params = arrayOf(pipelineId)
                )
                val latestVersion = getLatestVersionResource(
                    transactionContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    releaseResource = releaseResource,
                    baseVersion = releaseResource.version
                )
                pipelineName = savedSetting.pipelineName
                version = latestVersion.version + 1
                versionNum = (releaseResource.versionNum ?: releaseResource.version) + 1
                versionName = PipelineVersionUtils.getVersionName(
                    versionNum, releaseResource.pipelineVersion,
                    releaseResource.triggerVersion, savedSetting.version
                ) ?: ""
                logger.info(
                    "PROCESS|updateSettingVersion|version=$version|" +
                            "versionNum=$versionNum|versionName=$versionName"
                )
                val newModel = releaseResource.model.copy(
                    name = savedSetting.pipelineName, desc = savedSetting.desc
                )
                // 用新的流水线名称、描述和旧yaml的格式生成新的yaml
                val yamlWithVersion = try {
                    transferService.transfer(
                        userId, projectId, pipelineId, TransferActionType.FULL_MODEL2YAML,
                        TransferBody(
                            PipelineModelAndSetting(newModel, savedSetting),
                            releaseResource.yaml ?: ""
                        )
                    ).yamlWithVersion
                } catch (ignore: Throwable) {
                    logger.warn("TRANSFER_YAML_SETTING|$projectId|$userId|$pipelineId", ignore)
                    null
                }
                watcher.start("updatePipelineInfo")
                pipelineInfoDao.update(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    version = version,
                    updateLastModifyUser = updateLastModifyUser
                )
                watcher.start("updatePipelineResource")
                pipelineResourceDao.updateReleaseVersion(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = version,
                    model = newModel,
                    yamlStr = yamlWithVersion?.yamlStr,
                    yamlVersion = yamlWithVersion?.versionTag,
                    versionName = versionName,
                    versionNum = versionNum,
                    pipelineVersion = releaseResource.pipelineVersion,
                    triggerVersion = releaseResource.triggerVersion,
                    settingVersion = savedSetting.version
                )
                watcher.start("updatePipelineResourceVersion")
                pipelineResourceVersionDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    version = version,
                    model = newModel,
                    yamlStr = yamlWithVersion?.yamlStr,
                    yamlVersion = yamlWithVersion?.versionTag,
                    versionName = versionName,
                    versionNum = versionNum,
                    pipelineVersion = releaseResource.pipelineVersion,
                    triggerVersion = releaseResource.triggerVersion,
                    settingVersion = savedSetting.version,
                    versionStatus = VersionStatus.RELEASED,
                    branchAction = releaseResource.branchAction,
                    description = releaseResource.description,
                    // 对于新保存的版本如果没有指定基准版本则默认为上一个版本
                    baseVersion = releaseResource.baseVersion
                )
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
            lock.unlock()
        }
        return DeployPipelineResult(
            pipelineId,
            pipelineName = pipelineName,
            version = version,
            versionNum = versionNum,
            versionName = versionName
        )
    }

    fun updatePipelineBranchVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        branchName: String?,
        branchVersionAction: BranchVersionAction,
        transactionContext: DSLContext? = null
    ) {
        pipelineResourceVersionDao.updateBranchVersion(
            dslContext = transactionContext ?: dslContext,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            branchName = branchName,
            branchVersionAction = branchVersionAction
        )
    }

    fun updateLocked(
        userId: String,
        projectId: String,
        pipelineId: String,
        locked: Boolean,
        transactionContext: DSLContext? = null
    ): Boolean {
        return pipelineInfoDao.update(
            dslContext = transactionContext ?: dslContext,
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            locked = locked
        )
    }

    fun getPipelineOauthUser(projectId: String, pipelineId: String): String? {
        return try {
            client.get(ServiceAuthAuthorizationResource::class).getResourceAuthorization(
                projectId = projectId,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = pipelineId
            ).data
        } catch (ignored: Exception) {
            logger.info("get pipeline oauth user fail", ignored)
            null
        }?.handoverFrom
    }

    /**
     * 保存流水线单体回调记录
     */
    private fun savePipelineCallback(
        events: Map<String, PipelineCallbackEvent>?,
        pipelineId: String,
        projectId: String,
        dslContext: DSLContext,
        userId: String
    ) {
        if (events.isNullOrEmpty()) return
        val existEventNames = pipelineCallbackDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ).map { it.name }.toSet()
        if (existEventNames.isNotEmpty()) {
            val needDelNames = existEventNames.subtract(events.keys).toSet()
            pipelineCallbackDao.delete(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                names = needDelNames
            )
        }
        // 保存回调事件
        pipelineCallbackDao.save(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            list = events.map { (key, value) ->
                value.copy(secretToken = value.secretToken?.let { AESUtil.encrypt(aesKey, it) })
            }
        )
    }

    fun getReleaseVersionRecord(projectId: String, pipelineId: String): PipelineResourceVersion? {
        return pipelineResourceVersionDao.getReleaseVersionRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }
}
