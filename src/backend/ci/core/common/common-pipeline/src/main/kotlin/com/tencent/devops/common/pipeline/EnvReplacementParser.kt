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

package com.tencent.devops.common.pipeline

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.KeyReplacement
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.RuntimeDictionaryContextData
import com.tencent.devops.common.expression.context.RuntimeNamedValue
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import org.slf4j.LoggerFactory

object EnvReplacementParser {

    private val logger = LoggerFactory.getLogger(EnvReplacementParser::class.java)

    /**
     * 根据环境变量map进行object处理并保持原类型
     * @param obj 等待进行环境变量替换的对象，可以是任意类型
     * @param contextMap 环境变量map值
     * @param replacement 自定义替换逻辑（如果指定则不使用表达式替换或默认替换逻辑）
     * @param onlyExpression 只进行表达式替换（若指定了自定义替换逻辑此字段无效，为false）
     */
    fun <T : Any?> parse(
        obj: T,
        contextMap: Map<String, String>,
        onlyExpression: Boolean? = false,
        replacement: KeyReplacement? = null
    ): T {
        val realReplacement = replacement ?: if (onlyExpression == true) {
            // #7115 如果出现无法表达式解析则保持原文
            object : KeyReplacement {
                override fun getReplacement(key: String): String? {
                    return try {
                        ExpressionParser.evaluateByMap(key, contextMap, true)?.let {
                            JsonUtil.toJson(it, false)
                        }
                    } catch (ignore: ExpressionParseException) {
                        logger.warn("[$onlyExpression] Expression evaluation failed: ", ignore)
                        null
                    }
                }
            }
        } else {
            object : KeyReplacement {
                override fun getReplacement(key: String) = contextMap[key]
            }
        }
        return ObjectReplaceEnvVarUtil.replaceEnvVar(obj, contextMap, realReplacement) as T
    }

    fun getCustomReplacementByMap(
        variables: Map<String, String>,
        extendNamedValueMap: List<RuntimeNamedValue>? = null
    ): KeyReplacement {
        val context = ExecutionContext(DictionaryContextData())
        val nameValue = mutableListOf<NamedValueInfo>()
        extendNamedValueMap?.forEach { namedValue ->
            nameValue.add(NamedValueInfo(namedValue.key, ContextValueNode()))
            context.expressionValues.add(
                namedValue.key,
                RuntimeDictionaryContextData(namedValue)
            )
        }
        ExpressionParser.fillContextByMap(variables, context, nameValue)
        return object : KeyReplacement {
            override fun getReplacement(key: String): String? {
                return try {
                    ExpressionParser.evaluateByContext(key, context, nameValue, true)?.let {
                        JsonUtil.toJson(it, false)
                    }
                } catch (ignore: ExpressionParseException) {
                    logger.warn("Expression evaluation failed: ", ignore)
                    null
                }
            }
        }
    }
}
