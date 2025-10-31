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

package com.tencent.devops.process.yaml.transfer

import com.tencent.devops.common.api.constant.CommonMessageCode.YAML_NOT_VALID
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildContainerType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.pipeline.utils.CascadePropertyUtils
import com.tencent.devops.process.utils.FIXVERSION
import com.tencent.devops.process.utils.MAJORVERSION
import com.tencent.devops.process.utils.MINORVERSION
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.v3.models.BuildContainerTypeYaml
import com.tencent.devops.process.yaml.v3.models.RecommendedVersion
import com.tencent.devops.process.yaml.v3.models.Variable
import com.tencent.devops.process.yaml.v3.models.VariablePropOption
import com.tencent.devops.process.yaml.v3.models.VariablePropType
import com.tencent.devops.process.yaml.v3.models.VariableProps
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@Suppress("ComplexMethod")
class VariableTransfer {

    companion object {
        private val logger = LoggerFactory.getLogger(VariableTransfer::class.java)
        private val ignoredVariable =
            listOf(MAJORVERSION, "MajorVersion", MINORVERSION, "MinorVersion", FIXVERSION, "FixVersion")
    }

    fun makeVariableFromModel(model: Model): Map<String, Variable>? {
        val result = mutableMapOf<String, Variable>()
        model.getTriggerContainer().params.forEach {
            if (it.id in ignoredVariable) return@forEach
            var props = when {
                // 字符串
                it.type == BuildFormPropertyType.STRING -> VariableProps(
                    type = VariablePropType.VUEX_INPUT.value
                )
                // 文本框
                it.type == BuildFormPropertyType.TEXTAREA -> VariableProps(
                    type = VariablePropType.VUEX_TEXTAREA.value
                )
                // 布尔值
                it.type == BuildFormPropertyType.BOOLEAN -> VariableProps(
                    type = VariablePropType.BOOLEAN.value
                )
                // 单选框
                it.type == BuildFormPropertyType.ENUM -> VariableProps(
                    type = VariablePropType.SELECTOR.value,
                    options = it.options?.map { form ->
                        VariablePropOption(id = form.key, label = form.value)
                    },
                    payload = it.payload
                )
                // 复选框
                it.type == BuildFormPropertyType.MULTIPLE -> VariableProps(
                    type = VariablePropType.CHECKBOX.value,
                    options = it.options?.map { form ->
                        VariablePropOption(id = form.key, label = form.value)
                    },
                    payload = it.payload
                )
                // SVN分支或TAG
                it.type == BuildFormPropertyType.SVN_TAG -> VariableProps(
                    type = VariablePropType.SVN_TAG.value,
                    repoHashId = it.repoHashId,
                    relativePath = it.relativePath
                )
                // GIT分支或TAG
                it.type == BuildFormPropertyType.GIT_REF -> VariableProps(
                    type = VariablePropType.GIT_REF.value,
                    repoHashId = it.repoHashId
                )
                // 代码库和分支
                CascadePropertyUtils.supportCascadeParam(it.type) -> {
                    // 级联选择器类型变量
                    VariableProps(
                        type = VariablePropType.REPO_REF.value
                    )
                }
                // 代码库
                it.type == BuildFormPropertyType.CODE_LIB -> VariableProps(
                    type = VariablePropType.CODE_LIB.value,
                    scmType = it.scmType?.alis
                )
                // 构建资源
                it.type == BuildFormPropertyType.CONTAINER_TYPE -> VariableProps(
                    type = VariablePropType.CONTAINER_TYPE.value,
                    containerType = with(it.containerType) {
                        this?.let {
                            BuildContainerTypeYaml(
                                buildType, os
                            )
                        }
                    }
                )
                // 版本仓库过滤器
                it.type == BuildFormPropertyType.ARTIFACTORY -> VariableProps(
                    type = VariablePropType.ARTIFACTORY.value,
                    glob = it.glob,
                    properties = it.properties?.ifEmpty { null }
                )
                // 子流水线
                it.type == BuildFormPropertyType.SUB_PIPELINE -> VariableProps(
                    type = VariablePropType.SUB_PIPELINE.value
                )
                // 文件
                it.type == BuildFormPropertyType.CUSTOM_FILE -> VariableProps(
                    type = VariablePropType.CUSTOM_FILE.value,
                    versionControl = it.enableVersionControl.nullIfDefault(false)
                )
                // not use
                it.type == BuildFormPropertyType.PASSWORD -> VariableProps(
                    type = VariablePropType.VUEX_INPUT.value
                )
                // not use
                it.type == BuildFormPropertyType.TEMPORARY -> VariableProps(
                    type = VariablePropType.VUEX_INPUT.value
                )
                // not use
                it.type == BuildFormPropertyType.DATE -> VariableProps(
                    type = VariablePropType.VUEX_INPUT.value
                )
                // not use
                it.type == BuildFormPropertyType.LONG -> VariableProps(
                    type = VariablePropType.VUEX_INPUT.value
                )

                else -> null
            }
            val const = it.constant.nullIfDefault(false)

            if (it.name?.isNotEmpty() == true) {
                props = props ?: VariableProps()
                props.label = it.name
            }

            if (it.valueNotEmpty.nullIfDefault(false) != null) {
                props = props ?: VariableProps()
                props.required = it.valueNotEmpty
            }

            if (it.desc.nullIfDefault("") != null) {
                props = props ?: VariableProps()
                props.description = it.desc
            }

            if (it.category.nullIfDefault("") != null) {
                props = props ?: VariableProps()
                props.group = it.category
            }
            result[it.id] = Variable(
                value = if (CascadePropertyUtils.supportCascadeParam(it.type)) {
                    CascadePropertyUtils.parseDefaultValue(it.id, it.defaultValue, it.type)
                } else {
                    it.defaultValue.toString()
                },
                readonly = if (const == true) null else it.readOnly.nullIfDefault(false),
                allowModifyAtStartup = if (const != true) it.required.nullIfDefault(true) else null,
                const = const,
                props = if (props?.empty() == false) props else null,
                ifCondition = it.displayCondition?.ifEmpty { null }
            )
        }
        return if (result.isEmpty()) {
            null
        } else {
            result
        }
    }

