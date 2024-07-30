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

package com.tencent.devops.process.yaml.transfer

import com.tencent.devops.common.api.constant.CommonMessageCode.YAML_NOT_VALID
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildContainerType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
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
        (model.stages[0].containers[0] as TriggerContainer).params.forEach {
            if (it.id in ignoredVariable) return@forEach
            val props = when {
                // 不带
                it.type == BuildFormPropertyType.STRING && it.desc.isNullOrEmpty() -> null
                it.type == BuildFormPropertyType.STRING -> VariableProps(
                    type = VariablePropType.VUEX_INPUT.value
                )

                it.type == BuildFormPropertyType.TEXTAREA -> VariableProps(
                    type = VariablePropType.VUEX_TEXTAREA.value
                )

                it.type == BuildFormPropertyType.ENUM -> VariableProps(
                    type = VariablePropType.SELECTOR.value,
                    options = it.options?.map { form ->
                        VariablePropOption(id = form.key, label = form.value)
                    },
                    payload = it.payload
                )

                it.type == BuildFormPropertyType.DATE -> null // not use
                it.type == BuildFormPropertyType.LONG -> null // not use
                it.type == BuildFormPropertyType.BOOLEAN -> VariableProps(
                    type = VariablePropType.BOOLEAN.value
                )

                it.type == BuildFormPropertyType.SVN_TAG -> null // not use
                it.type == BuildFormPropertyType.GIT_REF -> VariableProps(
                    type = VariablePropType.GIT_REF.value,
                    repoHashId = it.repoHashId
                )

                it.type == BuildFormPropertyType.MULTIPLE -> VariableProps(
                    type = VariablePropType.CHECKBOX.value,
                    options = it.options?.map { form ->
                        VariablePropOption(id = form.key, label = form.value)
                    },
                    payload = it.payload
                )

                it.type == BuildFormPropertyType.CODE_LIB -> VariableProps(
                    type = VariablePropType.CODE_LIB.value,
                    scmType = it.scmType?.alis
                )

                it.type == BuildFormPropertyType.CONTAINER_TYPE -> VariableProps(
                    type = VariablePropType.CONTAINER_TYPE.value,
                    containerType = with(it.containerType) {
                        this?.let {
                            BuildContainerTypeYaml(
                                buildType, os
                            )
                        }
                    }
                ) // 构建机类型(公共构建机，第三方构建机，PCG构建机等)
                it.type == BuildFormPropertyType.ARTIFACTORY -> VariableProps(
                    type = VariablePropType.ARTIFACTORY.value,
                    glob = it.glob,
                    properties = it.properties?.ifEmpty { null }
                ) // 版本仓库
                it.type == BuildFormPropertyType.SUB_PIPELINE -> VariableProps(
                    type = VariablePropType.SUB_PIPELINE.value
                ) // 子流水线
                it.type == BuildFormPropertyType.CUSTOM_FILE -> VariableProps(
                    type = VariablePropType.CUSTOM_FILE.value
                ) // 自定义仓库文件
                it.type == BuildFormPropertyType.PASSWORD -> null // not use
                it.type == BuildFormPropertyType.TEMPORARY -> null // not use
                else -> null
            }
            val const = it.constant.nullIfDefault(false)
            result[it.id] = Variable(
                value = it.defaultValue.toString(),
                readonly = if (const == true) null else it.readOnly.nullIfDefault(false),
                allowModifyAtStartup = if (const != true) it.required.nullIfDefault(true) else null,
                const = const,
                props = props
            )

            if (it.name?.isNotEmpty() == true) {
                val p = result[it.id]?.props ?: VariableProps()
                p.label = it.name
            }

            if (it.valueNotEmpty.nullIfDefault(false) != null) {
                val p = result[it.id]?.props ?: VariableProps()
                p.required = it.valueNotEmpty
            }

            if (it.desc.nullIfDefault("") != null) {
                val p = result[it.id]?.props ?: VariableProps()
                p.description = it.desc
            }
        }
        return if (result.isEmpty()) {
            null
        } else {
            result
        }
    }

    fun makeRecommendedVersion(model: Model): RecommendedVersion? {
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val res = if (triggerContainer.buildNo != null) {
            with(triggerContainer.buildNo) {
                RecommendedVersion(
                    enabled = true, allowModifyAtStartup = this!!.required, buildNo = RecommendedVersion.BuildNo(
                        this.buildNo,
                        RecommendedVersion.Strategy.parse(this.buildNoType).alis
                    )
                )
            }
        } else return null

        (model.stages[0].containers[0] as TriggerContainer).params.forEach {
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
                    defaultValue = when (type) {
                        BuildFormPropertyType.BOOLEAN -> variable.value?.toBoolean() ?: false
                        else -> variable.value ?: ""
                    },
                    options = variable.props?.options?.map {
                        BuildFormValue(key = it.id.toString(), value = it.label ?: it.id.toString())
                    },
                    desc = variable.props?.description,
                    repoHashId = variable.props?.repoHashId,
                    relativePath = null,
                    scmType = ScmType.parse(variable.props?.scmType),
                    containerType = with(variable.props?.containerType) {
                        this?.let {
                            BuildContainerType(
                                buildType, os
                            )
                        }
                    },
                    glob = variable.props?.glob,
                    properties = variable.props?.properties,
                    readOnly = if (variable.const == true) true else {
                        variable.readonly ?: false
                    },
                    valueNotEmpty = variable.props?.required ?: false,
                    payload = variable.props?.payload
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
