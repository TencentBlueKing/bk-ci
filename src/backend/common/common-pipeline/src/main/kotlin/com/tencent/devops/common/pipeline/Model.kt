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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

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
    @ApiModelProperty("源模版ID", required = false)
    var srcTemplateId: String? = null
) {

    /**
     * 删除相关插件
     */
    fun removeElements(elementClassTypes: Set<String>): Model {
        val stageList = mutableListOf<Stage>()
        stages.forEach { stage ->

            val containerList = mutableListOf<Container>()
            stage.containers.forEach { container ->

                val elementList = container.elements.filterNot { elementClassTypes.contains(it.getClassType()) }
                val finalContainer = when (container) {
                    is VMBuildContainer -> {
                        VMBuildContainer(
                            containerId = container.containerId,
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
                            canRetry = container.canRetry,
                            enableExternal = container.enableExternal,
                            jobControlOption = container.jobControlOption,
                            mutexGroup = container.mutexGroup,
                            dispatchType = container.dispatchType
                        )
                    }
                    is NormalContainer -> {
                        NormalContainer(
                            containerId = container.containerId,
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
                            mutexGroup = container.mutexGroup
                        )
                    }
                    else -> {
                        container
                    }
                }
                containerList.add(finalContainer)
            }
            stageList.add(Stage(containerList, stage.id))
        }

        return Model(name, desc, stageList, labels, instanceFromTemplate, pipelineCreator)
    }

    fun getContainer(vmSeqId: String): Container? {
        stages.forEachIndexed { index, stage ->
            if (index == 0) {
                return@forEachIndexed
            }
            stage.containers.forEach { container ->
                if (container.id == vmSeqId) {
                    return container
                }
            }
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
}
