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
 * 矩阵Job最大并发数（maxConcurrency）的公共处理
 *
 * 最大并发数支持变量配置（MatrixControlOption.maxConcurrencyVar），变量在矩阵分裂前解析为实际数值：
 * - 模板实例化与运行时启动前都通过[isValid]做严格校验，非法值直接失败，不做静默截断；
 * - 只有调度侧读取历史数据（未经校验的存量数据）时才用[normalize]兜底。
 *
 * 注意：校验失败的文案统一由i18n维护（ProcessMessageCode.ERROR_TEMPLATE_MATRIX_MAX_CONCURRENCY_INVALID
 * 与ProcessMessageCode.ERROR_MATRIX_MAX_CONCURRENCY_INVALID），此处不再重复定义，避免文案漂移。
 */
object MatrixConcurrencyUtils {

    /** 矩阵内最大并发数的最小值 */
    const val MIN_MATRIX_CONCURRENCY = 1

    /**
     * 变量占位符：既匹配经典风格的 ${VAR}，也匹配制约风格的 ${{ variables.VAR }}。
     * 制约风格用非贪婪匹配，兼容 ${{ fromJSON(variables.VAR) }} 这类带函数调用的表达式。
     *
     * 全链路（模板实例化校验、YAML max-parallel 解析）统一使用此处判定，
     * 避免各处各写一份正则导致方言演进后判定漂移。
     */
    private val VARIABLE_PATTERN = Regex("""\$(?:\{\{.*?\}\}|\{[^{}]+\})""")

    /** 值中是否仍残留变量占位符，用于判断变量是否已被解析出结果 */
    fun containsVariable(value: String): Boolean = VARIABLE_PATTERN.containsMatchIn(value)

    /** 取默认值：未配置（null）时使用默认并发数，已配置的值不做任何改写 */
    fun default(value: Int?): Int = value ?: PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT

    /** 严格有效性：必须是[MIN_MATRIX_CONCURRENCY]到[PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX]之间的整数 */
    fun isValid(value: Int?): Boolean =
        value != null && value >= MIN_MATRIX_CONCURRENCY && value <= PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX

    /**
     * 兜底规范化，仅用于调度侧读取未经校验的历史数据（升级前遗留的在途构建）：
     * 未配置、0或负数时取默认值[PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT]，
     * 超过[PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX]时取上限。
     *
     * 新数据不应依赖本方法，非法值在模板实例化与矩阵启动时已经失败退出；
     * 调用方必须在收敛前打日志，避免退化成静默截断。
     */
    fun normalize(value: Int?): Int = when {
        value == null || value < MIN_MATRIX_CONCURRENCY -> PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT
        value > PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX -> PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX
        else -> value
    }
}
