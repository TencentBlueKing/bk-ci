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

package com.tencent.devops.plugin.quality.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.quality.QualityGateInElement
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class QualityGateInTaskAtom @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : IAtomTask<QualityGateInElement> {
    override fun getParamElement(task: PipelineBuildTask): QualityGateInElement {
        return JsonUtil.mapTo(task.taskParams, QualityGateInElement::class.java)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: QualityGateInElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        return QualityUtils.tryFinish(task, rabbitTemplate)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: QualityGateInElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val checkResult = QualityUtils.getCheckResult(
            task = task,
            interceptTaskName = param.interceptTaskName,
            interceptTask = param.interceptTask,
            runVariables = runVariables,
            client = client,
            rabbitTemplate = rabbitTemplate,
            position = ControlPointPosition.BEFORE_POSITION
        )

        return QualityUtils.handleResult(
            position = ControlPointPosition.BEFORE_POSITION,
            task = task,
            interceptTask = param.interceptTask!!,
            checkResult = checkResult,
            client = client,
            rabbitTemplate = rabbitTemplate,
            pipelineEventDispatcher = pipelineEventDispatcher
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityGateInTaskAtom::class.java)
    }
}
