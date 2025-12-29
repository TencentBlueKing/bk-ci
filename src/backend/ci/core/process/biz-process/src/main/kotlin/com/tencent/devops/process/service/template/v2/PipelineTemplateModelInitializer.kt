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

package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.ModelIdDuplicateChecker
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.JobTemplateModel
import com.tencent.devops.common.pipeline.template.StageTemplateModel
import com.tencent.devops.common.pipeline.template.StepTemplateModel
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.service.StageTagService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pipeline模板模型初始化器
 */
@Service
class PipelineTemplateModelInitializer @Autowired constructor(
    private val stageTagService: StageTagService,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val modelContainerIdGenerator: ModelContainerIdGenerator
) {

    fun initTemplateModel(templateModel: ITemplateModel) {
        when (templateModel) {
            is Model -> initModel(templateModel)
            is StageTemplateModel -> initStageTemplate(templateModel)
            is JobTemplateModel -> initJobTemplate(templateModel)
            is StepTemplateModel -> initStepTemplate(templateModel)
            else -> throw IllegalArgumentException("Unknown template type")
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun initModel(templateModel: Model) {
        val defaultStageTagId = stageTagService.getDefaultStageTag().data?.id
        val defaultTagIds = defaultStageTagId?.let { listOf(it) }
        // 去重id
        val distinctIdSet = mutableSetOf<String>()
        // 初始化ID 该构建环境下的ID,旧流水引擎数据无法转换为String，仍然是序号的方式
        val containerSeqId = AtomicInteger(0)
        val jobIdDuplicateChecker = ModelIdDuplicateChecker()

        templateModel.stages.forEachIndexed { index, stage ->
            stage.id = VMUtils.genStageId(index + 1)
            if (stage.name.isNullOrBlank()) stage.name = stage.id
            if (stage.tag == null) stage.tag = defaultTagIds
            stage.containers.forEach { container ->
                val stepIdDuplicateChecker = ModelIdDuplicateChecker()
                if (container is TriggerContainer) {
                    container.params = PipelineUtils.cleanOptions(params = container.params)
                    container.templateParams = container.templateParams?.let {
                        PipelineUtils.cleanOptions(params = it)
                    }
                }
                container.id = containerSeqId.getAndIncrement().toString()
                container.containerId = container.id
                if (container.containerHashId.isNullOrBlank() ||
                    distinctIdSet.contains(container.containerHashId)
                ) {
                    container.containerHashId = modelContainerIdGenerator.getNextId()
                }
                distinctIdSet.add(container.containerHashId!!)
                if (!container.jobId.isNullOrBlank()) {
                    jobIdDuplicateChecker.addId(container.jobId!!)
                }
                container.elements.forEach { e ->
                    if (e.id.isNullOrBlank() || distinctIdSet.contains(e.id)) {
                        e.id = modelTaskIdGenerator.getNextId()
                    }
                    distinctIdSet.add(e.id!!)
                    if (!e.stepId.isNullOrBlank()) {
                        stepIdDuplicateChecker.addId(e.stepId!!)
                    }
                }
                if (stepIdDuplicateChecker.duplicateIdSet.isNotEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_STEP_ID_DUPLICATE,
                        params = arrayOf(container.name, stepIdDuplicateChecker.duplicateIdSet.joinToString(","))
                    )
                }
            }
        }
        if (jobIdDuplicateChecker.duplicateIdSet.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_JOB_ID_DUPLICATE,
                params = arrayOf(jobIdDuplicateChecker.duplicateIdSet.joinToString(","))
            )
        }
    }

    private fun initStageTemplate(templateModel: StageTemplateModel) {
        val defaultStageTagId = stageTagService.getDefaultStageTag().data?.id
        val defaultTagIds = defaultStageTagId?.let { listOf(it) }
        templateModel.stages.forEachIndexed { index, stage ->
            stage.id = stage.id ?: VMUtils.genStageId(index + 1)
            if (stage.name.isNullOrBlank()) stage.name = stage.id
            if (stage.tag == null) stage.tag = defaultTagIds
            stage.containers.forEach { container ->
                if (container is TriggerContainer) {
                    container.params = PipelineUtils.cleanOptions(params = container.params)
                    container.templateParams = container.templateParams?.let {
                        PipelineUtils.cleanOptions(params = it)
                    }
                }
                if (container.containerId.isNullOrBlank()) {
                    container.containerId = container.id
                }
                if (container.containerHashId.isNullOrBlank()) {
                    container.containerHashId = modelContainerIdGenerator.getNextId()
                }
                container.elements.forEach { e ->
                    if (e.id.isNullOrBlank()) {
                        e.id = modelTaskIdGenerator.getNextId()
                    }
                }
            }
        }
    }

    private fun initJobTemplate(templateModel: JobTemplateModel) {
        templateModel.containers.forEach { container ->
            if (container is TriggerContainer) {
                container.params = PipelineUtils.cleanOptions(params = container.params)
                container.templateParams = container.templateParams?.let {
                    PipelineUtils.cleanOptions(params = it)
                }
            }
            if (container.containerId.isNullOrBlank()) {
                container.containerId = container.id
            }
            if (container.containerHashId.isNullOrBlank()) {
                container.containerHashId = modelContainerIdGenerator.getNextId()
            }
            container.elements.forEach { e ->
                if (e.id.isNullOrBlank()) {
                    e.id = modelTaskIdGenerator.getNextId()
                }
            }
        }
    }

    private fun initStepTemplate(templateModel: StepTemplateModel) {
        val container = templateModel.container
        if (container.containerId.isNullOrBlank()) {
            container.containerId = container.id
        }
        if (container.containerHashId.isNullOrBlank()) {
            container.containerHashId = modelContainerIdGenerator.getNextId()
        }
        container.elements.forEach { e ->
            if (e.id.isNullOrBlank()) {
                e.id = modelTaskIdGenerator.getNextId()
            }
        }
    }
}
