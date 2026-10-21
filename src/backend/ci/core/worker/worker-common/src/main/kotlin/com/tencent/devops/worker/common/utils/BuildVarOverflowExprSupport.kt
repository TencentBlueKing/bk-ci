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
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectReader
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
            params.forEach { (key, value) -> warnIfJsonBroken(key, value, value) }
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
     * 替换结果体检：参数看着像 JSON 却解析不了，说明变量值被未转义地拼进了参数文本，
     * 插件（如 MarketAtomTask 解析 data）后续解析必然失败，这里提前把现场打出来。
     *
     * 只有在**结果已经坏掉**时才回头解析原值判定责任方，正常构建下每个参数至多多一次解析。
     *
     * 与 [BuildTask.toString] 一样不打印参数内容：任务参数可能含敏感信息，也可能很大。
     * Jackson 的报错消息本身已带出错行列且不含原文（`INCLUDE_SOURCE_IN_LOCATION` 关闭），足够定位。
     */
    private fun warnIfJsonBroken(key: String, origin: String, result: String) {
        val error = jsonParseError(result) ?: return
        val stage = if (jsonParseError(origin) != null) "CLAIM" else "WORKER"
        logger.warn(
            "CLASSIC_PARAM_JSON_BROKEN|stage=$stage|key=$key|originLength=${origin.length}" +
                "|resultLength=${result.length}|$error"
        )
    }

    /** @return 解析失败原因；不像 JSON 或能正常解析时返回 null */
    private fun jsonParseError(value: String): String? {
        val trimmed = value.trim()
        val isObject = trimmed.startsWith("{") && trimmed.endsWith("}")
        val isArray = trimmed.startsWith("[") && trimmed.endsWith("]")
        if (!isObject && !isArray) return null
        return try {
            strictReader(isObject).readValue<Any?>(trimmed)
            null
        } catch (ignore: Throwable) {
            ignore.message ?: ignore.javaClass.name
        }
    }

    private fun replaceClassicParam(
        value: String,
        variables: Map<String, String>,
        overflowKeys: Set<String>,
        loader: ((String) -> String?)?
    ): String {
        // compressed 必须在替换前算：ObjectReplaceEnvVarUtil 会原地修改容器
        val param = asContainerParam(value)
        val origin: Any = param?.node ?: value
        val (target, context) = if (loader != null && value.contains("\${{")) {
            val result = BuildVarOverflowUtils.rewriteOverflowRefs(origin, overflowKeys, loader)
            val context = if (result.synthVars.isEmpty()) variables else variables.plus(result.synthVars)
            (result.value ?: origin) to context
        } else {
            origin to variables
        }
        val replaced = ObjectReplaceEnvVarUtil.replaceEnvVar(target, context) ?: return value
        if (param == null) return replaced as? String ?: JsonUtil.toJson(replaced, formatted = false)
        // 引擎压缩下发的 Map/List 参数按压缩输出；JSON 文本型字符串参数按缩进输出，
        // 与 ObjectReplaceEnvVarUtil 处理 JSON 字符串时的 toJson(dataObj, true) 保持一致
        return JsonUtil.toJson(replaced, formatted = !param.compressed)
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
     * @property node      解析出的可变容器
     * @property compressed 是否与「引擎压缩序列化」的结果逐字相同。
     *   为 true 说明这是引擎按 Map/List 下发的参数；为 false 说明它只是**内容恰好是 JSON 文本的字符串参数**
     *   （引擎对字符串参数原样下发，不会重新序列化），两者的输出格式不同，必须区分。
     */
    private class ContainerParam(val node: Any, val compressed: Boolean)

    private fun asContainerParam(value: String): ContainerParam? {
        val node = parseJsonContainer(value) ?: return null
        return ContainerParam(node, JsonUtil.toJson(node, formatted = false) == value)
    }

    /**
     * 表达式方言用：仅还原「引擎压缩下发的 Map/List 参数」为容器，其余情况返回原字符串，
     * 保持 [EnvReplacementParser] 对文本参数的历史替换语义不变。
     */
    private fun restoreContainerParam(value: String): Any {
        val param = asContainerParam(value) ?: return value
        return if (param.compressed) param.node else value
    }

    /**
     * 解析 JSON 容器参数。
     *
     * 必须与 [JsonUtil]（插件读取入参所用）保持同一套解析口径，不能用 `JsonSchemaUtil.validateJson`：
     * 后者基于 fge 的严格解析器，会拒绝字符串值里的**裸控制字符**（换行/制表符），而 [JsonUtil] 显式开启了
     * `ALLOW_UNESCAPED_CONTROL_CHARS`。两者不一致时，本该按结构替换的参数会退化成文本拼接，
     * 把变量值原样拼进 JSON，插件再解析必然失败。
     *
     * `FAIL_ON_TRAILING_TOKENS` 不可省：默认配置下 `{"a":1}{"b":2}` 会被静默截断成第一个对象。
     */
    private fun parseJsonContainer(value: String): Any? {
        val trimmed = value.trim()
        val isObject = trimmed.startsWith("{") && trimmed.endsWith("}")
        val isArray = trimmed.startsWith("[") && trimmed.endsWith("]")
        if (!isObject && !isArray) return null
        return try {
            strictReader(isObject).readValue(trimmed)
        } catch (ignore: Throwable) {
            null
        }
    }

    private fun strictReader(isObject: Boolean) = if (isObject) mapReader else listReader

    // ObjectReader 不可变且线程安全，复用即可；不能直接改 JsonUtil 的共享 mapper 配置
    private val mapReader: ObjectReader = strictReaderFor(object : TypeReference<LinkedHashMap<String, Any?>>() {})
    private val listReader: ObjectReader = strictReaderFor(object : TypeReference<ArrayList<Any?>>() {})

    private fun strictReaderFor(typeRef: TypeReference<*>): ObjectReader =
        JsonUtil.getObjectMapper().readerFor(typeRef).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
}
