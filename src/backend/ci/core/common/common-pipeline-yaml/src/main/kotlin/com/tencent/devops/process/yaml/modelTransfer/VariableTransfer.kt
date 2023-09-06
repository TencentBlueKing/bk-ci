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

package com.tencent.devops.process.yaml.modelTransfer

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.v3.models.Variable
import com.tencent.devops.process.yaml.v3.models.VariablePropOption
import com.tencent.devops.process.yaml.v3.models.VariablePropType
import com.tencent.devops.process.yaml.v3.models.VariableProps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class VariableTransfer @Autowired constructor() {

    companion object {
        private val logger = LoggerFactory.getLogger(VariableTransfer::class.java)
    }

    fun makeVariableFromModel(model: Model): Map<String, Variable>? {
        val result = mutableMapOf<String, Variable>()
        (model.stages[0].containers[0] as TriggerContainer).params.forEach {
            val props = when {
                // 不带
                it.type == BuildFormPropertyType.STRING && it.desc.isNullOrEmpty() -> null
                it.type == BuildFormPropertyType.STRING -> VariableProps(
                    type = VariablePropType.VUEX_INPUT.value,
                    description = it.desc.nullIfDefault("")
                )

                it.type == BuildFormPropertyType.TEXTAREA -> VariableProps(
                    type = VariablePropType.VUEX_TEXTAREA.value,
                    description = it.desc.nullIfDefault("")
                )

                it.type == BuildFormPropertyType.ENUM -> VariableProps(
                    type = VariablePropType.SELECTOR.value,
                    description = it.desc.nullIfDefault(""),
                    options = it.options?.map { form ->
                        VariablePropOption(id = form.value, label = form.key)
                    },
                    payload = it.payload
                )

                it.type == BuildFormPropertyType.DATE -> null // not use
                it.type == BuildFormPropertyType.LONG -> null // not use
                it.type == BuildFormPropertyType.BOOLEAN -> VariableProps(
                    type = VariablePropType.BOOLEAN.value,
                    description = it.desc.nullIfDefault("")
                )

                it.type == BuildFormPropertyType.SVN_TAG -> null // not use
                it.type == BuildFormPropertyType.GIT_REF -> VariableProps(
                    type = VariablePropType.GIT_REF.value,
                    repoHashId = it.repoHashId,
                    description = it.desc.nullIfDefault("")
                )

                it.type == BuildFormPropertyType.MULTIPLE -> VariableProps(
                    type = VariablePropType.CHECKBOX.value,
                    description = it.desc.nullIfDefault(""),
                    options = it.options?.map { form ->
                        VariablePropOption(id = form.value, label = form.key)
                    },
                    payload = it.payload
                )

                it.type == BuildFormPropertyType.CODE_LIB -> VariableProps(
                    type = VariablePropType.CODE_LIB.value,
                    scmType = it.scmType?.alis,
                    description = it.desc.nullIfDefault("")
                )

                it.type == BuildFormPropertyType.CONTAINER_TYPE -> VariableProps(
                    type = VariablePropType.CONTAINER_TYPE.value,
                    containerType = it.containerType,
                    description = it.desc.nullIfDefault("")
                ) // 构建机类型(公共构建机，第三方构建机，PCG构建机等)
                it.type == BuildFormPropertyType.ARTIFACTORY -> VariableProps(
                    type = VariablePropType.ARTIFACTORY.value,
                    glob = it.glob,
                    properties = it.properties?.ifEmpty { null },
                    description = it.desc.nullIfDefault("")
                ) // 版本仓库
                it.type == BuildFormPropertyType.SUB_PIPELINE -> VariableProps(
                    type = VariablePropType.SUB_PIPELINE.value,
                    description = it.desc.nullIfDefault("")
                ) // 子流水线
                it.type == BuildFormPropertyType.CUSTOM_FILE -> VariableProps(
                    type = VariablePropType.CUSTOM_FILE.value,
                    description = it.desc.nullIfDefault("")
                )  // 自定义仓库文件
                it.type == BuildFormPropertyType.PASSWORD -> null // not use
                it.type == BuildFormPropertyType.TEMPORARY -> null // not use
                else -> null
            }
            result[it.id] = Variable(
                it.defaultValue.toString(),
                readonly = it.readOnly.nullIfDefault(false),
                allowModifyAtStartup = it.required.nullIfDefault(true),
                valueNotEmpty = it.valueNotEmpty.nullIfDefault(false),
                props = props
            )
        }
        return if (result.isEmpty()) {
            null
        } else {
            result
        }
    }

    fun makeVariableFromYaml(
        variables: Map<String, Variable>?
    ): List<BuildFormProperty> {
        if (variables.isNullOrEmpty()) {
            return emptyList()
        }
        val buildFormProperties = mutableListOf<BuildFormProperty>()
        variables.forEach { (key, variable) ->
            buildFormProperties.add(
                BuildFormProperty(
                    id = key,
                    required = variable.allowModifyAtStartup ?: true,
                    type = VariablePropType.findType(variable.props?.type)?.toBuildFormPropertyType()
                        ?: BuildFormPropertyType.STRING,
                    defaultValue = variable.value ?: "",
                    options = variable.props?.options?.map {
                        BuildFormValue(
                            key = it.label ?: it.id.toString(),
                            value = it.id.toString()
                        )
                    },
                    desc = variable.props?.description,
                    repoHashId = variable.props?.repoHashId,
                    relativePath = null,
                    scmType = ScmType.parse(variable.props?.scmType),
                    containerType = variable.props?.containerType,
                    glob = variable.props?.glob,
                    properties = variable.props?.properties,
                    readOnly = variable.readonly,
                    valueNotEmpty = variable.valueNotEmpty
                )
            )
        }
        return buildFormProperties
    }
}
