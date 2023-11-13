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

package com.tencent.devops.process.yaml.modelCreate

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_BUILD_TRIGGER
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_MANUAL_TRIGGER
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.user.UserPipelineGroupResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.yaml.modelCreate.inner.ModelCreateEvent
import com.tencent.devops.process.yaml.pojo.QualityElementInfo
import com.tencent.devops.process.yaml.v2.models.ScriptBuildYaml
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.tencent.devops.process.yaml.v2.models.stage.Stage as StreamV2Stage

@Component
class ModelCreate @Autowired constructor(
    val client: Client,
    val modelStage: ModelStage
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ModelCreate::class.java)
    }

    fun createPipelineModel(
        modelName: String,
        event: ModelCreateEvent,
        yaml: ScriptBuildYaml,
        pipelineParams: List<BuildFormProperty>,
        asCodeSettings: PipelineAsCodeSettings?
    ): PipelineModelAndSetting {
        // 流水线插件标签设置
        val labelList = preparePipelineLabels(event, yaml)

        val stageList = mutableListOf<Stage>()

        // 第一个stage，触发类
        val triggerElementList = mutableListOf<Element>()
        val manualTriggerElement = ManualTriggerElement(
            I18nUtil.getCodeLanMessage(BK_MANUAL_TRIGGER),
            "T-1-1-1"
        )
        triggerElementList.add(manualTriggerElement)

        val triggerContainer = TriggerContainer(
            id = "0",
            name = I18nUtil.getCodeLanMessage(BK_BUILD_TRIGGER),
            elements = triggerElementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = pipelineParams
        )

        // 蓝盾引擎会将stageId从1开始顺序强制重写，因此在生成model时保持一致
        var stageIndex = 1
        val stageId = VMUtils.genStageId(stageIndex++)
        val stage1 = Stage(listOf(triggerContainer), id = stageId, name = stageId)
        stageList.add(stage1)

        // 红线的步骤指标 xxx* 的判断，当指定多个指标时报错，所以需要维护一套List
        // list中保存
        val elementNames: MutableList<QualityElementInfo> = mutableListOf()

        // 其他的stage
        yaml.stages.forEach { stage ->
            stageList.add(
                modelStage.createStage(
                    stage = stage,
                    event = event,
                    // stream的stage标号从1开始，后续都加1
                    stageIndex = stageIndex++,
                    resources = yaml.resource,
                    jobBuildTemplateAcrossInfos = event.jobTemplateAcrossInfo,
                    elementNames = elementNames
                )
            )
        }
        // 添加finally
        if (!yaml.finally.isNullOrEmpty()) {
            stageList.add(
                modelStage.createStage(
                    stage = StreamV2Stage(
                        name = "Finally",
                        label = emptyList(),
                        ifField = null,
                        fastKill = false,
                        jobs = yaml.finally!!,
                        checkIn = null,
                        checkOut = null
                    ),
                    event = event,
                    stageIndex = stageIndex,
                    finalStage = true,
                    resources = yaml.resource,
                    jobBuildTemplateAcrossInfos = event.jobTemplateAcrossInfo,
                    elementNames = null
                )
            )
        }

        return PipelineModelAndSetting(
            model = Model(
                name = modelName,
                desc = "",
                stages = stageList,
                labels = labelList,
                instanceFromTemplate = false,
                pipelineCreator = event.userId
            ),
            setting = PipelineSetting(
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
                // #6090 stream重试时均需要清理变量表
                cleanVariablesWhenRetry = true,
                maxQueueSize = yaml.concurrency?.queueLength ?: 1,
                labels = labelList,
                pipelineAsCodeSettings = asCodeSettings
            )
        )
    }

    @Suppress("NestedBlockDepth")
    private fun preparePipelineLabels(
        event: ModelCreateEvent,
        yaml: ScriptBuildYaml
    ): List<String> {
        val gitCIPipelineLabels = mutableListOf<String>()

        try {
            // 获取当前项目下存在的标签组
            val pipelineGroups = client.get(UserPipelineGroupResource::class)
                .getGroups(event.userId, event.projectCode)
                .data

            yaml.label?.forEach {
                // 要设置的标签组不存在，新建标签组和标签（同名）
                if (!checkPipelineLabel(it, pipelineGroups)) {
                    client.get(UserPipelineGroupResource::class).addGroup(
                        event.userId,
                        PipelineGroupCreate(
                            projectId = event.projectCode,
                            name = it
                        )
                    )

                    val pipelineGroup = getPipelineGroup(it, event.userId, event.projectCode)
                    if (pipelineGroup != null) {
                        client.get(UserPipelineGroupResource::class).addLabel(
                            userId = event.userId,
                            projectId = event.projectCode,
                            pipelineLabel = PipelineLabelCreate(
                                groupId = pipelineGroup.id,
                                name = it
                            )
                        )
                    }
                }

                // 保证标签已创建成功后，取label加密ID
                val pipelineGroup = getPipelineGroup(it, event.userId, event.projectCode)
                gitCIPipelineLabels.add(pipelineGroup!!.labels[0].id)
            }
        } catch (e: Exception) {
            logger.warn("${event.userId}|${event.projectCode} preparePipelineLabels error.", e)
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
