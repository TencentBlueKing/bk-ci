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

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import org.slf4j.LoggerFactory

/**
 * 大变量"引用协议"工具（引擎 / Worker 共用）。
 *
 * 变量值超过 [PIPELINE_VARIABLES_STRING_LENGTH_MAX]（4000 字符）时，真实值以文件形式存入制品库，
 * 变量表里只保存引用串（启动参数不支持大变量，见 PipelineBuildService）：
 *
 *     __BK_OVF__:<字符长度>:<写入时的执行次数>:<UTF-8 内容的 sha256 十六进制>
 *
 *  - 文件在 `variable` 仓库，路径为 [blobPath]（无后缀）：同一次执行内同值只存一份、写入天然幂等；
 *    重试产生的新值落在新的执行次数目录下，不会覆盖之前执行的文件，旧引用仍可读取原值；
 *  - 下载后用摘要校验内容完整性，摘要只允许 64 位小写十六进制，杜绝路径穿越；
 *  - `$xxx` / `${xxx}` 旧语法看到的固定是引用串，只有 `${{ xxx }}` 表达式按需加载真实值。
 */
object BuildVarOverflowUtils {

    private val logger = LoggerFactory.getLogger(BuildVarOverflowUtils::class.java)

    /** variables.xxx 落库 key 是 xxx，判断/加载时需去掉该前缀。 */
    private const val VARIABLES_PREFIX = "variables."

    /**
     * 仅匹配双花括号 `${{ key }}`；key 内不含 `$ { }`。
     * 单花括号 `${ }` / `$x` 旧语法不在此处处理（保持引用串，避免大值意外展开）。
     */
    private val doubleBracePattern = Regex("\\$\\{\\{([^{}$]+)}}")

    private val referencePattern = Regex(
        "^${Regex.escape(PIPELINE_VARIABLES_OVERFLOW_PREFIX)}(\\d{1,10}):([1-9]\\d{0,5}):([0-9a-f]{64})$"
    )

    private const val MAIN_TABLE_MAX_LENGTH: Int = PIPELINE_VARIABLES_STRING_LENGTH_MAX

    private const val LARGE_VALUE_MAX_BYTES: Int = PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES

    private val HEX = "0123456789abcdef".toCharArray()

    /** 哨兵前缀，用于识别"是否大变量引用"。 */
    const val OVERFLOW_PREFIX: String = PIPELINE_VARIABLES_OVERFLOW_PREFIX

    /**
     * 大变量文件所在的制品库仓库，与 common-archive 的 REPO_VARIABLE 保持一致。
     * `display = false`，不出现在制品库仓库列表。文件不带后缀，文件名就是内容摘要。
     */
    const val REPO_NAME = "variable"

    /**
     * 解析后的大变量引用。
     * @property length 原始值字符长度（供 UI / 日志展示与预算预判）
     * @property executeCount 写入该值时构建的执行次数（重试后递增），决定文件所在目录
     * @property digest UTF-8 内容 sha256 十六进制，同时是制品库文件名
     */
    data class LargeVarRef(val length: Int, val executeCount: Int, val digest: String)

    /**
     * 是否为大变量引用串（只看前缀）。无法解析的引用（含格式不完整的历史引用）
     * 一律按"加载失败"处理：保留引用串原样输出。
     */
    fun isOverflowReference(value: String?): Boolean {
        return value != null && value.startsWith(OVERFLOW_PREFIX)
    }

    fun parseReference(value: String?): LargeVarRef? {
        if (!isOverflowReference(value)) return null
        val match = referencePattern.matchEntire(value!!) ?: return null
        val length = match.groupValues[1].toIntOrNull() ?: return null
        return LargeVarRef(
            length = length,
            executeCount = match.groupValues[2].toInt(),
            digest = match.groupValues[3]
        )
    }

    fun toReference(ref: LargeVarRef): String = "$OVERFLOW_PREFIX${ref.length}:${ref.executeCount}:${ref.digest}"

    /** 是否需要以大变量形式存储（超过主表上限且不是引用串）。 */
    fun shouldOverflow(rawValue: String): Boolean =
        rawValue.length > MAIN_TABLE_MAX_LENGTH && !isOverflowReference(rawValue)

    /** 是否超过大变量单值上限 [PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES]（按 UTF-8 字节计）。 */
    fun exceedsLargeValueLimit(rawValue: String): Boolean {
        // 每个 char 编码后 1~3 字节（代理对 2 char 共 4 字节），先用字符数做无分配的快速判定
        if (rawValue.length > LARGE_VALUE_MAX_BYTES) return true
        if (rawValue.length.toLong() * 3 <= LARGE_VALUE_MAX_BYTES) return false
        return utf8Length(rawValue) > LARGE_VALUE_MAX_BYTES
    }

