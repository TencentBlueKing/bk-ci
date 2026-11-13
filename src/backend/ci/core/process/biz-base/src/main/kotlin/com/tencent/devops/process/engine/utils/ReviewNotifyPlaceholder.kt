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

import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.dialect.IPipelineDialect
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.utils.PipelineVarUtil

/**
 * 审核通知发出前，用 [EnvReplacementParser] 把正文和参数里的占位符换成当前构建变量。
 * 变量表先经 [PipelineVarUtil] 补上 ci.* 与 variables.*。解析失败或找不到变量时保留原文。
 */
object ReviewNotifyPlaceholder {

    private val preservedKeys = setOf("reviewUrl", "reviewAppUrl")

    fun replace(params: MutableMap<String, String>, variables: Map<String, String>) {
        if (params.isEmpty() || variables.isEmpty()) return
        val lookup = lookupMap(variables)
        val dialect = PipelineDialectUtil.getPipelineDialect(lookup[PIPELINE_DIALECT])
        val contextPair = EnvReplacementParser.getCustomExecutionContextByMap(lookup)
        params.keys.toList().forEach { key ->
            if (key in preservedKeys) return@forEach
            val raw = params[key] ?: return@forEach
            if (!raw.contains("\${")) return@forEach
            params[key] = parse(raw, lookup, dialect, contextPair)
        }
    }

    fun parseText(raw: String, variables: Map<String, String>): String {
        if (!raw.contains("\${") || variables.isEmpty()) return raw
        val lookup = lookupMap(variables)
        return parse(
            raw = raw,
            lookup = lookup,
            dialect = PipelineDialectUtil.getPipelineDialect(lookup[PIPELINE_DIALECT]),
            contextPair = EnvReplacementParser.getCustomExecutionContextByMap(lookup)
        )
    }

    /**
     * 同时放入旧变量名、ci.* 上下文名，以及 variables.xxx，让 ${var} 和 ${{variables.var}} 都能命中。
     */
    fun lookupMap(variables: Map<String, String>): Map<String, String> {
        val mixed = PipelineVarUtil.mixOldVarAndNewVar(LinkedHashMap(variables))
        return PipelineVarUtil.fillVariableMap(PipelineVarUtil.fillContextVarMap(mixed))
    }

    private fun parse(
        raw: String,
        lookup: Map<String, String>,
        dialect: IPipelineDialect,
        contextPair: Pair<ExecutionContext, List<NamedValueInfo>>?
    ): String {
        val parsed = runCatching {
            EnvReplacementParser.parse(
                value = raw,
                contextMap = lookup,
                dialect = dialect,
                contextPair = contextPair
            )
        }.getOrElse { return raw }
        if (!dialect.supportUseExpression() || !parsed.contains("\${")) {
            return parsed
        }
        // 表达式方言只处理 ${{}}，剩下的 ${var} 再按普通变量替换，仍找不到则保留原文。
        return runCatching {
            EnvReplacementParser.parse(
                value = parsed,
                contextMap = lookup,
                onlyExpression = false,
                contextPair = contextPair
            )
        }.getOrElse { parsed }
    }
}
