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

package com.tencent.devops.common.pipeline

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.context.RuntimeNamedValue
import com.tencent.devops.common.expression.expression.ExpressionOutput
import com.tencent.devops.common.expression.expression.IFunctionInfo
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.common.pipeline.dialect.IPipelineDialect
import com.tencent.devops.common.pipeline.utils.ExprReplacementUtil
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

@Suppress(
    "LoopWithTooManyJumpStatements",
    "ComplexCondition",
    "ComplexMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "LongParameterList"
)
object EnvReplacementParser {

    private val logger = LoggerFactory.getLogger(EnvReplacementParser::class.java)
    private val expressionPattern = Pattern.compile("\\$[{]{2}([^$^{}]+)[}]{2}")

    /**
     * 根据环境变量map进行object处理并保持原类型
     * @param value 等待进行环境变量替换的对象，可以是任意类型
     * @param contextMap 环境变量map值
     * @param contextPair 自定义表达式计算上下文（如果指定则不使用表达式替换或默认替换逻辑）
     * @param onlyExpression 只进行表达式替换（若指定了自定义替换逻辑此字段无效，为false）
     * @param functions 用户自定义的拓展用函数
     * @param output 表达式计算时输出
     */
    fun parse(
        value: Any?,
        contextMap: Map<String, String>,
        onlyExpression: Boolean? = false,
        contextPair: Pair<ExecutionContext, List<NamedValueInfo>>? = null,
        functions: Iterable<IFunctionInfo>? = null,
        output: ExpressionOutput? = null
    ): String {
        val options = ExprReplacementOptions(
            contextMap = contextMap,
            contextPair = contextPair,
            functions = functions,
            output = output
        )
        return parse(value = value, onlyExpression = onlyExpression, options = options)
    }

    /**
     * 根据环境变量map进行object处理并保持原类型
     * 根据方言的配置判断是否能够使用${}
     */
    fun parse(
        value: Any?,
        contextMap: Map<String, String>,
        dialect: IPipelineDialect,
        contextPair: Pair<ExecutionContext, List<NamedValueInfo>>? = null,
        functions: Iterable<IFunctionInfo>? = null,
        output: ExpressionOutput? = null
    ): String {
        val options = ExprReplacementOptions(
            contextMap = contextMap,
            contextPair = contextPair,
            functions = functions,
            output = output
        )
        return parse(
            value = value,
            onlyExpression = dialect.supportUseExpression(),
            options = options
        )
    }

    fun parse(
        value: Any?,
        onlyExpression: Boolean?,
        options: ExprReplacementOptions
    ): String {
        if (value == null) return ""
        return if (onlyExpression == true) {
            ExprReplaceEnvVarUtil.replaceEnvVar(value, options)
        } else {
            ObjectReplaceEnvVarUtil.replaceEnvVar(value, options.contextMap)
        }.let {
            JsonUtil.toJson(it, false)
        }
    }

    fun getCustomExecutionContextByMap(
        variables: Map<String, String>,
        extendNamedValueMap: List<RuntimeNamedValue>? = null
    ): Pair<ExecutionContext, List<NamedValueInfo>>? {
        return ExprReplacementUtil.getCustomExecutionContextByMap(
            variables = variables,
            extendNamedValueMap = extendNamedValueMap
        )
    }

    fun containsExpressions(value: String?): Boolean {
        if (value == null) return false
        return expressionPattern.matcher(value).find()
    }
}