    /** 不分配内存地计算字符串的 UTF-8 编码字节数。 */
    fun utf8Length(value: CharSequence): Long {
        var bytes = 0L
        var i = 0
        val n = value.length
        while (i < n) {
            val c = value[i]
            bytes += when {
                c.code < 0x80 -> 1
                c.code < 0x800 -> 2
                Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(value[i + 1]) -> {
                    i++
                    4
                }
                else -> 3
            }
            i++
        }
        return bytes
    }

    fun sha256Hex(value: String): String = sha256Hex(value.toByteArray(StandardCharsets.UTF_8))

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        digest.forEach { b ->
            val v = b.toInt() and 0xff
            sb.append(HEX[v ushr 4]).append(HEX[v and 0x0f])
        }
        return sb.toString()
    }

    /**
     * 校验下载内容与引用摘要一致后解码；不一致（文件损坏 / 路径被覆盖）返回 null。
     */
    fun decodeVerified(ref: LargeVarRef, bytes: ByteArray): String? {
        if (sha256Hex(bytes) != ref.digest) return null
        return String(bytes, StandardCharsets.UTF_8)
    }

    /**
     * 某次构建在 [REPO_NAME] 仓库中的目录，也是构建机临时令牌的授权范围（覆盖所有执行次数）。
     * 这个仓库里只有大变量，令牌授到构建目录不会碰到流水线仓库里的构建产物。
     */
    fun blobDir(pipelineId: String, buildId: String): String = "/$pipelineId/$buildId"

    /** 大变量文件完整路径：`/{pipelineId}/{buildId}/{executeCount}/{sha256}`，无后缀。 */
    fun blobPath(pipelineId: String, buildId: String, ref: LargeVarRef): String =
        "${blobDir(pipelineId, buildId)}/${ref.executeCount}/${ref.digest}"

    /**
     * 从变量 Map 中收集大变量引用键。
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
     * 估算 [texts] 中通过 `${{ }}` 可能引用到的大变量键（用于批量预取，允许少量误判）。
     */
    fun findReferencedOverflowKeys(texts: Collection<String>, overflowKeys: Set<String>): Set<String> {
        if (overflowKeys.isEmpty()) return emptySet()
        val exprTexts = texts.filter { it.contains("\${{") }
        if (exprTexts.isEmpty()) return emptySet()
        return overflowKeys.filterTo(HashSet()) { key ->
            val bare = key.removePrefix(VARIABLES_PREFIX)
            exprTexts.any { it.contains(bare) }
        }
    }

    /** 把表达式里的 key 映射成变量表中实际存在的 key（兼容 `variables.` 前缀）。 */
    fun resolveStoredKey(key: String, variables: Map<String, String>): String? {
        if (variables.containsKey(key)) return key
        val bare = key.removePrefix(VARIABLES_PREFIX)
        if (variables.containsKey(bare)) return bare
        val prefixed = VARIABLES_PREFIX + bare
        return if (variables.containsKey(prefixed)) prefixed else null
    }

    /**
     * 合成键溢出重写的结果。
     * @property value 重写后的值（结构与入参一致：String / Map / List / 其它原样）
     * @property synthVars 合成键 -> 大变量真实值 映射；调用方需并入替换上下文
     */
    data class OverflowRewriteResult(
        val value: Any?,
        val synthVars: Map<String, String>
    )

    /**
     * 传统方言 `${{ 大变量 }}` 按需加载的**唯一共享实现**（Worker 侧 claim 参数替换 / 脚本替换、
     * 以及兼容老 Worker 的引擎侧 claim 替换共用）。
     *
     * 把被 `${{ key }}` 引用到、且命中溢出键的大变量重写成 `${{ 合成唯一键 }}`，并把
     * 合成键 -> 真实值 收进 [OverflowRewriteResult.synthVars]；调用方将其并入替换上下文，交由各自的
     * 替换引擎（ObjectReplaceEnvVarUtil / ReplacementUtils）把合成键当**普通变量**完成替换，
     * 从而复用各引擎的 JSON 转义 / quoteReplacement，任意上下文都不会被破坏。
     *
     * 语义约束：
     * - 仅双花括号 `${{ }}` 且命中溢出键才触发加载；同键只加载一次（去重）；
     * - `${x}` / `$x` 旧语法、未被引用的大变量、其它普通变量：完全不动；
     * - loader 返回 null 或仍是引用串（加载失败）：保持原 token 不变，行为退化为"输出引用串"。
     *
     * @param value 待处理值，支持 String 及任意 Map/List 嵌套对象图
     * @param overflowKeys [collectOverflowKeys] 收集到的溢出键；为空时直接原样返回
     * @param loader 按 key 加载真实值；由调用方决定远程/本地及兜底策略
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
            is List<*> -> value.mapTo(ArrayList()) { rewrite(it) }
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
            val loadKey = key.removePrefix(VARIABLES_PREFIX)
            return overflowKeys.contains(key) ||
                overflowKeys.contains(loadKey) ||
                overflowKeys.contains(VARIABLES_PREFIX + loadKey)
        }
    }
}
