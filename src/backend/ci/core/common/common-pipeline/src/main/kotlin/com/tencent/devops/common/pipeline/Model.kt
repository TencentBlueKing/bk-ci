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

package com.tencent.devops.common.pipeline

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.PipelineCallbackEvent
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
@ApiModel("流水线模型-创建信息")
data class Model(
    @ApiModelProperty("名称", required = true)
    var name: String,
    @ApiModelProperty("描述", required = false)
    var desc: String?,
    @ApiModelProperty("阶段集合", required = true)
    val stages: List<Stage>,
    @ApiModelProperty("标签", required = false)
    var labels: List<String> = emptyList(),
    @ApiModelProperty("是否从模板中实例化出来的", required = false)
    val instanceFromTemplate: Boolean? = null,
    @ApiModelProperty("创建人", required = false)
    var pipelineCreator: String? = null,
    @ApiModelProperty("当前模板对应的被复制的模板或安装的研发商店的模板对应的ID", required = false)
    var srcTemplateId: String? = null,
    @ApiModelProperty("当前模板的ID", required = false)
    var templateId: String? = null,
    @ApiModelProperty("提示", required = false)
    var tips: String? = null,
    @ApiModelProperty("流水线事件回调", required = false)
    var events: Map<String, PipelineCallbackEvent>? = emptyMap(),
    @ApiModelProperty("静态流水线组", required = false)
    var staticViews: List<String> = emptyList(),
    @ApiModelProperty("各项耗时", required = true)
    var timeCost: BuildRecordTimeCost? = null
) {
    @ApiModelProperty("提交时流水线最新版本号", required = false)
    var latestVersion: Int = 0

    /**
     * 删除相关原子
     */
    fun removeElements(elementClassTypes: Set<String>): Model {
        val stageList = mutableListOf<Stage>()
        stages.forEach { stage ->

            val containerList = mutableListOf<Container>()
            stage.containers.forEach { container ->

                val elementList = container.elements
                    .filterNot { elementClassTypes.contains(it.getClassType()) }
                val finalContainer = when (container) {
                    is VMBuildContainer -> {
                        VMBuildContainer(
                            containerId = container.containerId,
                            containerHashId = container.containerHashId,
                            id = container.id,
                            name = container.name,
                            elements = elementList,
                            status = container.status,
                            startEpoch = container.startEpoch,
                            systemElapsed = container.systemElapsed,
                            elementElapsed = container.elementElapsed,
                            baseOS = container.baseOS,
                            vmNames = container.vmNames,
                            maxQueueMinutes = container.maxQueueMinutes,
                            maxRunningMinutes = container.maxRunningMinutes,
                            buildEnv = container.buildEnv,
                            customBuildEnv = container.customBuildEnv,
                            thirdPartyAgentId = container.thirdPartyAgentId,
                            thirdPartyAgentEnvId = container.thirdPartyAgentEnvId,
                            thirdPartyWorkspace = container.thirdPartyWorkspace,
                            dockerBuildVersion = container.dockerBuildVersion,
                            tstackAgentId = container.tstackAgentId,
                            canRetry = container.canRetry,
                            enableExternal = container.enableExternal,
                            jobControlOption = container.jobControlOption,
                            mutexGroup = container.mutexGroup,
                            dispatchType = container.dispatchType,
                            showBuildResource = container.showBuildResource,
                            jobId = container.jobId
                        )
                    }

                    is NormalContainer -> {
                        NormalContainer(
                            containerId = container.containerId,
                            containerHashId = container.containerHashId,
                            id = container.id,
                            name = container.name,
                            elements = elementList,
                            status = container.status,
                            startEpoch = container.startEpoch,
                            systemElapsed = container.systemElapsed,
                            elementElapsed = container.elementElapsed,
                            enableSkip = container.enableSkip,
                            conditions = container.conditions,
                            canRetry = container.canRetry,
                            jobControlOption = container.jobControlOption,
                            mutexGroup = container.mutexGroup,
                            jobId = container.jobId
                        )
                    }

                    else -> {
                        container
                    }
                }
                containerList.add(finalContainer)
            }
            stageList.add(
                Stage(
                    containers = containerList,
                    id = stage.id,
                    name = stage.name,
                    tag = stage.tag,
                    status = stage.status,
                    startEpoch = stage.startEpoch,
                    elapsed = stage.elapsed,
                    customBuildEnv = stage.customBuildEnv,
                    fastKill = stage.fastKill,
                    stageControlOption = stage.stageControlOption
                )
            )
        }

        return Model(
            name = name,
            desc = desc,
            stages = stageList,
            labels = labels,
            instanceFromTemplate = instanceFromTemplate,
            pipelineCreator = pipelineCreator,
            srcTemplateId = null,
            templateId = templateId
        )
    }

    fun getContainer(vmSeqId: String): Container? {
        stages.forEachIndexed { index, stage ->
            if (index == 0) {
                return@forEachIndexed
            }
            return stage.getContainer(vmSeqId) ?: return@forEachIndexed
        }
        return null
    }

    fun getStage(stageId: String): Stage? {
        stages.forEach { stage ->
            if (stage.id == stageId) return stage
        }
        return null
    }

    fun taskCount(skipTaskClassType: Set<String> = emptySet()): Int {
        var count = 0
        stages.forEach { s ->
            s.containers.forEach { c ->
                c.elements.forEach { e ->
                    if (!skipTaskClassType.contains(e.getClassType())) {
                        count++
                    }
                }
            }
        }
        return count
    }

    fun getPipelineCallBack(projectId: String, callbackEvent: CallBackEvent): List<ProjectPipelineCallBack> {
        val pipelineCallBack = mutableListOf<ProjectPipelineCallBack>()
        events?.forEach { eventName, event ->
            if (event.callbackEvent == callbackEvent) {
                pipelineCallBack.add(
                    ProjectPipelineCallBack(
                        id = null,
                        projectId = projectId,
                        events = event.callbackEvent.name,
                        callBackUrl = event.callbackUrl,
                        secretToken = event.secretToken
                    )
                )
            }
        }
        return pipelineCallBack
    }
}
