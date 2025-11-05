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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType
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
import com.tencent.devops.process.yaml.v3.models.on.IPreTriggerOn
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import com.tencent.devops.process.yaml.v3.models.stage.PreStage
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
        baseYaml.variables = model.triggerContainer()?.let { variableTransfer.makeVariableFromModel(it) }
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
            modelInput.aspectWrapper.setYaml4Yaml(baseYaml, PipelineTransferAspectWrapper.AspectType.AFTER)
        }
        return baseYaml
    }

    private fun ITemplateModel.triggerContainer() = when (this) {
        is Model -> stages[0].containers[0] as TriggerContainer
        is StageTemplateModel -> null
        is JobTemplateModel -> null
        is StepTemplateModel -> null
        else -> null
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
        val baseTrigger = elementTransfer.baseTriggers2yaml(triggers, modelInput.aspectWrapper)
            ?.toPre(modelInput.version)
        val scmTrigger = elementTransfer.scmTriggers2Yaml(
            triggers, modelInput.projectId, modelInput.aspectWrapper
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
