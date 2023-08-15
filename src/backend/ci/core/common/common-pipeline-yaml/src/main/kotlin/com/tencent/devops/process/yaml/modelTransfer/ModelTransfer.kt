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

package com.tencent.devops.process.yaml.modelTransfer

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.process.api.user.UserPipelineGroupResource
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT
import com.tencent.devops.process.yaml.modelTransfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.modelTransfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v2.models.Concurrency
import com.tencent.devops.process.yaml.v2.models.GitNotices
import com.tencent.devops.process.yaml.v2.models.IPreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYamlI
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.models.stage.PreStage
import com.tencent.devops.process.yaml.v3.models.PreScriptBuildYamlV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ModelTransfer @Autowired constructor(
    val client: Client,
    val modelStage: StageTransfer,
    val modelElement: ElementTransfer
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
            pipelineName = yaml.name ?: yamlInput.pipelineInfo?.pipelineName ?: "",
            desc = yamlInput.pipelineInfo?.pipelineDesc ?: "",
            concurrencyGroup = yaml.concurrency?.group,
            // Cancel-In-Progress 配置group后默认为true
            concurrencyCancelInProgress = yaml.concurrency?.cancelInProgress
                ?: yaml.concurrency?.group?.let { true }
                ?: true,
            runLockType = when {
                yaml.concurrency?.group != null -> PipelineRunLockType.GROUP_LOCK
                else -> PipelineRunLockType.MULTIPLE
            },
            waitQueueTimeMinute = yaml.concurrency?.queueTimeoutMinutes ?: TimeUnit.HOURS.toMinutes(8).toInt(),
            maxQueueSize = yaml.concurrency?.queueLength ?: PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT,
            labels = yaml2Labels(yamlInput),
            pipelineAsCodeSettings = yamlInput.asCodeSettings
        )
    }

    fun yaml2Model(
        yamlInput: YamlTransferInput
    ): Model {
        val stageList = mutableListOf<Stage>()

        // 蓝盾引擎会将stageId从1开始顺序强制重写，因此在生成model时保持一致
        var stageIndex = 1
        // todo 需要trigger on
        stageList.add(modelStage.yaml2TriggerStage(yamlInput, stageIndex++))

        // 其他的stage
        yamlInput.yaml.formatStages().forEach { stage ->
            stageList.add(
                modelStage.yaml2Stage(
                    stage = stage,
                    // stream的stage标号从1开始，后续都加1
                    stageIndex = stageIndex++,
                    yamlInput = yamlInput
                )
            )
        }
        // 添加finally
        val finallyJobs = yamlInput.yaml.formatFinallyStage()
        if (finallyJobs.isNotEmpty()) {
            stageList.add(
                modelStage.yaml2FinallyStage(
                    stageIndex = stageIndex,
                    finallyJobs = finallyJobs,
                    yamlInput = yamlInput
                )
            )
        }

        return Model(
            name = yamlInput.yaml.name ?: "",
            desc = "",
            stages = stageList,
            labels = emptyList(),
            instanceFromTemplate = false,
            pipelineCreator = yamlInput.pipelineInfo?.creator ?: yamlInput.userId
        )
    }

    fun model2yaml(modelInput: ModelTransferInput): PreScriptBuildYamlI {
        val stages = mutableListOf<PreStage>()
        modelInput.model.stages.forEachIndexed { index, stage ->
            if (index == 0 || stage.finally) return@forEachIndexed
            val ymlStage = modelStage.model2YamlStage(stage)
            stages.add(ymlStage)
        }
        val label = modelInput.model.labels.ifEmpty { null }
        val triggerOn = getTriggerOn(modelInput.model)
        val variables = getVariableFromModel(modelInput.model)
        val finally = modelStage.model2YamlStage(modelInput.model.stages.last()).jobs
        val concurrency = getConcurrency(modelInput.setting)
        val notices = "" // TODO: 2023/7/17

        return when (modelInput.version) {
            YamlVersion.Version.V2_0 -> PreScriptBuildYaml(
                version = "v2.0",
                name = modelInput.model.name,
                label = label,
                triggerOn = triggerOn[modelInput.defaultScmType]?.toPreV2(),
                variables = variables,
                stages = stages,
                extends = null,
                resources = null,
                notices = null,
                finally = finally,
                concurrency = concurrency
            )
            YamlVersion.Version.V3_0 -> PreScriptBuildYamlV3(
                version = "v3.0",
                name = modelInput.model.name,
                label = label,
                triggerOn = triggerOn.map { it.value.toPreV3() },
                variables = variables,
                stages = stages,
                extends = null,
                resources = null,
                notices = null,
                finally = finally,
                concurrency = concurrency
            )
        }
    }

    private fun getNotices(setting: PipelineSetting): List<GitNotices> {
        return emptyList()
    }

    private fun getConcurrency(setting: PipelineSetting): Concurrency? {
        if (setting.runLockType == PipelineRunLockType.GROUP_LOCK) {
            return Concurrency(
                group = setting.concurrencyGroup,
                cancelInProgress = setting.concurrencyCancelInProgress,
                queueLength = setting.maxQueueSize,
                queueTimeoutMinutes = setting.waitQueueTimeMinute
            )
        }
        return null
    }

    private fun getTriggerOn(model: Model): Map<ScmType, TriggerOn> {
        val triggers = (model.stages[0].containers[0] as TriggerContainer).elements
        return modelElement.triggers2Yaml(triggers)
    }

    private fun getVariableFromModel(model: Model): Map<String, Variable>? {
        val result = mutableMapOf<String, Variable>()
        (model.stages[0].containers[0] as TriggerContainer).params.forEach {
            // todo 启动参数需要更详细的解析
            result[it.id] = Variable(it.defaultValue.toString())
        }
        return if (result.isEmpty()) {
            null
        } else {
            result
        }
    }

    @Suppress("NestedBlockDepth")
    private fun preparePipelineLabels(
        userId: String,
        projectCode: String,
        yaml: IPreTemplateScriptBuildYaml
    ): List<String> {
        val gitCIPipelineLabels = mutableListOf<String>()

        try {
            // 获取当前项目下存在的标签组
            val pipelineGroups = client.get(UserPipelineGroupResource::class)
                .getGroups(userId, projectCode)
                .data

            yaml.label?.forEach {
                // 要设置的标签组不存在，新建标签组和标签（同名）
                if (!checkPipelineLabel(it, pipelineGroups)) {
                    client.get(UserPipelineGroupResource::class).addGroup(
                        userId,
                        PipelineGroupCreate(
                            projectId = projectCode,
                            name = it
                        )
                    )

                    val pipelineGroup = getPipelineGroup(it, userId, projectCode)
                    if (pipelineGroup != null) {
                        client.get(UserPipelineGroupResource::class).addLabel(
                            userId = userId,
                            projectId = projectCode,
                            pipelineLabel = PipelineLabelCreate(
                                groupId = pipelineGroup.id,
                                name = it
                            )
                        )
                    }
                }

                // 保证标签已创建成功后，取label加密ID
                val pipelineGroup = getPipelineGroup(it, userId, projectCode)
                gitCIPipelineLabels.add(pipelineGroup!!.labels[0].id)
            }
        } catch (e: Exception) {
            logger.warn("$userId|$projectCode preparePipelineLabels error.", e)
        }

        return gitCIPipelineLabels
    }

    private fun checkPipelineLabel(gitciPipelineLabel: String, pipelineGroups: List<PipelineGroup>?): Boolean {
        pipelineGroups?.forEach { pipelineGroup ->
            pipelineGroup.labels.forEach {
                if (it.name == gitciPipelineLabel) {
                    return true
                }
            }
        }

        return false
    }

    private fun getPipelineGroup(labelGroupName: String, userId: String, projectId: String): PipelineGroup? {
        val pipelineGroups = client.get(UserPipelineGroupResource::class)
            .getGroups(userId, projectId)
            .data
        pipelineGroups?.forEach {
            if (it.name == labelGroupName) {
                return it
            }
        }

        return null
    }
}
