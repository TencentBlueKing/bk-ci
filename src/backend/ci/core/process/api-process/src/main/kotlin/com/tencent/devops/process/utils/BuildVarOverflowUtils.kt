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

package com.tencent.devops.process.utils

import java.util.UUID
import org.slf4j.LoggerFactory

/**
 * 大变量"引用协议"工具。
 *
 * 主表 T_PIPELINE_BUILD_VAR.VALUE 长度受限于 varchar(4000)。
 * 当真实值超过该长度时：
 *  - 真实值进入 T_PIPELINE_BUILD_VAR_OVERFLOW（mediumtext，~16M 容量）；
 *  - 主表只保留一条**纯引用**：`__BK_OVF__:<originalLength>`；
 *  - 主表不再保留任何"截断摘要"内容，避免给现网带来无意义的字节占用与
 *    敏感数据泄露风险。
 *
 * 引用串只承载两种信息：
 *  - 是否大变量（是否带哨兵前缀）；
 *  - 原始字符长度（便于 UI/日志/告警显示，无需再二次查询溢出表）。
 *
 * 大变量真实值仅在 ${{ xxx }} 表达式按需求值时通过
 * [com.tencent.devops.process.engine.dao.PipelineBuildVarOverflowDao] 拉取，
 * `$xxx` / `${xxx}` 旧风格固定看到引用串本身。
 */
object BuildVarOverflowUtils {

    private val logger = LoggerFactory.getLogger(BuildVarOverflowUtils::class.java)

    /** variables.xxx 落库 key 是 xxx，判断/加载时需去掉该前缀。 */
    private const val VARIABLES_PREFIX = "variables."

    /**
     * 仅匹配双花括号 `${{ key }}`；key 内不含 `$ { }`。
     * 单花括号 `${ }` / `$x` 旧语法不在此处处理（保持引用串，避免 4M 值意外展开）。
     */
    private val doubleBracePattern = Regex("\\$\\{\\{([^{}$]+)}}")

    /** 主表 VALUE 列字符上限，与 [PIPELINE_VARIABLES_STRING_LENGTH_MAX] 对齐。 */
    const val MAIN_TABLE_MAX_LENGTH: Int = PIPELINE_VARIABLES_STRING_LENGTH_MAX

    /** 哨兵前缀，用于在主表 VALUE 列识别"是否大变量引用"。 */
    const val OVERFLOW_PREFIX: String = PIPELINE_VARIABLES_OVERFLOW_PREFIX

    /** 大变量值硬上限（字符数），超过则拒绝写入。 */
    const val HARD_MAX_LENGTH: Int = PIPELINE_VARIABLES_STRING_LENGTH_HARD_MAX

    /**
     * 判断主表 VALUE 是否为"大变量引用"。
     *
     * 用户主动构造该前缀的概率极低；即使发生，按需加载时溢出表查询为 null，
     * `$xxx`/`${xxx}` 风格仍直接看到这串字符，行为退化为"与改造前一致"。
     */
    fun isOverflowReference(value: String?): Boolean {
        return value != null && value.startsWith(OVERFLOW_PREFIX)
    }

    /**
     * 计算主表 VALUE 列实际写入的字符串。
     *  - rawValue 长度 ≤ [MAIN_TABLE_MAX_LENGTH]：直接返回原值；
     *  - 否则：返回纯引用串 `__BK_OVF__:<rawValue.length>`，
     *    其最大长度 = OVERFLOW_PREFIX.length + 10（Int 最大 10 位）≈ 25，
     *    主表 varchar(4000) 列恒能容纳。
     */
    fun toMainTableValue(rawValue: String): String {
        return if (rawValue.length <= MAIN_TABLE_MAX_LENGTH) rawValue else "$OVERFLOW_PREFIX${rawValue.length}"
    }

    /** 是否需要将真实值写入溢出表。 */
    fun shouldOverflow(rawValue: String): Boolean = rawValue.length > MAIN_TABLE_MAX_LENGTH

    /**
     * 从变量 Map 中收集大变量引用键（value 为 `__BK_OVF__:<len>`）。
     * 供引擎 / Worker 在调用 [com.tencent.devops.common.pipeline.EnvReplacementParser] 前组装 overflowKeys。
     */
    fun collectOverflowKeys(variables: Map<String, String>): Set<String> {
        if (variables.isEmpty()) return emptySet()
        return variables.asSequence()
            .filter { isOverflowReference(it.value) }
            .map { it.key }
            .toSet()
    }

