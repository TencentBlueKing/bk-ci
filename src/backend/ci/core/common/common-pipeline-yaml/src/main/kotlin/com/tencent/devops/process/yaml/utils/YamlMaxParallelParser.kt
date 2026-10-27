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

package com.tencent.devops.process.yaml.utils

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.process.utils.MatrixConcurrencyUtils

/**
 * YAML中矩阵最大并发数（strategy.max-parallel）的解析与导出
 *
 * max-parallel既支持纯数字（max-parallel: 5），也支持流水线变量表达式（max-parallel: ${{MATRIX_MAX}}），
 * 因此统一在此处做「数字/变量」的拆分与还原，避免各处实现发散。
 *
 * 注意：本类只负责YAML字段与[com.tencent.devops.common.pipeline.option.MatrixControlOption]的双向转换，
 * 数值范围校验由[MatrixConcurrencyUtils]在模板实例化与运行时统一处理。
 */
object YamlMaxParallelParser {

    /**
     * 解析max-parallel的取值：
     * - 未配置返回 (null, null)
     * - 纯数字返回 (数值, null)
     * - 变量表达式返回 (null, 表达式)，实际数值在矩阵分裂时解析
     * - 既不是数字也不是变量表达式时直接报错，避免脏值被当成变量存下来、直到运行时才暴露
     */
    fun parse(maxParallel: String?): Pair<Int?, String?> {
        val value = maxParallel?.trim()?.takeIf { it.isNotBlank() } ?: return Pair(null, null)
        value.toIntOrNull()?.let { return Pair(it, null) }
        if (!MatrixConcurrencyUtils.containsVariable(value)) {
            throw InvalidParamException(
                "matrix max-parallel($value) is invalid, must be an integer or a variable expression"
            )
        }
        return Pair(null, value)
    }

    /**
     * 反向导出max-parallel：变量表达式优先，未配置变量时输出数值
     */
    fun format(maxConcurrencyVar: String?, maxConcurrency: Int?): String? =
        maxConcurrencyVar?.trim()?.takeIf { it.isNotBlank() } ?: maxConcurrency?.toString()
}
