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

package com.tencent.devops.worker.common.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonSchemaUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import org.slf4j.LoggerFactory

/**
 * Worker 侧大变量表达式按需加载支持。
 *
 * 下发给构建机的变量中，大变量仅为引用串 `__BK_OVF__:<len>:<executeCount>:<sha256>`；
 * 当插件入参 / 脚本使用 `${{ key }}` 时，按引用串从制品库下载真实值（[LargeVariableStore]）。
 */
object BuildVarOverflowExprSupport {

    private val logger = LoggerFactory.getLogger(BuildVarOverflowExprSupport::class.java)

    /**
     * @param prefetchTexts 即将被替换的文本；其中通过 `${{ }}` 引用到的大变量会被批量预取
     * @return overflowKeys 与按需 loader；无大变量或制品库上下文不可用时 loader 为 null
     */
    fun resolveOverflowOptions(
        variables: Map<String, String>,
        prefetchTexts: Collection<String> = emptyList()
    ): Pair<Set<String>, ((String) -> String?)?> {
        val overflowKeys = BuildVarOverflowUtils.collectOverflowKeys(variables)
        if (overflowKeys.isEmpty()) {
            return emptySet<String>() to null
        }
        val scope = LargeVariableStore.currentScope()
        if (scope == null) {
            logger.warn("OVERFLOW_LOADER_SKIP|build context not ready, overflowKeys=${overflowKeys.size}")
            return overflowKeys to null
        }
        val referenced = BuildVarOverflowUtils.findReferencedOverflowKeys(prefetchTexts, overflowKeys)
        if (referenced.size > 1) {
            LargeVariableStore.prefetch(scope, referenced.mapNotNull { variables[it] })
        }
        val loader: (String) -> String? = { key ->
            BuildVarOverflowUtils.resolveStoredKey(key, variables)
                ?.let { variables[it] }
                ?.let { reference -> LargeVariableStore.load(scope, reference) ?: reference }
        }
        return overflowKeys to loader
    }

    /**
     * 经典方言脚本/文本的 ${{ 大变量 }} 按需加载。
     *
     * 重写逻辑复用引擎/Worker 共享实现 [BuildVarOverflowUtils.rewriteOverflowRefs]：把被 `${{ key }}`
     * 引用到的大变量重写成 `${{ 合成key }}`，调用方把返回的 合成key -> 真实值 映射并入替换上下文，
     * 交给 [ReplacementUtils] 统一替换（其 Matcher.quoteReplacement 负责 $ \ 转义）。
     *
     * @return Pair(重写后的文本, 合成变量映射)；无需处理时返回原文本与空映射
     */
    fun rewriteOverflowText(
        text: String,
        overflowKeys: Set<String>,
        loader: ((String) -> String?)?
    ): Pair<String, Map<String, String>> {
        if (loader == null || overflowKeys.isEmpty() || !text.contains("\${{")) {
            return text to emptyMap()
        }
        val result = BuildVarOverflowUtils.rewriteOverflowRefs(text, overflowKeys, loader)
        if (result.synthVars.isNotEmpty()) {
            logger.info("OVERFLOW_SCRIPT_RESOLVED|count=${result.synthVars.size}")
        }
        return (result.value as String) to result.synthVars
    }

    /**
     * 引擎声明未替换传统方言参数时（[BuildTask.replaceVarOnWorker]），在构建机上完成与引擎
     * `EngineVMBuildService.claim` 等价的替换：Map/List 参数按结构递归替换后压缩输出，字符串参数按文本替换；
     * 引用到的大变量按需从制品库加载。单个参数替换异常时退化为不加载大变量的文本替换。
     */
    fun replaceClassicTaskParams(buildTask: BuildTask): BuildTask {
        if (buildTask.replaceVarOnWorker != true) return buildTask
        val params = buildTask.params
        if (params.isNullOrEmpty()) return buildTask
        val variables = buildTask.buildVariable ?: emptyMap()
        val (overflowKeys, loader) = resolveOverflowOptions(variables, params.values)
        val replaced = params.mapValues { (key, value) ->
            try {
                replaceClassicParam(value, variables, overflowKeys, loader)
            } catch (ignore: Throwable) {
                logger.warn("CLASSIC_PARAM_REPLACE_FAIL|key=$key|${ignore.message}")
                ObjectReplaceEnvVarUtil.replaceEnvVar(value, variables).toString()
            }
        }
        return buildTask.copy(params = replaced)
    }

    private fun replaceClassicParam(
        value: String,
        variables: Map<String, String>,
        overflowKeys: Set<String>,
        loader: ((String) -> String?)?
    ): String {
        // 引擎对 Map/List 参数按结构替换后压缩输出、对字符串参数（含 JSON 文本）按文本替换；
        // Map/List 参数下发时即为同一 ObjectMapper 的压缩序列化结果，据此还原其原始类型
        val origin: Any = parseJsonContainer(value)
            ?.takeIf { JsonUtil.toJson(it, formatted = false) == value }
            ?: value
        val (target, context) = if (loader != null && value.contains("\${{")) {
            val result = BuildVarOverflowUtils.rewriteOverflowRefs(origin, overflowKeys, loader)
            val context = if (result.synthVars.isEmpty()) variables else variables.plus(result.synthVars)
            result.value to context
        } else {
            origin to variables
        }
        val replaced = ObjectReplaceEnvVarUtil.replaceEnvVar(target, context) ?: return value
        return replaced as? String ?: JsonUtil.toJson(replaced, formatted = false)
    }

    private fun parseJsonContainer(value: String): Any? {
        val trimmed = value.trim()
        val isObject = trimmed.startsWith("{") && trimmed.endsWith("}")
        val isArray = trimmed.startsWith("[") && trimmed.endsWith("]")
        if ((!isObject && !isArray) || !JsonSchemaUtil.validateJson(trimmed)) return null
        return try {
            if (isObject) JsonUtil.to(trimmed, mapTypeRef) else JsonUtil.to(trimmed, listTypeRef)
        } catch (ignore: Throwable) {
            null
        }
    }

    private val mapTypeRef = object : TypeReference<LinkedHashMap<String, Any?>>() {}
    private val listTypeRef = object : TypeReference<ArrayList<Any?>>() {}
}
