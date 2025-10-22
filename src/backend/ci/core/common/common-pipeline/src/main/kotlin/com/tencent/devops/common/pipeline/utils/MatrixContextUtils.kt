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

package com.tencent.devops.common.pipeline.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.common.api.util.YamlUtil
import org.yaml.snakeyaml.Yaml

object MatrixContextUtils {

    private const val strategyJsonPattern = "^(\\\\\$\\\\{\\\\{[ ]*fromJSON\\\\()([^(^)]+)(\\\\)[ ]*\\\\}\\\\})$"

    private const val schemaJson = """
{
    "type": "object",
    "required": [],
    "properties": {
        "include": {
            "description": "值格式为：List,用于给 matrix 的指定组合增加额外的属性,或者新增1个或多个组合,每个元素为一个 Object,或是一个'$'{{fromJSON(xxx)}}上下文",
            "oneOf": [
                {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "required": [],
                        "additionalProperties": {
                            "anyOf": [
                                {
                                    "type": "string"
                                },
                                {
                                    "type": "integer"
                                }
                            ]
                        }
                    }
                },
                {
                    "type": "string",
                    "pattern": "$strategyJsonPattern"
                }
            ]
        },
        "exclude": {
            "description": "值格式为：List,用于排除 matrix  中的一些组合,每个元素为一个 Object,或是一个'$'{{fromJSON(xxx)}}上下文",
            "oneOf": [
                {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "required": [],
                        "additionalProperties": {
                            "anyOf": [
                                {
                                    "type": "string"
                                },
                                {
                                    "type": "integer"
                                }
                            ]
                        }
                    }
                },
                {
                    "type": "string",
                    "pattern": "$strategyJsonPattern"
                }
            ]
        },
        "strategy": {
            "description": "值格式为：Object,,或是一个'$'{{fromJSON(xxx)}}上下文,定义的每个选项都有键和值，键将作为 matrix 上下文中的属性",
            "oneOf": [
                {
                    "type": "object",
                    "additionalProperties": {
                        "anyOf": [
                            {
                                "type": "array"
                            },
                            {
                                "type": "string",
                                "pattern": "$strategyJsonPattern"
                            }
                        ]
                    }
                },
                {
                    "type": "string",
                    "pattern": "$strategyJsonPattern"
                }
            ]
        }
    }
}
    """

    private val yaml = ThreadLocal.withInitial {
        Yaml()
    }

    private val schemaFactory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
        .objectMapper(YamlUtil.getObjectMapper())
        .build()

    /**
     * 使用循环遍历笛卡尔乘积算法
     */
    fun loopCartesianProduct(input: List<List<Any>>): List<List<Any>> =
        input.fold(listOf(listOf<Any>())) { acc, nextList ->
            acc.flatMap { list -> nextList.map { element -> list + element } }
        }.toList()

    /**
     * 使用递归遍历实现的笛卡尔乘积算法，因为容易导致堆栈溢出，仅用于单测校验
     */
    fun recursiveCartesianProduct(input: List<List<Any>>): List<List<Any>> {
        val output = mutableListOf<List<Any>>()
        product(input, output, 0, listOf())
        return output
    }

    fun schemaCheck(originYaml: String) {
        if (originYaml.isBlank()) {
            return
        }
        val yamlJson = YamlUtil.getObjectMapper().readTree(YamlUtil.toYaml(yaml.get().load(originYaml))).replaceOn()
        schemaFactory.getSchema(schemaJson).check(yamlJson)
    }

    private fun JsonSchema.check(yaml: JsonNode) {
        validate(yaml).let {
            if (!it.isNullOrEmpty()) {
                throw ExecuteException(it.toString())
            }
        }
    }

    // Yaml规则下会将on当成true在消除锚点时会将On替换为true
    private fun JsonNode.replaceOn(): JsonNode {
        val realOn = get("true") ?: return this
        val node = this as ObjectNode
        node.set<JsonNode>("on", realOn)
        node.remove("true")
        return this
    }

    /**
     * 笛卡尔乘积递归遍历操作:
     * 原二维数组[input], 通过乘积转化后的数组[output],
     * 层级参数[layer], 当前操作数组[currentList]
     */
    private fun product(
        input: List<List<Any>>,
        output: MutableList<List<Any>>,
        layer: Int,
        currentList: List<Any>
    ) {
        if (layer < input.size - 1) {
            if (input[layer].isEmpty()) {
                product(input, output, layer + 1, currentList)
            } else {
                for (i in input[layer].indices) {
                    val list = currentList.toMutableList()
                    list.add(input[layer][i])
                    product(input, output, layer + 1, list)
                }
            }
        } else if (layer == input.size - 1) {
            if (input[layer].isEmpty()) {
                output.add(currentList)
            } else {
                for (i in input[layer].indices) {
                    val list = currentList.toMutableList()
                    list.add(input[layer][i])
                    output.add(list)
                }
            }
        }
    }
}
