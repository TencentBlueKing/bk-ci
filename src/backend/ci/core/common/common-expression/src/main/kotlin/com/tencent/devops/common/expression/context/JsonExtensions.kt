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

package com.tencent.devops.common.expression.context

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode

@Suppress("NestedBlockDepth")
object JsonExtensions {

    fun JsonNode.toPipelineContextData(): PipelineContextData? {
        return toPipelineContextData(1, 100)
    }

    fun JsonNode?.toPipelineContextData(
        depth: Int,
        maxDepth: Int
    ): PipelineContextData? {
        val value = this
        if (depth < maxDepth) {
            return when (value?.nodeType) {
                JsonNodeType.STRING -> StringContextData(value.textValue())

                JsonNodeType.BOOLEAN -> BooleanContextData(value.booleanValue())

                JsonNodeType.NUMBER -> NumberContextData(value.doubleValue())

                JsonNodeType.OBJECT -> DictionaryContextData().also { dict ->
                    value as ObjectNode
                    value.fieldNames().forEach { name ->
                        val v = value.get(name)
                        dict[name] = v.toPipelineContextData(depth + 1, maxDepth)
                    }
                }

                JsonNodeType.ARRAY -> ArrayContextData().apply {
                    value as ArrayNode
                    value.forEach { node ->
                        add(node.toPipelineContextData(depth + 1, maxDepth))
                    }
                }

                JsonNodeType.NULL -> null

                // We don't understand the type
                else -> StringContextData(value.toString())
            }
        }

        // have reached our max, return as string
        return StringContextData(value.toString())
    }
}
