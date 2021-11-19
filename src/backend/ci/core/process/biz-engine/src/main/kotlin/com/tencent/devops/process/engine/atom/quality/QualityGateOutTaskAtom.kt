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

package com.tencent.devops.process.engine.atom.quality

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineBuildQualityService
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Suppress("UNUSED")
@Component
class QualityGateOutTaskAtom @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val templateService: TemplateService,
    private val pipelineBuildQualityService: PipelineBuildQualityService
) : IAtomTask<QualityGateOutElement> {

    override fun getParamElement(task: PipelineBuildTask): QualityGateOutElement {
        return JsonUtil.mapTo(task.taskParams, QualityGateOutElement::class.java)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: QualityGateOutElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        return pipelineBuildQualityService.tryFinish(task, buildLogPrinter)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: QualityGateOutElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val checkResult = pipelineBuildQualityService.getCheckResult(
            task = task,
            interceptTaskName = param.interceptTaskName,
            interceptTask = param.interceptTask,
            runVariables = runVariables,
            buildLogPrinter = buildLogPrinter,
            position = ControlPointPosition.AFTER_POSITION,
            templateId = templateService.getTemplateIdByPipeline(task.pipelineId)
        )

        return pipelineBuildQualityService.handleResult(
            position = ControlPointPosition.AFTER_POSITION,
            task = task,
            interceptTask = param.interceptTask!!,
            checkResult = checkResult,
            buildLogPrinter = buildLogPrinter
        )
    }
}
