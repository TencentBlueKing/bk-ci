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

package com.tencent.devops.stream.trigger.parsers.modelCreate

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.ci.task.DockerRunDevCloudTask
import com.tencent.devops.common.ci.task.GitCiCodeRepoTask
import com.tencent.devops.common.ci.task.ServiceJobDevCloudTask
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.utils.GitCIPipelineUtils
import com.tencent.devops.process.api.user.UserPipelineGroupResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ModelCreate @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val modelStage: ModelStage
) {

    @Value("\${rtx.v2GitUrl:#{null}}")
    private val v2GitUrl: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(ModelCreate::class.java)
    }

    fun createPipelineModel(
        event: GitRequestEvent,
        gitBasicSetting: GitCIBasicSetting,
        yaml: ScriptBuildYaml,
        pipeline: GitProjectPipeline,
        changeSet: Set<String>? = null,
        webhookParams: Map<String, String> = mapOf()
    ): Model {
        // 流水线插件标签设置
        val labelList = preparePipelineLabels(event, gitBasicSetting, yaml)

        // 预安装插件市场的插件
        ModelCommon.installMarketAtom(client, gitBasicSetting, event.userId, GitCiCodeRepoTask.atomCode)
        ModelCommon.installMarketAtom(client, gitBasicSetting, event.userId, DockerRunDevCloudTask.atomCode)
        ModelCommon.installMarketAtom(client, gitBasicSetting, event.userId, ServiceJobDevCloudTask.atomCode)

        val stageList = mutableListOf<Stage>()

        // 第一个stage，触发类
        val triggerElementList = mutableListOf<Element>()
        val manualTriggerElement = ManualTriggerElement("手动触发", "T-1-1-1")
        triggerElementList.add(manualTriggerElement)

        val originEvent = try {
            objectMapper.readValue<GitEvent>(event.event)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${e.message}")
            null
        }

        val params = ModelParameters.createPipelineParams(
            yaml = yaml,
            gitBasicSetting = gitBasicSetting,
            event = event,
            v2GitUrl = v2GitUrl,
            originEvent = originEvent,
            webhookParams = webhookParams
        )

        val triggerContainer = TriggerContainer(
            id = "0",
            name = "构建触发",
            elements = triggerElementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = params
        )

        // 蓝盾引擎会将stageId从1开始顺序强制重写，因此在生成model时保持一致
        var stageIndex = 1
        val stageId = VMUtils.genStageId(stageIndex++)
        val stage1 = Stage(listOf(triggerContainer), id = stageId, name = stageId)
        stageList.add(stage1)

        // 其他的stage
        yaml.stages.forEach { stage ->
            stageList.add(
                modelStage.createStage(
                    stage = stage,
                    event = event,
                    gitBasicSetting = gitBasicSetting,
                    // stream的stage标号从1开始，后续都加1
                    stageIndex = stageIndex++,
                    resources = yaml.resource,
                    changeSet = changeSet,
                    pipeline = pipeline
                )
            )
        }
        // 添加finally
        if (!yaml.finally.isNullOrEmpty()) {
            stageList.add(
                modelStage.createStage(
                    stage = com.tencent.devops.common.ci.v2.Stage(
                        name = "Finally",
                        id = null,
                        label = emptyList(),
                        ifField = null,
                        fastKill = false,
                        jobs = yaml.finally!!,
                        checkIn = null,
                        checkOut = null
                    ),
                    event = event,
                    gitBasicSetting = gitBasicSetting,
                    stageIndex = stageIndex,
                    finalStage = true,
                    resources = yaml.resource,
                    pipeline = pipeline
                )
            )
        }

        return Model(
            name = GitCIPipelineUtils.genBKPipelineName(gitBasicSetting.gitProjectId),
            desc = "",
            stages = stageList,
            labels = labelList,
            instanceFromTemplate = false,
            pipelineCreator = event.userId
        )
    }

    @Suppress("NestedBlockDepth")
    private fun preparePipelineLabels(
        event: GitRequestEvent,
        gitBasicSetting: GitCIBasicSetting,
        yaml: ScriptBuildYaml
    ): List<String> {
        val gitCIPipelineLabels = mutableListOf<String>()

        try {
            // 获取当前项目下存在的标签组
            val pipelineGroups = client.get(UserPipelineGroupResource::class)
                .getGroups(event.userId, gitBasicSetting.projectCode!!)
                .data

            yaml.label?.forEach {
                // 要设置的标签组不存在，新建标签组和标签（同名）
                if (!checkPipelineLabel(it, pipelineGroups)) {
                    client.get(UserPipelineGroupResource::class).addGroup(
                        event.userId, PipelineGroupCreate(
                            projectId = gitBasicSetting.projectCode!!,
                            name = it
                        )
                    )

                    val pipelineGroup = getPipelineGroup(it, event.userId, gitBasicSetting.projectCode!!)
                    if (pipelineGroup != null) {
                        client.get(UserPipelineGroupResource::class).addLabel(
                            userId = event.userId,
                            projectId = gitBasicSetting.projectCode!!,
                            pipelineLabel = PipelineLabelCreate(
                                groupId = pipelineGroup.id,
                                name = it
                            )
                        )
                    }
                }

                // 保证标签已创建成功后，取label加密ID
                val pipelineGroup = getPipelineGroup(it, event.userId, gitBasicSetting.projectCode!!)
                gitCIPipelineLabels.add(pipelineGroup!!.labels[0].id)
            }
        } catch (e: Exception) {
            logger.warn("${event.userId}|${gitBasicSetting.projectCode!!} preparePipelineLabels error.", e)
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