    /**
     * 合成键溢出重写的结果。
     * @property value    重写后的值（结构与入参一致：String / Map / List / 其它原样）
     * @property synthVars 合成键 -> 大变量真实值 映射；调用方需并入替换上下文
     */
    data class OverflowRewriteResult(
        val value: Any?,
        val synthVars: Map<String, String>
    )

    /**
     * 传统方言 `${{ 大变量 }}` 按需加载的**唯一共享实现**（引擎 claim 侧与 Worker 侧共用）。
     *
     * 把被 `${{ key }}` 引用到、且命中溢出键的大变量重写成 `${{ 合成唯一键 }}`，并把
     * 合成键 -> 真实值 收进 [OverflowRewriteResult.synthVars]；调用方将其并入替换上下文，交由各自的
     * 替换引擎（引擎侧 ObjectReplaceEnvVarUtil / Worker 脚本侧 ReplacementUtils）把合成键当**普通变量**
     * 完成替换，从而复用各引擎的 JSON 转义 / quoteReplacement，任意上下文都不会被破坏。
     *
     * 语义约束（两侧一致）：
     * - 仅双花括号 `${{ }}` 且命中溢出键才触发加载；同键只加载一次（去重）；
     * - `${x}` / `$x` 旧语法、未被引用的大变量、其它普通变量：完全不动；
     * - loader 返回 null 或仍是引用串（溢出表 miss / 加载失败）：保持原 token 不变，
     *   行为退化为"输出引用串"，与历史一致。
     *
     * @param value        待处理值，支持 String 及任意 Map/List 嵌套对象图
     * @param overflowKeys [collectOverflowKeys] 收集到的溢出键；为空时直接原样返回
     * @param loader       按落库 key 拉取真实值；由调用方决定远程/本地及兜底策略
     */
    fun rewriteOverflowRefs(
        value: Any?,
        overflowKeys: Set<String>,
        loader: (String) -> String?
    ): OverflowRewriteResult {
        if (overflowKeys.isEmpty()) {
            return OverflowRewriteResult(value, emptyMap())
        }
        val ctx = OverflowRewriteContext(overflowKeys, loader)
        val rewritten = ctx.rewrite(value)
        return OverflowRewriteResult(rewritten, ctx.synthVars)
    }

    private class OverflowRewriteContext(
        private val overflowKeys: Set<String>,
        private val loader: (String) -> String?
    ) {
        // 纯小写字母数字 + 随机 run token：不含 ${}，可被替换引擎当普通变量识别，且不与真实变量名冲突
        private val runToken = UUID.randomUUID().toString().replace("-", "")
        val synthVars = LinkedHashMap<String, String>()
        private val keyToSynth = HashMap<String, String>()

        fun rewrite(value: Any?): Any? = when (value) {
            is String -> rewriteText(value)
            is Map<*, *> -> value.entries.associateTo(LinkedHashMap<Any?, Any?>()) { (k, v) -> k to rewrite(v) }
            is List<*> -> value.map { rewrite(it) }
            else -> value
        }

        private fun rewriteText(text: String): String {
            if (!text.contains("\${{")) return text
            return doubleBracePattern.replace(text) { m ->
                val rawKey = m.groupValues[1].trim()
                if (!isOverflowToken(rawKey)) {
                    return@replace m.value
                }
                keyToSynth[rawKey]?.let { return@replace "\${{$it}}" }
                val real = try {
                    loader.invoke(rawKey)
                } catch (ignore: Throwable) {
                    logger.warn("OVERFLOW_REWRITE_LOAD_FAIL|key=$rawKey|${ignore.message}")
                    null
                }
                if (real == null || isOverflowReference(real)) {
                    // 加载失败或仍是引用串：保持原样，行为退化为"输出引用串"
                    m.value
                } else {
                    val synth = "bkovf${runToken}n${keyToSynth.size}"
                    keyToSynth[rawKey] = synth
                    synthVars[synth] = real
                    "\${{$synth}}"
                }
            }
        }

        // 支持任意层级 key，含嵌套 context 键（如 jobs.x.steps.y.outputs.z），其落库 key 即完整 dotted 名
        private fun isOverflowToken(key: String): Boolean {
            val loadKey = if (key.startsWith(VARIABLES_PREFIX)) key.removePrefix(VARIABLES_PREFIX) else key
            return overflowKeys.contains(key) ||
                overflowKeys.contains(loadKey) ||
                overflowKeys.contains(VARIABLES_PREFIX + loadKey)
        }
    }
}
