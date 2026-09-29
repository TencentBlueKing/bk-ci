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
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import com.tencent.devops.common.pipeline.pojo.setting.BuildCancelPolicy
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.JobTemplateModel
import com.tencent.devops.common.pipeline.template.StageTemplateModel
import com.tencent.devops.common.pipeline.template.StepTemplateModel
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.pojo.TemplateModelTransferInput
import com.tencent.devops.process.yaml.transfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.v3.enums.SyntaxDialectType
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYamlParser
import com.tencent.devops.process.yaml.v3.models.PreTemplateScriptBuildYamlV3Parser
import com.tencent.devops.process.yaml.v3.models.TriggerType
import com.tencent.devops.process.yaml.v3.models.VariableTemplate
import com.tencent.devops.process.yaml.v3.models.on.IPreTriggerOn
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.stage.PreStage
import com.tencent.devops.process.yaml.v3.parsers.template.Constants.TEMPLATE_KEY
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("ComplexMethod")
class TemplateModelTransfer @Autowired constructor(
    val client: Client,
    val modelStage: StageTransfer,
    val elementTransfer: ElementTransfer,
    val variableTransfer: VariableTransfer,
    val transferCache: TransferCacheService,
    val modelTransfer: ModelTransfer
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateModelTransfer::class.java)
    }

    fun yaml2TemplateModel(
        yamlInput: YamlTransferInput
    ): ITemplateModel {
        yamlInput.aspectWrapper.setYaml4Yaml(yamlInput.yaml, PipelineTransferAspectWrapper.AspectType.BEFORE)
        val transferType = yamlInput.templateType ?: Model::class.java
        when (transferType) {
            Model::class.java -> {
                val stageList = mutableListOf<Stage>()
                val model = Model(
                    name = yamlInput.yaml.name ?: "",
                    desc = yamlInput.yaml.desc ?: "",
                    stages = stageList,
                    labels = emptyList(),
                    instanceFromTemplate = false,
                    pipelineCreator = yamlInput.userId
                )
                model.projectId = yamlInput.projectCode
                model.publicVarGroups = yamlInput.yaml.formatVariableTemplates().map {
                    PublicVarGroupRef.create(groupName = it.name, versionName = it.version)
                }

                // 蓝盾引擎会将stageId从1开始顺序强制重写，因此在生成model时保持一致
                var stageIndex = 1
                stageList.add(modelStage.yaml2TriggerStage(yamlInput, stageIndex++))

                // 其他的stage
                yamlInput.yaml.formatStages().forEach { stage ->
                    yamlInput.aspectWrapper.setYamlStage4Yaml(
                        yamlStage = stage,
                        aspectType = PipelineTransferAspectWrapper.AspectType.BEFORE
                    )
                    stageList.add(
                        modelStage.yaml2Stage(
                            stage = stage,
                            // stream的stage标号从1开始，后续都加1
                            stageIndex = stageIndex++,
                            yamlInput = yamlInput
                        ).also {
                            yamlInput.aspectWrapper.setModelStage4Model(
                                it,
                                PipelineTransferAspectWrapper.AspectType.AFTER
                            )
                        }
                    )
                }
                // 添加finally
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
                            yamlInput.aspectWrapper.setModelStage4Model(
                                it,
                                PipelineTransferAspectWrapper.AspectType.AFTER
                            )
                        }
                    )
                }
                yamlInput.aspectWrapper.setModel4Model(model, PipelineTransferAspectWrapper.AspectType.AFTER)
                return model
            }

            else -> {
                throw IllegalArgumentException("unsupported transfer type: $transferType")
            }
        }
    }

    fun templateModel2yaml(modelInput: TemplateModelTransferInput): IPreTemplateScriptBuildYamlParser {
        val model = modelInput.model
        val setting = modelInput.setting
        val baseYaml = when (modelInput.version) {
            YamlVersion.V2_0 -> throw PipelineTransferException(YAML_NOT_VALID, arrayOf("only support v3"))
            YamlVersion.V3_0 -> PreTemplateScriptBuildYamlV3Parser(
                version = "v3.0"
            )
        }
        if (model is Model) {
            baseYaml.resources = model.resources
        }
        when (modelInput.version) {
            YamlVersion.V2_0 -> {
                throw PipelineTransferException(YAML_NOT_VALID, arrayOf("only support v3"))
            }

            YamlVersion.V3_0 -> {
                baseYaml.triggerOn =
                    makeTriggerOn(modelInput).ifEmpty { null }?.let { if (it.size == 1) it.first() else it }
            }
        }
        val stages = mutableListOf<PreStage>()
        model.stages()?.forEachIndexed { index, stage ->
            if (index == 0 || stage.finally) return@forEachIndexed
            modelInput.aspectWrapper.setModelStage4Model(stage, PipelineTransferAspectWrapper.AspectType.BEFORE)
            val ymlStage = modelStage.model2YamlStage(
                stage = stage,
                userId = modelInput.userId,
                projectId = modelInput.projectId,
                aspectWrapper = modelInput.aspectWrapper
            )
            modelInput.aspectWrapper.setYamlStage4Yaml(
                yamlPreStage = ymlStage,
                aspectType = PipelineTransferAspectWrapper.AspectType.AFTER
            )
            stages.add(ymlStage)
        }
        baseYaml.stages = stages.ifEmpty { null }?.let { TransferMapper.anyTo(stages) }
        baseYaml.variables = model.triggerContainer()?.let { triggerContainer ->
            makeVariablesYaml(modelInput, triggerContainer)
        }
        val lastStage = model.stages()?.last()
        val finally = if (lastStage?.finally == true) {
            modelInput.aspectWrapper.setModelStage4Model(lastStage, PipelineTransferAspectWrapper.AspectType.BEFORE)
            modelStage.model2YamlStage(
                stage = lastStage,
                userId = modelInput.userId,
                projectId = modelInput.projectId,
                aspectWrapper = modelInput.aspectWrapper
            ).jobs as LinkedHashMap<String, Any>?
        } else null
        baseYaml.finally = finally

        baseYaml.recommendedVersion = modelInput.model.triggerContainer()?.let {
            variableTransfer.makeRecommendedVersion(it)
        }
        if (setting != null) {
            baseYaml.name = setting.pipelineName
            baseYaml.desc = setting.desc.ifEmpty { null }
            baseYaml.label = prepareYamlLabels(modelInput.userId, setting).ifEmpty { null }
            baseYaml.notices = modelTransfer.makeNoticesV3(setting)
            baseYaml.syntaxDialect = makeSyntaxDialect(setting)
            baseYaml.concurrency = modelTransfer.makeConcurrency(setting)
            baseYaml.customBuildNum = setting.buildNumRule
            baseYaml.disablePipeline = (setting.runLockType == PipelineRunLockType.LOCK).nullIfDefault(false)
            baseYaml.failIfVariableInvalid = setting.failIfVariableInvalid.nullIfDefault(false)
            baseYaml.cancelPolicy =
                setting.buildCancelPolicy.nullIfDefault(BuildCancelPolicy.EXECUTE_PERMISSION)?.yamlCode()
            modelInput.aspectWrapper.setYaml4Yaml(baseYaml, PipelineTransferAspectWrapper.AspectType.AFTER)
        }
        return baseYaml
    }

    private fun makeVariablesYaml(
        modelInput: TemplateModelTransferInput,
        triggerContainer: TriggerContainer
    ): Map<String, Any>? {
        modelInput.model.handlePublicVarInfo()
        val variables = mutableMapOf<String, Any>()
        val publicVarGroups = modelInput.model.getPublicVarGroups()
        if (!publicVarGroups.isNullOrEmpty()) {
            variables[TEMPLATE_KEY] = publicVarGroups.map {
                VariableTemplate(it.groupName, it.versionName)
            }
        }
        // 模板实例化流水线：仅输出公共变量组引用，不输出模板参数变量
        if ((modelInput.model as? Model)?.template == null) {
            variableTransfer.makeVariableFromModel(triggerContainer)?.let { variables.putAll(it) }
        }
        return variables.ifEmpty { null }
    }

    private fun ITemplateModel.triggerContainer() = when (this) {
        is Model -> stages[0].containers[0] as TriggerContainer
        is StageTemplateModel -> null
        is JobTemplateModel -> null
        is StepTemplateModel -> null
        else -> null
    }

    private fun ITemplateModel.handlePublicVarInfo() = when (this) {
        is Model -> handlePublicVarInfo()
        is StageTemplateModel -> null
        is JobTemplateModel -> null
        is StepTemplateModel -> null
        else -> null
    }

    private fun ITemplateModel.getPublicVarGroups(): List<PublicVarGroupRef>? {
        if (this !is Model) {
            return null
        }
        return publicVarGroups
    }

    private fun ITemplateModel.stages() = when (this) {
        is Model -> stages
        is StageTemplateModel -> stages
        is JobTemplateModel -> null
        is StepTemplateModel -> null
        else -> null
    }

    private fun makeTriggerOn(modelInput: TemplateModelTransferInput): List<IPreTriggerOn> {
        val model = modelInput.model
        if (model !is Model) {
            return emptyList()
        }
        modelInput.aspectWrapper.setModelStage4Model(
            model.stages[0],
            PipelineTransferAspectWrapper.AspectType.BEFORE
        )
        modelInput.aspectWrapper.setModelJob4Model(
            model.stages[0].containers[0],
            PipelineTransferAspectWrapper.AspectType.BEFORE
        )
        val triggers = (model.getTriggerContainer()).elements
        val baseTrigger = elementTransfer.baseTriggers2yaml(
            elements = triggers,
            aspectWrapper = modelInput.aspectWrapper,
            userId = modelInput.userId,
            projectId = modelInput.projectId
        )?.toPre(modelInput.version)
        val scmTrigger = elementTransfer.scmTriggers2Yaml(
            triggers, modelInput.projectId, modelInput.aspectWrapper
        )
        // TAPD 触发独立聚合，与代码库触发平级
        val tapdTrigger = elementTransfer.tapdTriggers2Yaml(triggers, modelInput.aspectWrapper)
            .map { it.toPre(modelInput.version) }
        // 统一框架触发器（如制品到达）：注册表驱动，已带 type 标识，与代码库触发平级
        val registryTrigger = elementTransfer.registryTriggers2Yaml(
            triggers, modelInput.version, modelInput.aspectWrapper
        )
        return when (modelInput.version) {
            YamlVersion.V2_0 -> makeTriggerOnV2(modelInput, scmTrigger, baseTrigger)
            YamlVersion.V3_0 -> makeTriggerOnV3(
                version = modelInput.version,
                scmTrigger = scmTrigger,
                tapdTrigger = tapdTrigger,
                registryTrigger = registryTrigger,
                baseTrigger = baseTrigger
            )
        }
    }

    private fun makeTriggerOnV2(
        modelInput: TemplateModelTransferInput,
        scmTrigger: Map<ScmType, List<TriggerOn>>,
        baseTrigger: IPreTriggerOn?
    ): List<IPreTriggerOn> {
        val defaultScm = scmTrigger[modelInput.defaultScmType]
        // 融合默认git触发器 + 基础触发器
        if (defaultScm != null && defaultScm.size == 1) {
            val res = defaultScm.first().toPre(modelInput.version) as PreTriggerOn
            return listOf(
                res.copy(
                    manual = baseTrigger?.manual,
                    schedules = baseTrigger?.schedules,
                    remote = baseTrigger?.remote
                )
            )
        }
        // 只带基础触发器 / 不带触发器
        return if (baseTrigger != null) listOf(baseTrigger) else emptyList()
    }

    private fun makeTriggerOnV3(
        version: YamlVersion,
        scmTrigger: Map<ScmType, List<TriggerOn>>,
        tapdTrigger: List<IPreTriggerOn>,
        registryTrigger: List<IPreTriggerOn>,
        baseTrigger: IPreTriggerOn?
    ): List<IPreTriggerOn> {
        val triggerV3 = collectTriggerOnV3(version, scmTrigger, tapdTrigger)
        triggerV3.addAll(registryTrigger)
        return mergeBaseTriggerOnV3(baseTrigger, triggerV3)
    }

    private fun collectTriggerOnV3(
        version: YamlVersion,
        scmTrigger: Map<ScmType, List<TriggerOn>>,
        tapdTrigger: List<IPreTriggerOn>
    ): MutableList<IPreTriggerOn> {
        val triggerV3: MutableList<IPreTriggerOn> = scmTrigger.flatMap { (scmType, triggerOns) ->
            triggerOns.map { pre ->
                val preV3 = pre.toPre(version) as PreTriggerOnV3
                if (!preV3.repoName.isNullOrBlank()) {
                    preV3.type = scmType.alis
                }
                preV3
            }
        }.toMutableList()
        tapdTrigger.forEach { pre ->
            (pre as? PreTriggerOnV3)?.type = TriggerType.TAPD.alis
            triggerV3.add(pre)
        }
        return triggerV3
    }

    private fun mergeBaseTriggerOnV3(
        baseTrigger: IPreTriggerOn?,
        triggerV3: List<IPreTriggerOn>
    ): List<IPreTriggerOn> {
        if (baseTrigger == null) return triggerV3
        return when (triggerV3.size) {
            // 只带基础触发器
            0 -> listOf(baseTrigger)
            1 -> listOf(mergeSingleTriggerOnV3(baseTrigger, triggerV3.first() as PreTriggerOnV3))
            // 队列首插入基础触发器
            else -> listOf(baseTrigger) + triggerV3
        }
    }

    private fun mergeSingleTriggerOnV3(
        baseTrigger: IPreTriggerOn,
        only: PreTriggerOnV3
    ): IPreTriggerOn {
        if (only.events.isNotEmpty()) {
            // 嵌套式触发器（如 artifact）：事件放入 events，再加上基础类型
            (baseTrigger as PreTriggerOnV3).events[only.type!!] = only.events
            return baseTrigger
        }
        // 融合唯一代码库触发器 + 基础触发器
        return only.copy(
            manual = baseTrigger.manual,
            schedules = baseTrigger.schedules,
            remote = baseTrigger.remote
        )
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
        userId: String,
        pipelineSetting: PipelineSetting
    ): List<String> {
        val labels = mutableListOf<String>()

        transferCache.getPipelineLabel(userId, pipelineSetting.projectId)?.forEach { group ->
            group.labels.forEach {
                if (pipelineSetting.labels.contains(it.id)) labels.add(it.name)
            }
        }
        return labels
    }
}
