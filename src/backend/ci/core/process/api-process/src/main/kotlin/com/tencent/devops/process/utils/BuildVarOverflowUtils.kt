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

/**
 * 大变量"摘要协议"工具。
 *
 * 主表 T_PIPELINE_BUILD_VAR.VALUE 长度受限于 varchar(4000)。
 * 当真实值超过该长度时，主表只写入"摘要 + 哨兵前缀 + 长度"形式的占位符，
 * 真实值进入 T_PIPELINE_BUILD_VAR_OVERFLOW 表，由 BuildVariableService 按需加载。
 *
 * 占位符格式：
 *   __BK_OVF__:<originalLength>:<truncatedValue>
 *
 *   - originalLength: 原始字符长度，便于 UI / API 展示提示
 *   - truncatedValue: 前 [SUMMARY_TRUNCATE_LENGTH] 个字符（去除可能撑爆主表上限的长度后），
 *     便于旧 ${xxx}/$xxx 风格仍可看到摘要而非空字符串，避免大变化破坏现存脚本行为
 */
object BuildVarOverflowUtils {

    /** 主表 VALUE 列字符上限，与 [PIPELINE_VARIABLES_STRING_LENGTH_MAX] 对齐。 */
    const val MAIN_TABLE_MAX_LENGTH: Int = PIPELINE_VARIABLES_STRING_LENGTH_MAX

    /** 摘要值的最大字符数。 */
    private const val SUMMARY_TRUNCATE_LENGTH = 1024

    /** 哨兵前缀，用于在主表 VALUE 列识别"是否大变量摘要"。 */
    const val OVERFLOW_PREFIX: String = PIPELINE_VARIABLES_OVERFLOW_PREFIX

    /** 大变量值硬上限，超过则拒绝写入。 */
    const val HARD_MAX_LENGTH: Int = PIPELINE_VARIABLES_STRING_LENGTH_HARD_MAX

    /**
     * 判断主表 VALUE 是否为"溢出占位符"。
     * 注意：用户主动写入相同前缀的字符串概率极低（前缀以双下划线包围，符合保留字习惯），
     * 即使发生也只是"按需触发一次空查询"，无副作用。
     */
    fun isOverflowSummary(value: String?): Boolean {
        return value != null && value.startsWith(OVERFLOW_PREFIX)
    }

    /**
     * 解析占位符字符串为 [OverflowSummary]，解析失败时返回 null。
     */
    fun parseOverflowSummary(value: String?): OverflowSummary? {
        if (!isOverflowSummary(value)) return null
        // 形如 __BK_OVF__:1234567:xxx...
        val rest = value!!.substring(OVERFLOW_PREFIX.length)
        val sepIndex = rest.indexOf(':')
        if (sepIndex <= 0) return null
        val length = rest.substring(0, sepIndex).toIntOrNull() ?: return null
        val summary = rest.substring(sepIndex + 1)
        return OverflowSummary(originalLength = length, truncated = summary)
    }

    /**
     * 由真实值构造主表占位符。
     * 截断长度同时考虑 [MAIN_TABLE_MAX_LENGTH] 与 [SUMMARY_TRUNCATE_LENGTH]，
     * 避免占位符自身溢出主表 VALUE 列。
     */
    fun buildOverflowSummary(rawValue: String): String {
        val len = rawValue.length
        val maxSummaryLen = minOf(
            SUMMARY_TRUNCATE_LENGTH,
            // 预留 OVERFLOW_PREFIX 与长度数字
            MAIN_TABLE_MAX_LENGTH - OVERFLOW_PREFIX.length - len.toString().length - 1
        ).coerceAtLeast(0)
        val truncated = if (rawValue.length <= maxSummaryLen) rawValue else rawValue.substring(0, maxSummaryLen)
        return "$OVERFLOW_PREFIX$len:$truncated"
    }

    /**
     * 在主表 VALUE 列要写入的"实际字符串"。
     * - 当 raw <= [MAIN_TABLE_MAX_LENGTH]：直接返回 raw；
     * - 否则：返回占位符（不会再次溢出）。
     */
    fun toMainTableValue(rawValue: String): String {
        return if (rawValue.length <= MAIN_TABLE_MAX_LENGTH) rawValue else buildOverflowSummary(rawValue)
    }

    /** 是否需要将真实值写入溢出表。 */
    fun shouldOverflow(rawValue: String): Boolean = rawValue.length > MAIN_TABLE_MAX_LENGTH

    data class OverflowSummary(
        /** 原始变量值字符长度。 */
        val originalLength: Int,
        /** 截断后的摘要值。 */
        val truncated: String
    )
}
