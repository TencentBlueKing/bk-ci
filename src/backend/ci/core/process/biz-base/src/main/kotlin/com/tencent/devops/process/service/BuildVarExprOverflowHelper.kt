/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
    10| *
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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import java.util.UUID
import org.slf4j.LoggerFactory

/**
 * 引擎侧表达式求值：从变量 Map 组装 overflowKeys + 按需 loader。
 * 无大变量时 loader 为 null，调用方可直接传给 [com.tencent.devops.common.pipeline.EnvReplacementParser]。
 */
object BuildVarExprOverflowHelper {

    private val logger = LoggerFactory.getLogger(BuildVarExprOverflowHelper::class.java)

    private const val VARIABLES_PREFIX = "variables."

    // 仅匹配双花括号 ${{ key }}；单花括号 ${ } / $x 不在此处处理（保持引用串，避免 4M 值意外展开）
    private val doubleBracePattern = Regex("\\$\\{\\{([^{}$]+)}}")

    fun options(
        buildVariableService: BuildVariableService,
        projectId: String,
        buildId: String,
        variables: Map<String, String>
    ): Pair<Set<String>, ((String) -> String?)?> {
        val overflowKeys = BuildVarOverflowUtils.collectOverflowKeys(variables)
        if (overflowKeys.isEmpty()) {
            return emptySet<String>() to null
        }
        val loader: (String) -> String? = { key ->
            buildVariableService.getVariableValue(projectId, buildId, key)
        }
        return overflowKeys to loader
    }

    /**
     * 传统方言(不走表达式引擎)在引擎侧 claim 时对任务参数做变量替换的入口。
     *
     * 与直接调用 [ObjectReplaceEnvVarUtil.replaceEnvVar] 的差异**仅**在于：被 `${{ key }}` 引用到、
     * 且命中溢出键的大变量会被替换成真实值(按需从溢出表加载)；`${x}` / `$x` 旧语法、未被引用的大变量、
     * 其它普通变量一律保持原有行为不变。无大变量或未命中时，行为与直接 replaceEnvVar 完全一致。
     *
     * 实现：把命中的 `${{ overflowKey }}` 改写成 `${{ 合成唯一键 }}`，并把该键的真实值放进一份变量副本，
     * 让 [ObjectReplaceEnvVarUtil.replaceEnvVar] 像处理**普通变量**一样完成替换。真实值因此走与普通变量
     * 完全一致的 JSON 转义 / 反序列化路径，任意上下文(纯字符串、内嵌 JSON 串)都不会被破坏。
     *
     * 内存安全：只对**本次参数文本里实际出现**的 `${{ }}` 溢出键触发加载，且同键只加载一次。
     */
    fun replaceEnvVarWithOverflow(
        value: Any?,
        variables: Map<String, String>,
        buildVariableService: BuildVariableService,
        projectId: String,
        buildId: String
    ): Any? {
        val overflowKeys = BuildVarOverflowUtils.collectOverflowKeys(variables)
        if (overflowKeys.isEmpty()) {
            return ObjectReplaceEnvVarUtil.replaceEnvVar(value, variables)
        }
        val loader: (String) -> String? = { key ->
            val loadKey = if (key.startsWith(VARIABLES_PREFIX)) key.removePrefix(VARIABLES_PREFIX) else key
            buildVariableService.getVariableValue(projectId, buildId, loadKey)
        }
        // 合成键用纯小写字母数字 + 随机 run token：不含 `${}` 可被 EnvUtils 正常识别，且不会与真实变量名冲突。
        val runToken = UUID.randomUUID().toString().replace("-", "")
        val synthVars = LinkedHashMap<String, String>()
        val keyToSynth = HashMap<String, String>()
        val rewritten = rewriteOverflow(value, overflowKeys, loader, synthVars, keyToSynth, runToken)
        if (synthVars.isEmpty()) {
            // 没有任何 ${{ }} 命中大变量真实值：与原逻辑等价，直接替换（用原始 value，避免多余对象拷贝）
            return ObjectReplaceEnvVarUtil.replaceEnvVar(value, variables)
        }
        // 仅追加合成键，原变量表原样保留；${x}/$x、其它变量的替换行为完全不变
        val effectiveVars = HashMap(variables).apply { putAll(synthVars) }
        logger.info("CLASSIC_OVERFLOW_RESOLVED|buildId=$buildId|count=${synthVars.size}")
        return ObjectReplaceEnvVarUtil.replaceEnvVar(rewritten, effectiveVars)
    }

    private fun isOverflowToken(key: String, overflowKeys: Set<String>): Boolean {
        val loadKey = if (key.startsWith(VARIABLES_PREFIX)) key.removePrefix(VARIABLES_PREFIX) else key
        return overflowKeys.contains(key) ||
            overflowKeys.contains(loadKey) ||
            overflowKeys.contains(VARIABLES_PREFIX + loadKey)
    }

    private fun rewriteText(
        text: String,
        overflowKeys: Set<String>,
        loader: (String) -> String?,
        synthVars: MutableMap<String, String>,
        keyToSynth: MutableMap<String, String>,
        runToken: String
    ): String {
        if (!text.contains("\${{")) return text
        return doubleBracePattern.replace(text) { matchResult ->
            val rawKey = matchResult.groupValues[1].trim()
            if (!isOverflowToken(rawKey, overflowKeys)) {
                matchResult.value
            } else {
                val existing = keyToSynth[rawKey]
                if (existing != null) {
                    "\${{$existing}}"
                } else {
                    val real = try {
                        loader.invoke(rawKey)
                    } catch (ignore: Throwable) {
                        logger.warn("CLASSIC_OVERFLOW_LOAD_FAIL|key=$rawKey|${ignore.message}")
                        null
                    }
                    if (real == null || BuildVarOverflowUtils.isOverflowReference(real)) {
                        // 加载失败或仍是引用串：保持原样，行为退化为「输出引用串」
                        matchResult.value
                    } else {
                        val synth = "bkovf${runToken}n${keyToSynth.size}"
                        keyToSynth[rawKey] = synth
                        synthVars[synth] = real
                        "\${{$synth}}"
                    }
                }
            }
        }
    }

    private fun rewriteOverflow(
        value: Any?,
        overflowKeys: Set<String>,
        loader: (String) -> String?,
        synthVars: MutableMap<String, String>,
        keyToSynth: MutableMap<String, String>,
        runToken: String
    ): Any? = when (value) {
        is String -> rewriteText(value, overflowKeys, loader, synthVars, keyToSynth, runToken)
        is Map<*, *> -> value.entries.associateTo(LinkedHashMap<Any?, Any?>()) { (k, v) ->
            k to rewriteOverflow(v, overflowKeys, loader, synthVars, keyToSynth, runToken)
        }
        is List<*> -> value.map { rewriteOverflow(it, overflowKeys, loader, synthVars, keyToSynth, runToken) }
        else -> value
    }
}
