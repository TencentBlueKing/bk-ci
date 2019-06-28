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

package com.tencent.devops.process.engine.atom

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_ATOM_NOT_FOUND
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.store.api.atom.ServiceMarketAtomEnvResource
import org.springframework.amqp.rabbit.core.RabbitTemplate

object AtomUtils {

    fun <T> parseAtomBeanName(task: Class<T>): String {
        val taskAtomClass = task.simpleName
        return taskAtomClass[0].toLowerCase() + taskAtomClass.substring(1)
    }

    /**
     * 解析出Container中的市场插件，如果市场插件相应版本找不到就抛出异常
     */
    fun parseContainerMarketAtom(
        container: Container,
        task: PipelineBuildTask,
        client: Client,
        rabbitTemplate: RabbitTemplate
    ): MutableMap<String, String> {
        val atoms = mutableMapOf<String, String>()
        val serviceMarketAtomEnvResource = client.get(ServiceMarketAtomEnvResource::class)
        container.elements.forEach { element ->
            if (element is MarketBuildAtomElement || element is MarketBuildLessAtomElement) {
                var version = element.version
                if (version.isBlank()) {
                    version = "1.*"
                }
                val atomCode = element.getAtomCode()
                val atomEnvResult = serviceMarketAtomEnvResource.getAtomEnv(task.projectId, atomCode, version)
                val atomEnv = atomEnvResult.data
                if (atomEnvResult.isNotOk() || atomEnv == null) {
                    val message = "Can not found task($atomCode):${element.name}| ${atomEnvResult.message}"
                    throw BuildTaskException(ERROR_ATOM_NOT_FOUND, message, task.pipelineId, task.buildId, task.taskId)
                }

                LogUtils.addLine(
                    rabbitTemplate = rabbitTemplate,
                    buildId = task.buildId,
                    message = "Prepare ${element.name}(${atomEnv.atomName})",
                    tag = task.taskId,
                    executeCount = task.executeCount ?: 1
                )
                atoms[atomCode] = atomEnv.projectCode!!
            }
        }
        return atoms
    }
}
