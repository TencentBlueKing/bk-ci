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
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.dialect.IPipelineDialect
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
     * 引用到的大变量按需从制品库加载。单个参数替换异常时退化为不加载大变量的替换。
     */
    fun replaceClassicTaskParams(buildTask: BuildTask): BuildTask {
        val params = buildTask.params
        if (params.isNullOrEmpty()) return buildTask
        if (buildTask.replaceVarOnWorker != true) {
            // 引擎侧已替换，这里只做体检：参数看着像 JSON 却解析不了，说明替换在引擎侧就把它拼坏了
            params.forEach { (key, value) -> warnIfJsonUnparsable(key, value) }
            return buildTask
        }
        val variables = buildTask.buildVariable ?: emptyMap()
        // 大变量上下文不可用时只记录告警，仍按普通变量完成替换，不能让整个取任务流程中断
        val (overflowKeys, loader) = try {
            resolveOverflowOptions(variables, params.values)
        } catch (ignore: Throwable) {
            logger.warn("CLASSIC_PARAM_OVERFLOW_INIT_FAIL|${ignore.message}")
            emptySet<String>() to null
        }
        val replaced = params.mapValues { (key, value) ->
            val result = try {
                replaceClassicParam(value, variables, overflowKeys, loader)
            } catch (ignore: Throwable) {
                logger.warn("CLASSIC_PARAM_REPLACE_FAIL|key=$key|${ignore.message}")
                replaceClassicParamFallback(value, variables)
            }
            warnIfJsonBroken(key, value, result)
            warnIfJsonUnparsable(key, result)
            result
        }
        return buildTask.copy(params = replaced)
    }

    /**
     * 任务参数变量替换的统一入口，供 claim 后的传统方言替换与 [TaskUtil.resolveTaskParams] 共用。
     *
     * 两种方言都先把"引擎压缩序列化下发的 Map/List 参数"还原成容器再替换，变量值只会落到字符串叶子上，
     * 最后由 Jackson 统一序列化转义；否则大变量或普通变量的值里只要带 `{` `"`，就会把外层参数 JSON 拼坏。
     * 非容器文本（脚本等）保持原字符串替换语义不变。
     */
    fun replaceTaskParam(
        value: String,
        variables: Map<String, String>,
        dialect: IPipelineDialect,
        overflowKeys: Set<String>,
        loader: ((String) -> String?)?
    ): String {
        if (!dialect.supportUseExpression()) {
            return replaceClassicParam(value, variables, overflowKeys, loader)
        }
        return EnvReplacementParser.parse(
            value = restoreContainerParam(value),
            contextMap = variables,
            dialect = dialect,
            overflowKeys = overflowKeys,
            overflowLoader = loader
        )
    }

    /**
     * 替换前是合法 JSON、替换后不再是,说明变量值被未转义地拼进了参数文本。
     * 插件(如 MarketAtomTask 解析 data)后续解析必然失败,这里提前把现场打出来便于定位。
     */
    private fun warnIfJsonBroken(key: String, origin: String, result: String) {
        if (result === origin || parseJsonContainer(origin) == null || parseJsonContainer(result) != null) return
        logger.warn(
            "CLASSIC_PARAM_JSON_BROKEN|key=$key|originLength=${origin.length}|resultLength=${result.length}" +
                "|origin=${origin.take(PARAM_LOG_LIMIT)}|result=${result.take(PARAM_LOG_LIMIT)}"
        )
    }

    /**
     * 参数以 `{` / `[` 开头结尾却解析不了，插件按 JSON 读取时必然失败。
     * 日志里带上出错位置附近的片段，便于直接看出是哪个变量值把引号提前闭合了。
     */
    private fun warnIfJsonUnparsable(key: String, value: String) {
        val trimmed = value.trim()
        val looksLikeJson = (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
        if (!looksLikeJson) return
        try {
            JsonUtil.to(trimmed, mapOrListTypeRef(trimmed))
            return
        } catch (ignore: Throwable) {
            logger.warn(
                "CLASSIC_PARAM_JSON_UNPARSABLE|key=$key|length=${value.length}|${ignore.message}" +
                    "|head=${value.take(PARAM_LOG_LIMIT)}"
            )
        }
    }

    private fun mapOrListTypeRef(trimmed: String): TypeReference<*> =
        if (trimmed.startsWith("{")) mapTypeRef else listTypeRef

    private fun replaceClassicParam(
        value: String,
        variables: Map<String, String>,
        overflowKeys: Set<String>,
        loader: ((String) -> String?)?
    ): String {
        val origin: Any = restoreContainerParam(value)
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

    private fun replaceClassicParamFallback(
        value: String,
        variables: Map<String, String>
    ): String {
        return try {
            val origin = parseJsonContainer(value) ?: value
            val replaced = ObjectReplaceEnvVarUtil.replaceEnvVar(origin, variables) ?: return value
            replaced as? String ?: JsonUtil.toJson(replaced, formatted = false)
        } catch (ignore: Throwable) {
            // 保留原参数比输出被拼坏的 JSON 更安全，后续插件仍可按引用串或原表达式处理。
            logger.warn("CLASSIC_PARAM_REPLACE_FALLBACK_FAIL|${ignore.message}")
            value
        }
    }

    /**
     * 还原"引擎压缩序列化下发的 Map/List 参数"为可变容器；其余情况返回原字符串。
     *
     * 逐字比对不可省：JSON 文本型的**字符串**参数由替换器按带缩进格式输出，
     * 若把它当成容器压缩输出，结果就会与引擎侧不一致。
     */
    private fun restoreContainerParam(value: String): Any {
        val container = parseJsonContainer(value) ?: return value
        if (JsonUtil.toJson(container, formatted = false) == value) return container
        // 能解析成容器却与压缩序列化结果不一致，说明是 JSON 文本型字符串参数，按字符串路径处理
        logger.info("PARAM_NOT_COMPRESSED_CONTAINER|length=${value.length}")
        return value
    }

    private fun parseJsonContainer(value: String): Any? {
        val trimmed = value.trim()
        val isObject = trimmed.startsWith("{") && trimmed.endsWith("}")
        val isArray = trimmed.startsWith("[") && trimmed.endsWith("]")
        // validateJson 不可省：Jackson 默认允许尾部多余内容，`{"a":1}{"b":2}` 会被静默截断成第一个对象
        if ((!isObject && !isArray) || !JsonSchemaUtil.validateJson(trimmed)) return null
        return try {
            if (isObject) JsonUtil.to(trimmed, mapTypeRef) else JsonUtil.to(trimmed, listTypeRef)
        } catch (ignore: Throwable) {
            null
        }
    }

    // 参数可能很大，排障日志只截取开头，避免刷爆构建机日志
    private const val PARAM_LOG_LIMIT = 1000

    private val mapTypeRef = object : TypeReference<LinkedHashMap<String, Any?>>() {}
    private val listTypeRef = object : TypeReference<ArrayList<Any?>>() {}
}
