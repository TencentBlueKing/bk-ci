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

import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.variable.BuildVariableSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import org.slf4j.LoggerFactory

/**
 * Worker 侧大变量表达式按需加载支持。
 *
 * claim 下发的 variables 中大变量仅为引用串 `__BK_OVF__:<len>`；
 * 当插件入参 / 脚本使用 `${{ key }}` 时，通过 loader 远程拉取真实值。
 */
object BuildVarOverflowExprSupport {

    private val logger = LoggerFactory.getLogger(BuildVarOverflowExprSupport::class.java)
    private val variableApi by lazy { ApiFactory.create(BuildVariableSDKApi::class) }

    private const val VARIABLES_PREFIX = "variables."

    /**
     * @return overflowKeys 与按需 loader；无大变量时 loader 为 null
     */
    fun resolveOverflowOptions(
        variables: Map<String, String>
    ): Pair<Set<String>, ((String) -> String?)?> {
        val overflowKeys = BuildVarOverflowUtils.collectOverflowKeys(variables)
        if (overflowKeys.isEmpty()) {
            return emptySet<String>() to null
        }
        // variables map 里不一定有 BK_CI_PIPELINE_ID（例如部分上下文裁剪场景），
        // 用 LoggerService.buildVariables 兜底，避免 loader 被跳过导致 ${{ }} 仍展开成引用串。
        val pipelineId = variables[PIPELINE_ID]?.takeIf { it.isNotBlank() }
            ?: LoggerService.buildVariables?.pipelineId.orEmpty()
        if (pipelineId.isBlank()) {
            logger.warn("OVERFLOW_LOADER_SKIP|pipelineId blank, overflowKeys=$overflowKeys")
            return overflowKeys to null
        }
        val loader: (String) -> String? = loader@{ key ->
            try {
                // variables.xxx 落库 key 是 xxx
                val loadKey = if (key.startsWith(VARIABLES_PREFIX)) {
                    key.removePrefix(VARIABLES_PREFIX)
                } else {
                    key
                }
                val result = variableApi.getBuildVariableValue(pipelineId = pipelineId, varName = loadKey)
                if (result.isNotOk()) {
                    logger.warn(
                        "OVERFLOW_LOADER_FAIL|key=$key|loadKey=$loadKey|" +
                            "status=${result.status}|message=${result.message}"
                    )
                    return@loader variables[key] ?: variables[loadKey]
                }
                val data = result.data
                if (data == null) {
                    logger.warn("OVERFLOW_LOADER_NULL|key=$key|loadKey=$loadKey")
                    return@loader variables[key] ?: variables[loadKey]
                }
                if (BuildVarOverflowUtils.isOverflowReference(data)) {
                    // process 仍返回引用串：溢出表 miss 或未加载成功
                    logger.warn("OVERFLOW_LOADER_STILL_REF|key=$key|loadKey=$loadKey|data=$data")
                } else {
                    logger.info("OVERFLOW_LOADER_OK|key=$key|loadKey=$loadKey|len=${data.length}")
                }
                data
            } catch (ignore: Throwable) {
                logger.warn("OVERFLOW_LOADER_FAIL|key=$key|${ignore.message}", ignore)
                variables[key] ?: variables[key.removePrefix(VARIABLES_PREFIX)]
            }
        }
        return overflowKeys to loader
    }

    // 仅匹配双花括号 ${{ key }}；单花括号 ${ } / $x 不在此处处理（保持引用串，避免 4M 值意外展开）
    private val doubleBracePattern = Regex("\\$\\{\\{([^{}$]+)}}")
    private const val SENTINEL_PREFIX = "\u0000__BK_OVF_SENTINEL_"
    private const val SENTINEL_SUFFIX = "__\u0000"

    private fun isOverflowToken(key: String, overflowKeys: Set<String>): Boolean {
        if (overflowKeys.isEmpty()) return false
        if (overflowKeys.contains(key)) return true
        if (key.startsWith(VARIABLES_PREFIX) && overflowKeys.contains(key.removePrefix(VARIABLES_PREFIX))) return true
        if (overflowKeys.contains(VARIABLES_PREFIX + key)) return true
        return false
    }

    /**
     * 传统方言(不支持表达式)专用：把文本里命中溢出键的 `${{ key }}` 替换成随机哨兵占位，
     * 并把哨兵 -> 真实值写入 [sink]。调用方在跑完经典替换器后再用 [sink] 还原真实值，
     * 从而避免大值被经典替换器再次解析、也不影响 `${x}` / `$x` 旧语法（它们仍拿到引用串）。
     *
     * 未命中溢出键、或加载失败/仍是引用串的 `${{ }}` 原样保留，交给后续经典替换器处理。
     */
    fun sentinelizeOverflowInText(
        text: String,
        overflowKeys: Set<String>,
        loader: (String) -> String?,
        sink: MutableMap<String, String>
    ): String {
        if (overflowKeys.isEmpty() || !text.contains("\${{")) return text
        return doubleBracePattern.replace(text) { matchResult ->
            val rawKey = matchResult.groupValues[1].trim()
            if (!isOverflowToken(rawKey, overflowKeys)) {
                matchResult.value
            } else {
                val real = try {
                    loader.invoke(rawKey)
                } catch (ignore: Throwable) {
                    logger.warn("OVERFLOW_SENTINEL_FAIL|key=$rawKey|${ignore.message}")
                    null
                }
                if (real == null || BuildVarOverflowUtils.isOverflowReference(real)) {
                    // 加载失败或 process 仍返回引用串：保持原样，行为退化为「输出引用串」
                    matchResult.value
                } else {
                    val sentinel = "$SENTINEL_PREFIX${sink.size}$SENTINEL_SUFFIX"
                    sink[sentinel] = real
                    sentinel
                }
            }
        }
    }

    /**
     * 对任意对象图(String / Map / List)递归执行 [sentinelizeOverflowInText]，返回新对象图，
     * 不修改入参。用于插件入参这类结构化对象的传统方言替换。
     */
    fun sentinelizeOverflowInObject(
        value: Any?,
        overflowKeys: Set<String>,
        loader: (String) -> String?,
        sink: MutableMap<String, String>
    ): Any? = when (value) {
        is String -> sentinelizeOverflowInText(value, overflowKeys, loader, sink)
        is Map<*, *> -> value.entries.associateTo(LinkedHashMap<Any?, Any?>()) { (k, v) ->
            k to sentinelizeOverflowInObject(v, overflowKeys, loader, sink)
        }
        is List<*> -> value.map { sentinelizeOverflowInObject(it, overflowKeys, loader, sink) }
        else -> value
    }

    /**
     * 与 [sentinelizeOverflowInObject] 配对：在经典替换完成后，对对象图里 String 叶子中的哨兵
     * 还原为真实值。在对象层还原(而非 JSON 串上还原)可保证后续 toJson 正确转义大值内容。
     */
    fun restoreSentinelsInObject(value: Any?, sink: Map<String, String>): Any? = when {
        sink.isEmpty() -> value
        value is String -> {
            var text = value
            sink.forEach { (sentinel, real) -> if (text.contains(sentinel)) text = text.replace(sentinel, real) }
            text
        }
        value is Map<*, *> -> value.entries.associateTo(LinkedHashMap<Any?, Any?>()) { (k, v) ->
            k to restoreSentinelsInObject(v, sink)
        }
        value is List<*> -> value.map { restoreSentinelsInObject(it, sink) }
        else -> value
    }
}