    fun makeRecommendedVersion(model: Model): RecommendedVersion? {
        val triggerContainer = model.getTriggerContainer()
        val res = triggerContainer.buildNo?.let {
            RecommendedVersion(
                enabled = true,
                allowModifyAtStartup = it.required,
                buildNo = RecommendedVersion.BuildNo(
                    it.buildNo, RecommendedVersion.Strategy.parse(it.buildNoType).alis
                )
            )
        } ?: return null

        triggerContainer.params.forEach {
            if (it.id == MAJORVERSION || it.id == "MajorVersion") {
                res.major = it.defaultValue.toString().toIntOrNull() ?: 0
            }

            if (it.id == MINORVERSION || it.id == "MinorVersion") {
                res.minor = it.defaultValue.toString().toIntOrNull() ?: 0
            }

            if (it.id == FIXVERSION || it.id == "FixVersion") {
                res.fix = it.defaultValue.toString().toIntOrNull() ?: 0
            }
        }
        return res
    }

    fun makeVariableFromYaml(
        variables: Map<String, Variable>?
    ): List<BuildFormProperty> {
        if (variables.isNullOrEmpty()) {
            return emptyList()
        }
        val buildFormProperties = mutableListOf<BuildFormProperty>()
        variables.forEach { (key, variable) ->
            val type = VariablePropType.findType(variable.props?.type)?.toBuildFormPropertyType()
                ?: BuildFormPropertyType.STRING
            check(key, variable)
            buildFormProperties.add(
                BuildFormProperty(
                    id = key,
                    name = variable.props?.label,
                    required = variable.allowModifyAtStartup ?: true,
                    constant = variable.const ?: false,
                    type = type,
                    defaultValue = when {
                        type == BuildFormPropertyType.BOOLEAN ->
                            (variable.value as String?)?.toBoolean() ?: false

                        CascadePropertyUtils.supportCascadeParam(type) ->
                            variable.value ?: mapOf<String, String>()

                        else -> variable.value ?: ""
                    },
                    options = variable.props?.options?.map {
                        BuildFormValue(key = it.id.toString(), value = it.label ?: it.id.toString())
                    },
                    desc = variable.props?.description,
                    category = variable.props?.group,
                    repoHashId = variable.props?.repoHashId,
                    relativePath = variable.props?.relativePath,
                    scmType = ScmType.parse(variable.props?.scmType),
                    containerType = with(variable.props?.containerType) {
                        this?.let {
                            BuildContainerType(
                                buildType, os
                            )
                        }
                    },
                    enableVersionControl = variable.props?.versionControl,
                    glob = variable.props?.glob,
                    properties = variable.props?.properties,
                    readOnly = if (variable.const == true) true else {
                        variable.readonly ?: false
                    },
                    valueNotEmpty = variable.props?.required ?: false,
                    payload = variable.props?.payload,
                    displayCondition = variable.ifCondition ?: emptyMap()
                )
            )
        }
        return buildFormProperties
    }

    private fun check(key: String, variable: Variable) {
        if (key.length > 64) {
            throw PipelineTransferException(
                YAML_NOT_VALID,
                arrayOf("variable key no more than 64 characters. variable: $key")
            )
        }

        if (variable.const == true && variable.readonly == false) {
            throw PipelineTransferException(
                YAML_NOT_VALID,
                arrayOf("When the const attribute is set to true, readonly must be true. variable: $key")
            )
        }
        if (variable.const == true && variable.allowModifyAtStartup != null) {
            throw PipelineTransferException(
                YAML_NOT_VALID,
                arrayOf(
                    "The const attribute and the allow-modify-at-startup attribute are mutually exclusive. " +
                        "If configured at the same time, the verification will fail. variable: $key"
                )
            )
        }

        if (variable.const == true && variable.readonly == null) {
            variable.readonly = true
        }
    }
}
