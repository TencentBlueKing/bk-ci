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

package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.compatibility.BuildPropertyCompatibilityTools
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_STRING_LENGTH_MAX
import java.util.regex.Pattern
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory

object PipelineUtils {

    private val logger = LoggerFactory.getLogger(PipelineUtils::class.java)

    private const val ENGLISH_NAME_PATTERN = "[A-Za-z_][A-Za-z_0-9.]*"

    private val PIPELINE_ID_PATTERN = Pattern.compile("(p-)?[a-f\\d]{32}")

    fun checkPipelineName(name: String, maxPipelineNameSize: Int) {
        if (name.toCharArray().size > maxPipelineNameSize) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_TOO_LONG,
                defaultMessage = "Pipeline's name is too long"
            )
        }
    }

    fun checkPipelineParams(params: List<BuildFormProperty>): MutableMap<String, BuildFormProperty> {
        val map = mutableMapOf<String, BuildFormProperty>()
        params.forEach { param ->
            if (!Pattern.matches(ENGLISH_NAME_PATTERN, param.id)) {
                logger.warn("Pipeline's start params[${param.id}] is illegal")
                throw OperationException(
                    message = I18nUtil.getCodeLanMessage(
                        ProcessMessageCode.ERROR_PIPELINE_PARAMS_NAME_ERROR,
                        params = arrayOf(param.id)
                    )
                )
            }
            if (param.constant == true) {
                if (param.defaultValue.toString().isBlank()) throw OperationException(
                    message = I18nUtil.getCodeLanMessage(
                        ProcessMessageCode.ERROR_PIPELINE_CONSTANTS_BLANK_ERROR,
                        params = arrayOf(param.id)
                    )
                )
                // 常量一定不作为入参，且只读不可覆盖
                param.required = false
                param.readOnly = true
            }
            map[param.id] = param
        }
        return map
    }

    fun checkPipelineDescLength(desc: String?, maxPipelineNameSize: Int) {
        if (desc != null && desc.toCharArray().size > maxPipelineNameSize) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_DESC_TOO_LONG,
                defaultMessage = "Pipeline's desc is too long"
            )
        }
    }

    /**
     *  检查stage审核参数是否符合规范
     */
    @Suppress("NestedBlockDepth")
    fun checkStageReviewParam(reviewParams: List<ManualReviewParam>?) {
        reviewParams?.forEach { param ->
            when (param.valueType) {
                ManualReviewParamType.MULTIPLE -> {
                    val value = param.value
                    if (value is List<*>) {
                        value.forEach { checkVariablesLength(param.key, it.toString()) }
                    }
                }

                else -> {
                    checkVariablesLength(param.key, param.value.toString())
                }
            }
        }
    }

    fun transformUserIllegalReviewParams(reviewParams: List<ManualReviewParam>?) {
        reviewParams?.forEach { param ->
            val value = param.value
            if (param.valueType == ManualReviewParamType.MULTIPLE && value is String && value.isBlank()) {
                param.value = null
            }
        }
    }

    private fun checkVariablesLength(key: String, value: String) {
        if (value.length >= PIPELINE_VARIABLES_STRING_LENGTH_MAX) {
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_STAGE_REVIEW_VARIABLES_OUT_OF_LENGTH,
                params = arrayOf(key)
            )
        }
    }

    fun getFixedStages(
        model: Model,
        fixedTriggerContainer: TriggerContainer,
        defaultStageTagId: String?
    ): List<Stage> {
        val stages = ArrayList<Stage>()
        val defaultTagIds = if (defaultStageTagId.isNullOrBlank()) emptyList() else listOf(defaultStageTagId)
        model.stages.forEachIndexed { index, stage ->
            stage.id = stage.id ?: VMUtils.genStageId(index + 1)
            stage.transformCompatibility()
            if (index == 0) {
                stages.add(stage.copy(containers = listOf(fixedTriggerContainer)))
            } else {
                if (stage.name.isNullOrBlank()) stage.name = stage.id
                if (stage.tag == null) stage.tag = defaultTagIds
                stages.add(stage)
            }
        }
        return stages
    }

    /**
     * 将流水线常量转换成模板常量
     */
    fun fixedTemplateParam(model: Model): Model {
        val triggerContainer = model.getTriggerContainer()
        val params = mutableListOf<BuildFormProperty>()
        val templateParams = mutableListOf<BuildFormProperty>()
        triggerContainer.params.forEach {
            if (it.constant == true) {
                templateParams.add(it)
            } else {
                params.add(it)
            }
        }
        val fixedTriggerContainer = triggerContainer.copy(
            params = params,
            templateParams = if (templateParams.isEmpty()) {
                null
            } else {
                templateParams
            }
        )
        val stages = ArrayList<Stage>()
        model.stages.forEachIndexed { index, stage ->
            if (index == 0) {
                stages.add(stage.copy(containers = listOf(fixedTriggerContainer)))
            } else {
                stages.add(stage)
            }
        }
        return model.copy(stages = stages)
    }

    /**
     * 通过流水线参数和模板编排生成新Model
     */
    @Suppress("ALL")
    fun instanceModel(
        templateModel: Model,
        pipelineName: String,
        buildNo: BuildNo?,
        param: List<BuildFormProperty>?,
        instanceFromTemplate: Boolean,
        labels: List<String>? = null,
        defaultStageTagId: String?
    ): Model {
        val templateTrigger = templateModel.getTriggerContainer()
        val instanceParam = if (templateTrigger.templateParams == null) {
            BuildPropertyCompatibilityTools.mergeProperties(templateTrigger.params, param ?: emptyList())
        } else {
            BuildPropertyCompatibilityTools.mergeProperties(
                from = templateTrigger.params,
                to = BuildPropertyCompatibilityTools.mergeProperties(
                    // 模板常量需要变成流水线常量
                    from = templateTrigger.templateParams!!.map { it.copy(constant = true) },
                    to = param ?: emptyList()
                )
            )
        }

        val triggerContainer = TriggerContainer(
            name = templateTrigger.name,
            elements = templateTrigger.elements,
            params = cleanOptions(instanceParam),
            buildNo = buildNo,
            containerId = templateTrigger.containerId,
            containerHashId = templateTrigger.containerHashId
        )

        return Model(
            name = pipelineName,
            desc = "",
            stages = getFixedStages(templateModel, triggerContainer, defaultStageTagId),
            labels = labels ?: templateModel.labels,
            instanceFromTemplate = instanceFromTemplate
        )
    }

    /**
     * 清空options
     *
     * 当参数类型为GIT/SNV分支、代码库、子流水线时,流水线保存、模板保存和模板实例化时,需要清空options参数,减少model大小.
     * options需在运行时实时计算
     */
    fun cleanOptions(params: List<BuildFormProperty>): List<BuildFormProperty> {
        val filterParams = mutableListOf<BuildFormProperty>()
        params.forEach {
            when (it.type) {
                BuildFormPropertyType.SVN_TAG,
                BuildFormPropertyType.GIT_REF,
                BuildFormPropertyType.CODE_LIB,
                BuildFormPropertyType.SUB_PIPELINE,
                BuildFormPropertyType.CONTAINER_TYPE -> {
                    filterParams.add(it.copy(options = emptyList(), replaceKey = null, searchUrl = null))
                }

                else ->
                    filterParams.add(it)
            }
        }
        return filterParams
    }

    fun isPipelineId(pipelineId: String): Boolean {
        return PIPELINE_ID_PATTERN.matcher(pipelineId).matches()
    }
}
