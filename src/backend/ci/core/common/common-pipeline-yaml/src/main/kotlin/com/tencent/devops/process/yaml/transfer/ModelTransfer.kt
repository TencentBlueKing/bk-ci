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
 */

package com.tencent.devops.process.yaml.transfer

import com.tencent.devops.common.api.constant.CommonMessageCode.YAML_NOT_VALID
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.TemplateInstanceDescriptor
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceField
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceRecommendedVersion
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceTriggerConfig
import com.tencent.devops.common.pipeline.pojo.TemplateVariable
import com.tencent.devops.common.pipeline.pojo.setting.BuildCancelPolicy
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSettingGroupType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.pojo.transfer.ExtendsRecommendedVersion
import com.tencent.devops.common.pipeline.pojo.transfer.ExtendsTriggerConfig
import com.tencent.devops.common.pipeline.pojo.transfer.IfType
import com.tencent.devops.common.pipeline.pojo.transfer.PreTemplateVariable
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_CONCURRENCY_GROUP_DEFAULT
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.transfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.v3.enums.SyntaxDialectType
import com.tencent.devops.process.yaml.v3.models.Concurrency
import com.tencent.devops.process.yaml.v3.models.ExtendsTemplate
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYamlParser
import com.tencent.devops.process.yaml.v3.models.Notices
import com.tencent.devops.process.yaml.v3.models.PacNotices
import com.tencent.devops.process.yaml.v3.models.PreExtends
import com.tencent.devops.process.yaml.v3.models.PreTemplateScriptBuildYamlV3Parser
import com.tencent.devops.process.yaml.v3.models.VariableTemplate
import com.tencent.devops.process.yaml.v3.models.on.IPreTriggerOn
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import com.tencent.devops.process.yaml.v3.models.stage.PreStage
import com.tencent.devops.process.yaml.v3.parsers.template.Constants.TEMPLATE_KEY
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("ComplexMethod")
class ModelTransfer @Autowired constructor(
    val client: Client,
    val modelStage: StageTransfer,
    val elementTransfer: ElementTransfer,
    val variableTransfer: VariableTransfer,
    val transferCache: TransferCacheService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ModelTransfer::class.java)
    }

    fun yaml2Labels(yamlInput: YamlTransferInput): List<String> {
        return preparePipelineLabels(yamlInput.userId, yamlInput.projectCode, yamlInput.yaml)
    }

    fun yaml2Setting(yamlInput: YamlTransferInput): PipelineSetting {
        val yaml = yamlInput.yaml
        return PipelineSetting(
            projectId = yamlInput.pipelineInfo?.projectId ?: "",
            pipelineId = yamlInput.pipelineInfo?.pipelineId ?: "",
            buildNumRule = yaml.customBuildNum,
            // 如果yaml内容缺失，先用pac文件名兜底，没有文件名才用原流水线名
            pipelineName = yaml.name ?: yamlInput.yamlFileName ?: yamlInput.pipelineInfo?.pipelineName ?: "",
            desc = yaml.desc ?: yamlInput.pipelineInfo?.pipelineDesc ?: "",
            concurrencyGroup = yaml.concurrency?.group ?: PIPELINE_SETTING_CONCURRENCY_GROUP_DEFAULT,
            // Cancel-In-Progress 配置group后默认为true
            concurrencyCancelInProgress = yaml.concurrency?.cancelInProgress ?: false,
            runLockType = when {
                yaml.disablePipeline == true -> PipelineRunLockType.LOCK
                yaml.concurrency?.group != null -> PipelineRunLockType.GROUP_LOCK
                else -> PipelineRunLockType.MULTIPLE
            },
            waitQueueTimeMinute = yaml.concurrency?.queueTimeoutMinutes
                ?: VariableDefault.DEFAULT_WAIT_QUEUE_TIME_MINUTE,
            maxQueueSize = yaml.concurrency?.queueLength ?: VariableDefault.DEFAULT_PIPELINE_SETTING_MAX_QUEUE_SIZE,
            maxConRunningQueueSize = yaml.concurrency?.maxParallel ?: PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX,
            labels = yaml2Labels(yamlInput),
            pipelineAsCodeSettings = yamlSyntaxDialect2Setting(yaml.syntaxDialect),
            successSubscriptionList = yamlNotice2Setting(
                projectId = yamlInput.projectCode,
                notices = yaml.notices?.filter { it.checkNotifyForSuccess() }
            ),
            failSubscriptionList = yamlNotice2Setting(
                projectId = yamlInput.projectCode,
                notices = yaml.notices?.filter { it.checkNotifyForFail() }
            ),
            failIfVariableInvalid = yaml.failIfVariableInvalid.nullIfDefault(false),
            buildCancelPolicy = BuildCancelPolicy.codeParse(yaml.cancelPolicy)
        )
    }

    private fun yamlNotice2Setting(projectId: String, notices: List<Notices>?): List<Subscription> {
        if (notices.isNullOrEmpty()) return listOf()
        return notices.map {
            val res = it.toSubscription()
            prepareModelGroups(projectId, res)
        }
    }

    private fun yamlSyntaxDialect2Setting(syntaxDialectType: String?): PipelineAsCodeSettings? {
        if (syntaxDialectType.isNullOrBlank()) return null
        return when (syntaxDialectType) {
            SyntaxDialectType.CLASSIC.name -> PipelineAsCodeSettings(
                inheritedDialect = false,
                pipelineDialect = PipelineDialectType.CLASSIC.name
            )

            SyntaxDialectType.CONSTRAINT.name -> PipelineAsCodeSettings(
                inheritedDialect = false,
                pipelineDialect = PipelineDialectType.CONSTRAINED.name
            )

            else -> PipelineAsCodeSettings(inheritedDialect = true)
        }
    }

    private fun prepareModelGroups(projectId: String, notice: Subscription): Subscription {
        if (notice.groups.isEmpty()) return notice
        val info = transferCache.getProjectGroupAndUsers(projectId)?.associateBy { it.displayName } ?: return notice
        val groups = notice.groups.map { info[it]?.roleName ?: "" }.toSet()
        return notice.copy(groups = groups)
    }

    private fun prepareYamlGroups(projectId: String, notice: PacNotices): PacNotices {
        if (notice.groups.isNullOrEmpty()) return notice
        val info = transferCache.getProjectGroupAndUsers(projectId)?.associateBy { it.roleName } ?: return notice
        val groups = notice.groups.mapNotNull { info[it]?.displayName }.ifEmpty { null }
        return notice.copy(groups = groups)
    }

    fun yaml2Model(
        yamlInput: YamlTransferInput
    ): Model {
        yamlInput.aspectWrapper.setYaml4Yaml(yamlInput.yaml, PipelineTransferAspectWrapper.AspectType.BEFORE)
        val stageList = mutableListOf<Stage>()
        val model = Model(
            name = yamlInput.yaml.name ?: yamlInput.pipelineInfo?.pipelineName ?: "",
            desc = yamlInput.yaml.desc ?: yamlInput.pipelineInfo?.pipelineDesc ?: "",
            stages = stageList,
            labels = emptyList(),
            instanceFromTemplate = false,
            pipelineCreator = yamlInput.pipelineInfo?.creator ?: yamlInput.userId
        )
        model.latestVersion = yamlInput.pipelineInfo?.version ?: 0
        model.publicVarGroups = yamlInput.yaml.formatVariableTemplates().map {
            PublicVarGroupRef(groupName = it.name, versionName = it.version)
        }

        // 蓝盾引擎会将stageId从1开始顺序强制重写，因此在生成model时保持一致
        val stageIndex = AtomicInteger(0)
        // trigger stage
        if (!yamlInput.yaml.checkForTemplateUse()) {
            stageList.add(modelStage.yaml2TriggerStage(yamlInput, stageIndex.incrementAndGet()))
        }

        // 其他的stage
        formatStage(yamlInput, stageList, stageIndex)
        // 添加finally
        formatFinally(yamlInput, stageList, stageIndex.incrementAndGet())
        formatTemplate(yamlInput, model)
        yamlInput.aspectWrapper.setModel4Model(model, PipelineTransferAspectWrapper.AspectType.AFTER)
        return model
    }

    private fun formatTemplate(
        input: YamlTransferInput,
        model: Model
    ) {
        val template = input.yaml.formatExtends()?.template
        if (template != null) {
            model.templateId = template.templateId
            model.template = TemplateInstanceDescriptor(
                templateRefType = if (template.templateId != null) {
                    TemplateRefType.ID
                } else {
                    TemplateRefType.PATH
                },
                templatePath = template.templatePath,
                templateRef = template.templateRef,
                templateId = template.templateId,
                templateVersionName = template.templateVersionName,
                templateVariables = template.variables?.map {
                    TemplateVariable(
                        key = it.key,
                        value = it.value.value,
                        allowModifyAtStartup = it.value.allowModifyAtStartup ?: false
                    )
                },
                triggerConfigs = template.triggerConfig?.map {
                    TemplateInstanceTriggerConfig(
                        stepId = it.key,
                        disabled = it.value.disabled,
                        cron = it.value.cron?.let { c -> listOf(c) },
                        variables = it.value.variables?.map { v ->
                            TemplateVariable(key = v.key, value = v.value)
                        }
                    )
                },
                recommendedVersion = template.recommendedVersion?.let {
                    TemplateInstanceRecommendedVersion(
                        allowModifyAtStartup = it.allowModifyAtStartup,
                        major = it.major,
                        minor = it.minor,
                        fix = it.fix,
                        buildNo = it.buildNo?.let { buildNo ->
                            TemplateInstanceRecommendedVersion.BuildNo(buildNo.initialValue)
                        }
                    )
                }
            )
            model.overrideTemplateField = TemplateInstanceField(
                paramIds = template.variables?.keys?.toList(),
                triggerStepIds = template.triggerConfig?.keys?.toList(),
                settingGroups = input.yaml.settingGroups(),
            )
        }
    }

    private fun formatStage(
        yamlInput: YamlTransferInput,
        stageList: MutableList<Stage>,
        stageIndex: AtomicInteger
    ) {
        yamlInput.yaml.formatStages().forEach { stage ->
            yamlInput.aspectWrapper.setYamlStage4Yaml(
                yamlStage = stage,
                aspectType = PipelineTransferAspectWrapper.AspectType.BEFORE
            )
            stageList.add(
                modelStage.yaml2Stage(
                    stage = stage,
                    // stream的stage标号从1开始，后续都加1
                    stageIndex = stageIndex.incrementAndGet(),
                    yamlInput = yamlInput
                ).also {
                    yamlInput.aspectWrapper.setModelStage4Model(it, PipelineTransferAspectWrapper.AspectType.AFTER)
                }
            )
        }
    }

    private fun formatFinally(
        yamlInput: YamlTransferInput,
        stageList: MutableList<Stage>,
        stageIndex: Int
    ) {
        val finallyJobs = yamlInput.yaml.formatFinallyStage()
        if (finallyJobs.isNotEmpty()) {
            yamlInput.aspectWrapper.setYamlStage4Yaml(
                aspectType = PipelineTransferAspectWrapper.AspectType.BEFORE
            )
            stageList.add(
                modelStage.yaml2FinallyStage(
                    stageIndex = stageIndex,
                    finallyJobs = finallyJobs,
                    yamlInput = yamlInput
                ).also {
                    yamlInput.aspectWrapper.setModelStage4Model(it, PipelineTransferAspectWrapper.AspectType.AFTER)
                }
            )
        }
    }

    fun model2yaml(modelInput: ModelTransferInput): IPreTemplateScriptBuildYamlParser {
        modelInput.aspectWrapper.setModel4Model(modelInput.model, PipelineTransferAspectWrapper.AspectType.BEFORE)
        val label = prepareYamlLabels(modelInput.setting).ifEmpty { null }
        val yaml = when (modelInput.version) {
            YamlVersion.V2_0 -> throw PipelineTransferException(YAML_NOT_VALID, arrayOf("only support v3"))
            YamlVersion.V3_0 -> PreTemplateScriptBuildYamlV3Parser(
                version = "v3.0",
                name = modelInput.setting.pipelineName,
                desc = modelInput.setting.desc.ifEmpty { null },
                label = label,
                resources = modelInput.model.resources,
                syntaxDialect = makeSyntaxDialect(modelInput.setting)
            )
        }

        val triggerOn = makeTriggerOn(modelInput)
        when (modelInput.version) {
            YamlVersion.V2_0 -> {
                throw PipelineTransferException(YAML_NOT_VALID, arrayOf("only support v3"))
            }

            YamlVersion.V3_0 -> {
                yaml.triggerOn = triggerOn.ifEmpty { null }?.let { if (it.size == 1) it.first() else it }
            }
        }
        yaml.notices = makeNoticesV3(modelInput)
        yaml.stages = makeStages(modelInput).ifEmpty { null }?.let { TransferMapper.anyTo(it) }
        val variables = mutableMapOf<String, Any>()
        modelInput.model.handlePublicVarInfo()
        val publicVarGroups = modelInput.model.publicVarGroups
        if (!publicVarGroups.isNullOrEmpty()) {
            variables[TEMPLATE_KEY] = publicVarGroups.map {
                VariableTemplate(it.groupName, it.versionName)
            }
        }
        variableTransfer.makeVariableFromModel(modelInput.model.getTriggerContainer())?.let { variables.putAll(it) }
        yaml.variables = if (variables.isEmpty()) null else variables
        yaml.extends = makeExtend(modelInput.model)
        yaml.finally = makeFinally(modelInput)?.ifEmpty { null }
        yaml.concurrency = makeConcurrency(modelInput)
        yaml.customBuildNum = makeBuildNum(modelInput)
        yaml.recommendedVersion = variableTransfer.makeRecommendedVersion(getTriggerContainer(modelInput))
        yaml.disablePipeline = (modelInput.setting.runLockType == PipelineRunLockType.LOCK ||
            modelInput.pipelineInfo?.locked == true).nullIfDefault(false)
        yaml.failIfVariableInvalid = modelInput.setting.failIfVariableInvalid.nullIfDefault(false)
        yaml.cancelPolicy =
            modelInput.setting.buildCancelPolicy.nullIfDefault(BuildCancelPolicy.EXECUTE_PERMISSION)?.yamlCode()
        modelInput.aspectWrapper.setYaml4Yaml(yaml, PipelineTransferAspectWrapper.AspectType.AFTER)
        return yaml
    }

    private fun makeStages(modelInput: ModelTransferInput): MutableList<PreStage> {
        val stages = mutableListOf<PreStage>()
        if (modelInput.fromTemplate()) return mutableListOf()
        modelInput.model.stages.forEachIndexed { index, stage ->
            if (index == 0 || stage.finally) return@forEachIndexed
            modelInput.aspectWrapper.setModelStage4Model(stage, PipelineTransferAspectWrapper.AspectType.BEFORE)
            val ymlStage = modelStage.model2YamlStage(
                stage = stage,
                userId = modelInput.userId,
                projectId = modelInput.setting.projectId,
                aspectWrapper = modelInput.aspectWrapper
            )
            modelInput.aspectWrapper.setYamlStage4Yaml(
                yamlPreStage = ymlStage,
                aspectType = PipelineTransferAspectWrapper.AspectType.AFTER
            )
            stages.add(ymlStage)
        }
        return stages
    }

    private fun makeFinally(modelInput: ModelTransferInput): LinkedHashMap<String, Any>? {
        if (modelInput.fromTemplate()) return LinkedHashMap()
        val lastStage = modelInput.model.stages.lastOrNull()
        val finally = if (lastStage != null && lastStage.finally) {
            modelInput.aspectWrapper.setModelStage4Model(lastStage, PipelineTransferAspectWrapper.AspectType.BEFORE)
            modelStage.model2YamlStage(
                stage = lastStage,
                userId = modelInput.userId,
                projectId = modelInput.setting.projectId,
                aspectWrapper = modelInput.aspectWrapper
            ).jobs
        } else null
        return finally as LinkedHashMap<String, Any>?
    }

    private fun makeExtend(templateInfo: Model): PreExtends? {
        if (templateInfo.template == null) return null
        with(templateInfo.template!!) {
            return PreExtends(
                template = ExtendsTemplate(
                    templatePath = templatePath,
                    templateRef = templateRef,
                    templateId = templateId,
                    templateVersionName = templateVersionName,
                    variables = templateVariables?.associateBy(
                        { it.key },
                        { PreTemplateVariable(it.value, it.allowModifyAtStartup) }
                    )?.ifEmpty { null },
                    triggerConfig = triggerConfigs?.filter { it.stepId != null }?.associateBy(
                        { it.stepId!! },
                        {
                            ExtendsTriggerConfig(
                                disabled = it.disabled,
                                cron = it.cron?.firstOrNull(),
                                variables = it.variables?.filter { v -> v.value != null }
                                    ?.associateBy({ v -> v.key }, { v -> v.value!! })
                            )
                        }
                    )?.ifEmpty { null },
                    recommendedVersion = recommendedVersion?.let {
                        ExtendsRecommendedVersion(
                            allowModifyAtStartup = it.allowModifyAtStartup,
                            major = it.major,
                            minor = it.minor,
                            fix = it.fix,
                            buildNo = it.buildNo?.let { buildNo ->
                                ExtendsRecommendedVersion.BuildNo(
                                    initialValue = buildNo.buildNo,
                                )
                            }
                        )
                    }
                )
            )
        }
    }

    fun getTriggerContainer(modelInput: ModelTransferInput): TriggerContainer? {
        if (modelInput.fromTemplate()) return null
        return modelInput.model.stages.firstOrNull()?.containers?.firstOrNull() as TriggerContainer?
    }

    fun makeNoticesV3(modelInput: ModelTransferInput): List<PacNotices>? {
        if (modelInput.fromTemplate() && !modelInput.overrideTemplateSettingGroups(PipelineSettingGroupType.NOTICES)) {
            return null
        }
        return makeNoticesV3(modelInput.setting)
    }


    fun makeNoticesV3(setting: PipelineSetting): List<PacNotices>? {
        val res = mutableListOf<PacNotices>()
        setting.successSubscriptionList?.ifEmpty { setting.successSubscription?.let { listOf(it) } }?.forEach {
            if (it.types.isNotEmpty()) {
                val notice = PacNotices(it, IfType.SUCCESS.name)
                res.add(prepareYamlGroups(setting.projectId, notice))
            }
        }
        setting.failSubscriptionList?.ifEmpty { setting.failSubscription?.let { listOf(it) } }?.forEach {
            if (it.types.isNotEmpty()) {
                val notice = PacNotices(it, IfType.FAILURE.name)
                res.add(prepareYamlGroups(setting.projectId, notice))
            }
        }
        return res.ifEmpty { null }
    }

    fun makeBuildNum(modelInput: ModelTransferInput): String? {
        if (modelInput.fromTemplate() &&
            !modelInput.overrideTemplateSettingGroups(PipelineSettingGroupType.CUSTOM_BUILD_NUM)
        ) {
            return null
        }
        return modelInput.setting.buildNumRule
    }

    fun makeConcurrency(modelInput: ModelTransferInput): Concurrency? {
        if (modelInput.fromTemplate() &&
            !modelInput.overrideTemplateSettingGroups(PipelineSettingGroupType.CONCURRENCY)
        ) {
            return null
        }
        return makeConcurrency(modelInput.setting)
    }

    fun makeConcurrency(setting: PipelineSetting): Concurrency? {
        if (setting.runLockType == PipelineRunLockType.GROUP_LOCK ||
            setting.runLockType == PipelineRunLockType.LOCK
        ) {
            return Concurrency(
                group = setting.concurrencyGroup,
                cancelInProgress = setting.concurrencyCancelInProgress.nullIfDefault(false),
                queueLength = setting.maxQueueSize
                    .nullIfDefault(VariableDefault.DEFAULT_PIPELINE_SETTING_MAX_QUEUE_SIZE),
                queueTimeoutMinutes = setting.waitQueueTimeMinute
                    .nullIfDefault(VariableDefault.DEFAULT_WAIT_QUEUE_TIME_MINUTE),
                maxParallel = null
            )
        }
        if (setting.runLockType == PipelineRunLockType.MULTIPLE) {
            return Concurrency(
                group = null,
                cancelInProgress = null,
                queueLength = null,
                queueTimeoutMinutes = setting.waitQueueTimeMinute
                    .nullIfDefault(VariableDefault.DEFAULT_WAIT_QUEUE_TIME_MINUTE),
                maxParallel = setting.maxConRunningQueueSize.nullIfDefault(PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX)
            )
        }
        return null
    }

    private fun makeTriggerOn(modelInput: ModelTransferInput): List<IPreTriggerOn> {
        if (modelInput.model.stages.isEmpty()) return emptyList()
        modelInput.aspectWrapper.setModelStage4Model(
            modelInput.model.stages[0],
            PipelineTransferAspectWrapper.AspectType.BEFORE
        )
        modelInput.aspectWrapper.setModelJob4Model(
            modelInput.model.stages[0].containers[0],
            PipelineTransferAspectWrapper.AspectType.BEFORE
        )
        val triggers = getTriggerContainer(modelInput)?.elements ?: return emptyList()
        val baseTrigger = elementTransfer.baseTriggers2yaml(triggers, modelInput.aspectWrapper)
            ?.toPre(modelInput.version)
        val scmTrigger = elementTransfer.scmTriggers2Yaml(
            triggers, modelInput.setting.projectId, modelInput.aspectWrapper
        )
        when (modelInput.version) {
            YamlVersion.V2_0 -> {
                // 融合默认git触发器 + 基础触发器
                if (scmTrigger[modelInput.defaultScmType] != null &&
                    scmTrigger[modelInput.defaultScmType]!!.size == 1
                ) {
                    val res = scmTrigger[modelInput.defaultScmType]!!.first().toPre(modelInput.version) as PreTriggerOn
                    return listOf(
                        res.copy(
                            manual = baseTrigger?.manual,
                            schedules = baseTrigger?.schedules,
                            remote = baseTrigger?.remote
                        )
                    )
                }
                // 只带基础触发器
                if (baseTrigger != null) {
                    return listOf(baseTrigger)
                }
                // 不带触发器
                return emptyList()
            }

            YamlVersion.V3_0 -> {
                val trigger = mutableListOf<IPreTriggerOn>()
                val triggerV3 = mutableListOf<IPreTriggerOn>()
                scmTrigger.map { on ->
                    on.value.forEach { pre ->
                        triggerV3.add(pre.toPre(modelInput.version).also {
                            it as PreTriggerOnV3
                            if (!it.repoName.isNullOrBlank()) {
                                it.type = on.key.alis
                            }
                        })
                    }
                }
                if (baseTrigger != null) {
                    when (triggerV3.size) {
                        // 只带基础触发器
                        0 -> return listOf(baseTrigger)
                        // 融合一个git触发器 + 基础触发器
                        1 -> return listOf(
                            (triggerV3.first() as PreTriggerOnV3).copy(
                                manual = baseTrigger.manual,
                                schedules = baseTrigger.schedules,
                                remote = baseTrigger.remote
                            )
                        )
                        // 队列首插入基础触发器
                        else -> trigger.add(0, baseTrigger)
                    }
                }
                trigger.addAll(triggerV3)
                return trigger
            }
        }
    }

    private fun makeSyntaxDialect(setting: PipelineSetting): String? {
        val asCodeSettings = setting.pipelineAsCodeSettings ?: return null
        return when {
            asCodeSettings.inheritedDialect == true -> SyntaxDialectType.INHERIT.name
            asCodeSettings.pipelineDialect == PipelineDialectType.CLASSIC.name -> SyntaxDialectType.CLASSIC.name
            asCodeSettings.pipelineDialect == PipelineDialectType.CONSTRAINED.name -> SyntaxDialectType.CONSTRAINT.name
            else -> null
        }
    }

    @Suppress("NestedBlockDepth")
    private fun preparePipelineLabels(
        userId: String,
        projectCode: String,
        yaml: IPreTemplateScriptBuildYamlParser
    ): List<String> {
        val ymlLabel = yaml.label ?: return emptyList()
        val labels = mutableListOf<String>()

        transferCache.getPipelineLabel(userId, projectCode)?.forEach { group ->
            group.labels.forEach {
                if (ymlLabel.contains(it.name)) labels.add(it.id)
            }
        }
        return labels
    }

    private fun prepareYamlLabels(
        pipelineSetting: PipelineSetting
    ): List<String> {
        val labels = mutableListOf<String>()
        pipelineSetting.labels.forEach { labelId ->
            transferCache.getPipelineLabelById(
                projectId = pipelineSetting.projectId, labelId = labelId
            )?.let {
                labels.add(it.name)
            }
        }
        return labels
    }
}
