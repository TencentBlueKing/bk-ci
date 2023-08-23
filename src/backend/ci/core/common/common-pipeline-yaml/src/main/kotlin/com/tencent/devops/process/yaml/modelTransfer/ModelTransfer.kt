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
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.modelTransfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.modelTransfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v2.models.Concurrency
import com.tencent.devops.process.yaml.v2.models.GitNotices
import com.tencent.devops.process.yaml.v2.models.IPreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.IfType
import com.tencent.devops.process.yaml.v2.models.Notices
import com.tencent.devops.process.yaml.v2.models.PacNotices
import com.tencent.devops.process.yaml.v2.models.PreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.models.stage.PreStage
import com.tencent.devops.process.yaml.v3.models.PreTemplateScriptBuildYamlV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ModelTransfer @Autowired constructor(
    val client: Client,
    val modelStage: StageTransfer,
    val modelElement: ElementTransfer,
    val transferCache: TransferCacheService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ModelTransfer::class.java)
    }

    fun yaml2Labels(yamlInput: YamlTransferInput): List<String> {
        return preparePipelineLabels(yamlInput.projectCode, yamlInput.yaml)
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
            concurrencyCancelInProgress = yaml.concurrency?.cancelInProgress ?: false,
            runLockType = when {
                yaml.concurrency?.group != null -> PipelineRunLockType.GROUP_LOCK
                else -> PipelineRunLockType.MULTIPLE
            },
            waitQueueTimeMinute = yaml.concurrency?.queueTimeoutMinutes
                ?: VariableDefault.DEFAULT_WAIT_QUEUE_TIME_MINUTE,
            maxQueueSize = yaml.concurrency?.queueLength ?: VariableDefault.DEFAULT_PIPELINE_SETTING_MAX_QUEUE_SIZE,
            labels = yaml2Labels(yamlInput),
            pipelineAsCodeSettings = yamlInput.asCodeSettings,
            successSubscriptionList = yamlNotice2Setting(
                projectId = yamlInput.projectCode,
                notices = yaml.notices?.filter { it.checkNotifyForSuccess() }
            ),
            failSubscriptionList = yamlNotice2Setting(
                projectId = yamlInput.projectCode,
                notices = yaml.notices?.filter { it.checkNotifyForFail() }
            )
        )
    }

    private fun yamlNotice2Setting(projectId: String, notices: List<Notices>?): List<Subscription> {
        if (notices.isNullOrEmpty()) return listOf(Subscription())
        return notices.map {
            val res = it.toSubscription()
            prepareModelGroups(projectId, res)
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
        val groups = notice.groups.mapNotNull { info[it]?.displayName }
        return notice.copy(groups = groups)
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

    fun model2yaml(modelInput: ModelTransferInput): IPreTemplateScriptBuildYaml {
        val stages = mutableListOf<PreStage>()
        modelInput.model.stages.forEachIndexed { index, stage ->
            if (index == 0 || stage.finally) return@forEachIndexed
            val ymlStage = modelStage.model2YamlStage(stage)
            stages.add(ymlStage)
        }
        val label = prepareYamlLabels(modelInput.setting).ifEmpty { null }
        val triggerOn = getTriggerOn(modelInput.model)
        val variables = getVariableFromModel(modelInput.model)
        val lastStage = modelInput.model.stages.last()
        val finally = if (lastStage.finally) modelStage.model2YamlStage(lastStage).jobs else null
        val concurrency = getConcurrency(modelInput.setting)

        return when (modelInput.version) {
            YamlVersion.Version.V2_0 -> PreTemplateScriptBuildYaml(
                version = "v2.0",
                name = modelInput.model.name,
                label = label,
                triggerOn = triggerOn[modelInput.defaultScmType]?.toPreV2(),
                variables = variables,
                stages = TransferMapper.anyTo(stages),
                extends = null,
                resources = null,
                notices = makeNoticesV2(modelInput.setting),
                finally = finally,
                concurrency = concurrency
            )
            YamlVersion.Version.V3_0 -> PreTemplateScriptBuildYamlV3(
                version = "v3.0",
                name = modelInput.model.name,
                label = label,
                triggerOn = triggerOn.map { on ->
                    on.value.toPreV3().also { it.type = on.key.alis }
                }.ifEmpty { null }?.let { if (it.size == 1) it.first() else it },
                variables = variables,
                stages = TransferMapper.anyTo(stages),
                extends = null,
                resources = null,
                notices = makeNoticesV3(modelInput.setting),
                finally = finally,
                concurrency = concurrency
            )
        }
    }

    private fun makeNoticesV2(setting: PipelineSetting): List<GitNotices> {
        val res = mutableListOf<GitNotices>()
        setting.successSubscriptionList?.forEach {
            if (it.types.isNotEmpty()) {
                res.add(GitNotices(it, IfType.SUCCESS.name))
            }
        }
        setting.failSubscriptionList?.forEach {
            if (it.types.isNotEmpty()) {
                res.add(GitNotices(it, IfType.FAILURE.name))
            }
        }
        return res
    }

    private fun makeNoticesV3(setting: PipelineSetting): List<PacNotices> {
        val res = mutableListOf<PacNotices>()
        setting.successSubscriptionList?.forEach {
            if (it.types.isNotEmpty()) {
                val notice = PacNotices(it, IfType.SUCCESS.name)
                res.add(prepareYamlGroups(setting.projectId, notice))
            }
        }
        setting.failSubscriptionList?.forEach {
            if (it.types.isNotEmpty()) {
                val notice = PacNotices(it, IfType.FAILURE.name)
                res.add(prepareYamlGroups(setting.projectId, notice))
            }
        }
        return res
    }

    private fun getConcurrency(setting: PipelineSetting): Concurrency? {
        if (setting.runLockType == PipelineRunLockType.GROUP_LOCK) {
            return Concurrency(
                group = setting.concurrencyGroup,
                cancelInProgress = setting.concurrencyCancelInProgress.nullIfDefault(false),
                queueLength = setting.maxQueueSize
                    .nullIfDefault(VariableDefault.DEFAULT_PIPELINE_SETTING_MAX_QUEUE_SIZE),
                queueTimeoutMinutes = setting.waitQueueTimeMinute
                    .nullIfDefault(VariableDefault.DEFAULT_WAIT_QUEUE_TIME_MINUTE)
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
        projectCode: String,
        yaml: IPreTemplateScriptBuildYaml
    ): List<String> {
        val ymlLabel = yaml.label ?: return emptyList()
        val labels = mutableListOf<String>()

        transferCache.getPipelineLabel(projectCode)?.forEach { group ->
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

        transferCache.getPipelineLabel(pipelineSetting.projectId)?.forEach { group ->
            group.labels.forEach {
                if (pipelineSetting.labels.contains(it.id)) labels.add(it.name)
            }
        }
        return labels
    }
}
