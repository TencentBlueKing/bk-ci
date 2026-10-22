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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MatrixConcurrencyUtilsTest {

    @Test
    fun isValid() {
        assertTrue(MatrixConcurrencyUtils.isValid(1))
        assertTrue(MatrixConcurrencyUtils.isValid(PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX))
        // 不能超过上限，也不能是非正数，否则矩阵Job必须失败退出
        assertFalse(MatrixConcurrencyUtils.isValid(PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX + 1))
        assertFalse(MatrixConcurrencyUtils.isValid(0))
        assertFalse(MatrixConcurrencyUtils.isValid(-1))
        assertFalse(MatrixConcurrencyUtils.isValid(null))
    }

    @Test
    fun defaultConcurrency() {
        assertEquals(PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT, MatrixConcurrencyUtils.default(null))
        // 已配置的值不做任何改写
        assertEquals(1, MatrixConcurrencyUtils.default(1))
        assertEquals(PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX, MatrixConcurrencyUtils.default(50))
    }

    @Test
    fun normalize() {
        // 仅用于兜底历史数据：非法值收敛到合法区间
        assertEquals(PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT, MatrixConcurrencyUtils.normalize(null))
        assertEquals(PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT, MatrixConcurrencyUtils.normalize(0))
        assertEquals(PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT, MatrixConcurrencyUtils.normalize(-1))
        assertEquals(PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX, MatrixConcurrencyUtils.normalize(100))
        assertEquals(7, MatrixConcurrencyUtils.normalize(7))
    }

    @Test
    fun containsVariable() {
        // 经典风格 ${VAR} 与制约风格 ${{ variables.VAR }} 都要能识别
        assertTrue(MatrixConcurrencyUtils.containsVariable("\${MATRIX_MAX}"))
        assertTrue(MatrixConcurrencyUtils.containsVariable("\${{ variables.MATRIX_MAX }}"))
        assertTrue(MatrixConcurrencyUtils.containsVariable("\${{MATRIX_MAX}}"))
        // 带函数调用的表达式也要识别为变量
        assertTrue(MatrixConcurrencyUtils.containsVariable("\${{ fromJSON(variables.MATRIX_MAX) }}"))
        assertFalse(MatrixConcurrencyUtils.containsVariable("5"))
        assertFalse(MatrixConcurrencyUtils.containsVariable(""))
    }
}
